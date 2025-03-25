package site.easy.to.build.crm.service.importt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.ImportResult;
import site.easy.to.build.crm.repository.CustomerRepository;
import site.easy.to.build.crm.service.customer.CustomerService;
import site.easy.to.build.crm.service.importt.CustomerImportService.CustomerCsvDto;



@Service
public class BudgetImportService {

    private final JdbcTemplate jdbcTemplate;
    private final CustomerRepository customerRepository;

    
    public BudgetImportService(JdbcTemplate jdbcTemplate, CustomerRepository customerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.customerRepository= customerRepository;
    }

    @Transactional
    public ImportResult importBudgets(MultipartFile file) throws IOException {
        List<BudgetCsvDto> budgets = new ArrayList<>();
        try {
            System.out.println("Début de l'importation");

            createTempBudgetTable();
            budgets = parseCsv(file);
            insertIntoTempTable(budgets); 
            validateData();
            insertIntoBudgetTable();

            System.out.println("Importation terminée");
            
            return new ImportResult(true, "Importation réussie", null);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            System.err.println("Erreur lors de l'importation : " + errorMessage);
            e.printStackTrace();
            return new ImportResult(false, errorMessage, null); 
        } finally {
            cleanUpTempTable();
        }
    }

    public void createTempBudgetTable() {
        System.out.println("Creation table temporaire");
        String sql = "CREATE TEMPORARY TABLE temp_budget ("
                + "line_number INT NOT NULL," // Colonne pour le numéro de ligne
                + "customer_id int unsigned DEFAULT NULL,"
                + "amount decimal(10,2) DEFAULT NULL,"
                + "budget_date datetime DEFAULT NULL,"
                + "email VARCHAR(255) DEFAULT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci";
        
        try {
            jdbcTemplate.execute(sql);
            System.out.println("Table temporaire créée");
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la table temporaire : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de la création de la table temporaire", e);
        }
    }

    public Integer getCustomerId(String email){
        Customer customer= customerRepository.findByEmail(email);
        return customer.getCustomerId();
    }

    public List<BudgetCsvDto> parseCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier CSV est vide ou non fourni");
        }
    
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            System.out.println("Début du parsing du CSV");
            CsvToBean<BudgetCsvDto> csvToBean = new CsvToBeanBuilder<BudgetCsvDto>(reader)
                    .withType(BudgetCsvDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withThrowExceptions(true)
                    .build();
    
            List<BudgetCsvDto> budgets = csvToBean.parse();

            for (int i = 0; i < budgets.size(); i++) {
                BudgetCsvDto budget = budgets.get(i);
                // System.out.println("Ligne " + (i + 1) + " parsée : amount=" + budget.getAmount() + 
                //         ", email=" + budget.getEmail());
            }
            if (budgets.isEmpty()) {
                throw new IOException("Aucune donnée valide trouvée dans le fichier CSV");
            }
            return budgets;
        } catch (RuntimeException e) {
            throw new IOException("Erreur lors du parsing du CSV : " + e.getMessage(), e);
        }
    }

    public void insertIntoTempTable(List<BudgetCsvDto> budgets) throws SQLException {
        System.out.println("Insertion dans table temporaire");
        String sql = "INSERT INTO temp_budget (line_number, customer_id, amount, budget_date, email) " +
                "VALUES (?, ?, ?, ?, ?)";
        
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    BudgetCsvDto budget = budgets.get(i);
                    System.out.println("Insertion de la ligne " + (i + 1) + ": " + 
                            budget.getAmount());
                    ps.setInt(1, i + 1); // Numéro de ligne (commence à 1)
                    
                    // On laisse customer_id à 0 pour l'instant, la validation se fera après
                    ps.setInt(2, 0); 
                    ps.setObject(3, budget.getAmount());
                    ps.setObject(4, LocalDateTime.now());
                    ps.setString(5, budget.getEmail());
                }
        
                @Override
                public int getBatchSize() {
                    return budgets.size();
                }
            });
            System.out.println("Insertion dans table temporaire terminée");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'insertion : " + e.getMessage());
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        }
    }

    public void validateCustomer() {
        // Vérifier si l'email dans temp_budget existe dans temp_customer
        String sql = "SELECT tb.line_number, tb.email " +
                "FROM temp_budget tb " +
                "LEFT JOIN temp_customer tc ON tb.email = tc.email " +
                "WHERE tc.email IS NULL " +
                "LIMIT 1";
        
        List<String> invalidCustomers = jdbcTemplate.query(sql, (rs, rowNum) -> 
                "Budget CSV- Ligne " + rs.getInt("line_number") + ": Email=" + rs.getString("email"));
        if (!invalidCustomers.isEmpty()) {
            throw new RuntimeException("Email de customer inexistant dans temp_customer : " + invalidCustomers.get(0));
        }
    }

    public void updateBudgetCustomerIds() {
        String sql = "UPDATE temp_budget tb " +
                "JOIN temp_customer tc ON tb.email = tc.email " +
                "SET tb.customer_id = tc.line_number"; 
        jdbcTemplate.update(sql);
    }

    public void validateAmount() {
        String sql = "SELECT line_number, amount FROM temp_budget " +
                "WHERE amount<0 " +
                "LIMIT 1";
        
        List<String> invalidMontants = jdbcTemplate.query(sql, (rs, rowNum) -> 
                "Budget CSV- Ligne " + rs.getInt("line_number") + ": Montant=" + rs.getInt("amount"));
        if (!invalidMontants.isEmpty()) {
            throw new RuntimeException("Montant negatif : " + invalidMontants.get(0));
        }
    }

    public void validateData() {
        System.out.println("Validation des données");
        validateCustomer();
        validateAmount();
        System.out.println("Validation terminée");
    }

    public void insertIntoBudgetTable() {
        String sql = "INSERT INTO budget (customer_id, amount, budget_date) " +
                "SELECT customer_id, amount, budget_date FROM temp_budget";
        
        jdbcTemplate.execute(sql);
        System.out.println("Données insérées dans la table budget");
    }

    public void cleanUpTempTable() {
        jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS temp_budget");
    }


    public static class BudgetCsvDto {

        @CsvBindByName(column = "customer_email")
        private String email;

        @CsvBindByName(column = "Budget")
        private BigDecimal amount;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }


        
    }   
}