package site.easy.to.build.crm.service.importt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

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
import site.easy.to.build.crm.entity.User;
import site.easy.to.build.crm.repository.CustomerRepository;
import site.easy.to.build.crm.repository.UserRepository;
import site.easy.to.build.crm.service.customer.CustomerService;
import site.easy.to.build.crm.service.importt.BudgetImportService.BudgetCsvDto;
import site.easy.to.build.crm.service.importt.CustomerImportService.CustomerCsvDto;



@Service
public class ExpenseImportService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    
    public ExpenseImportService(JdbcTemplate jdbcTemplate, UserRepository userRepository, CustomerRepository customerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository= userRepository;
        this.customerRepository= customerRepository;
    }


    @Transactional
    public ImportResult importExpenses(MultipartFile file) throws IOException {
        List<ExpenseCsvDto> Expenses = new ArrayList<>();
        try {
            System.out.println("Début de l'importation");

            createTempExpenseTable();
            Expenses = parseCsv(file);
            insertIntoTempTable(Expenses); 
            validateData();
            insertIntoFinalTables();

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



    public void createTempExpenseTable() {
        System.out.println("Création de la table temporaire temp_expense");
        String sql = "CREATE TEMPORARY TABLE temp_expense ("
                + "temp_id INT AUTO_INCREMENT PRIMARY KEY," // Ajout
                + "line_number INT NOT NULL," 
                + "customer_email VARCHAR(255) DEFAULT NULL," 
                + "subject_or_name VARCHAR(255) DEFAULT NULL," 
                + "type VARCHAR(100) DEFAULT NULL," 
                + "status VARCHAR(50) DEFAULT NULL," 
                + "amount DECIMAL(10,2) DEFAULT NULL," 
                + "description VARCHAR(255) DEFAULT NULL,"
                + "priority VARCHAR(255) DEFAULT NULL,"
                + "customer_id INT NOT NULL,"
                + "manager_id INT NOT NULL,"
                + "employee_id INT NOT NULL,"
                + "phone VARCHAR(20) DEFAULT NULL,"
                + "created_at DATETIME DEFAULT NULL,"
                + "lead_id INT UNSIGNED DEFAULT NULL,"
                + "ticket_id INT UNSIGNED DEFAULT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci";
        
        try {
            jdbcTemplate.execute(sql);
            System.out.println("Table temporaire temp_expense créée");
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la table temporaire temp_expense : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de la création de la table temporaire temp_expense", e);
        }
    }

    public List<ExpenseCsvDto> parseCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier CSV est vide ou non fourni");
        }
    
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            System.out.println("Début du parsing du CSV");
            CsvToBean<ExpenseCsvDto> csvToBean = new CsvToBeanBuilder<ExpenseCsvDto>(reader)
                    .withType(ExpenseCsvDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withThrowExceptions(true)
                    .build();
    
            List<ExpenseCsvDto> expenses = csvToBean.parse();

            for (int i = 0; i < expenses.size(); i++) {
                ExpenseCsvDto expense = expenses.get(i);
                System.out.println("Ligne " + (i + 1) + " parsée : amount=" + expense.getExpense() + 
                        ", email=" + expense.getEmail());
            }
            if (expenses.isEmpty()) {
                throw new IOException("Aucune donnée valide trouvée dans le fichier CSV");
            }
            return expenses;
        } catch (RuntimeException e) {
            throw new IOException("Erreur lors du parsing du CSV : " + e.getMessage(), e);
        }
    }

    public void insertIntoTempTable(List<ExpenseCsvDto> expenses) throws SQLException {
        System.out.println("Insertion dans table temporaire");
        String sql = "INSERT INTO temp_expense (line_number, customer_id, customer_email, subject_or_name, type, status, amount, " +
                     "description, priority, manager_id, employee_id, phone, created_at) VALUES " +
                     "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ExpenseCsvDto expense = expenses.get(i);
                    System.out.println("Insertion de la ligne " + (i + 1) + ": " + expense.getSubjectOrName());
                    
                    // int customerId = 0;
                    // Customer customer = customerRepository.findByEmail(expense.getEmail());
                    // if (customer != null) {
                    //     customerId = customer.getCustomerId();
                    // }
                    // System.out.println("Id de customer récupéré : " + customerId);
    
                    BigDecimal amount = expense.getAmount();
                    System.out.println("Valeur de amount avant insertion : " + amount);
    
                    ps.setInt(1, i + 1);
                    ps.setInt(2, 0);
                    ps.setString(3, expense.getEmail());
                    ps.setString(4, expense.getSubjectOrName());
                    ps.setString(5, expense.getType());
                    ps.setString(6, expense.getStatus());
                    ps.setObject(7, amount);
                    ps.setString(8, generateRandomString());
                    ps.setString(9, generateTicketPriority());
                    ps.setInt(10, getRandomUserId());
                    ps.setInt(11, getRandomUserId());
                    ps.setString(12, generatePhoneNumber());
                    ps.setObject(13, getRandomDateTime());
                }
    
                @Override
                public int getBatchSize() {
                    return expenses.size();
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
        // Vérifier si l'email dans temp_expense existe dans temp_customer
        String sql = "SELECT tb.line_number, tb.customer_email " +
                "FROM temp_expense tb " +
                "LEFT JOIN temp_customer tc ON tb.customer_email = tc.email " +
                "WHERE tc.email IS NULL " +
                "LIMIT 1";
        
        List<String> invalidCustomers = jdbcTemplate.query(sql, (rs, rowNum) -> 
                "Expense CSV- Ligne " + rs.getInt("line_number") + ": Email=" + rs.getString("customer_email"));
        if (!invalidCustomers.isEmpty()) {
            throw new RuntimeException("Email de customer inexistant dans temp_customer : " + invalidCustomers.get(0));
        }
    }

    public void validateType() {
        String sql = "SELECT line_number, type FROM temp_expense " +
                "WHERE type NOT IN ('lead', 'ticket') " +
                "LIMIT 1";
        
        List<String> invalidCustomers = jdbcTemplate.query(sql, (rs, rowNum) -> 
                "Expense CSV- Ligne " + rs.getInt("line_number") + ": Type=" + rs.getString("type"));
        if (!invalidCustomers.isEmpty()) {
            throw new RuntimeException("Type invalide : " + invalidCustomers.get(0));
        }
    }

    public void validateAmount() {
        String sql = "SELECT line_number, amount FROM temp_expense " +
                "WHERE amount<0 AND amount IS NOT NULL " +
                "LIMIT 1";
        
        List<String> invalidMontants = jdbcTemplate.query(sql, (rs, rowNum) -> 
                "Expense CSV- Ligne " + rs.getInt("line_number") + ": Montant=" + rs.getInt("amount"));
        if (!invalidMontants.isEmpty()) {
            throw new RuntimeException("Montant negatif : " + invalidMontants.get(0));
        }
    }

    public void validateData() {
        System.out.println("Validation des données");
        validateCustomer();
        validateAmount();
        validateType();
        System.out.println("Validation terminée");
    }

    public void updateExpenseCustomerIds() {
        String sql = "UPDATE temp_expense tb " +
                "JOIN temp_customer tc ON tb.customer_email = tc.email " +
                "SET tb.customer_id = tc.line_number"; 
        jdbcTemplate.update(sql);
    }

    public void insertIntoFinalTables() {
        System.out.println("Insertion dans les tables finales");
    
        // Étape 1 : Insérer les leads et mettre à jour temp_expense avec lead_id
        String leadInsertSql = "INSERT INTO trigger_lead (customer_id, user_id, name, phone, employee_id, status, created_at) " +
                "SELECT customer_id, manager_id, subject_or_name, phone, employee_id, status, created_at " +
                "FROM temp_expense WHERE type = 'lead'";
        
        String updateLeadIdSql = "UPDATE temp_expense te " +
                "JOIN trigger_lead tl ON tl.customer_id = te.customer_id " +
                "AND tl.name = te.subject_or_name " +
                "AND tl.created_at = te.created_at " +
                "SET te.lead_id = tl.lead_id " +
                "WHERE te.type = 'lead'";
    
        // Étape 2 : Insérer les tickets et mettre à jour temp_expense avec ticket_id
        String ticketInsertSql = "INSERT INTO trigger_ticket (subject, description, status, priority, customer_id, manager_id, employee_id, created_at) " +
                "SELECT subject_or_name, description, status, priority, customer_id, manager_id, employee_id, created_at " +
                "FROM temp_expense WHERE type = 'ticket'";
        
        String updateTicketIdSql = "UPDATE temp_expense te " +
                "JOIN trigger_ticket tt ON tt.customer_id = te.customer_id " +
                "AND tt.subject = te.subject_or_name " +
                "AND tt.created_at = te.created_at " +
                "SET te.ticket_id = tt.ticket_id " +
                "WHERE te.type = 'ticket'";
    
        // Étape 3 : Insérer dans expense
        String expenseInsertSql = "INSERT INTO expense (customer_id, amount, ticket_id, lead_id) " +
                "SELECT customer_id, amount, ticket_id, lead_id " +
                "FROM temp_expense ";
                //"WHERE amount IS NOT NULL AND (ticket_id IS NOT NULL OR lead_id IS NOT NULL)";
    
        try {
            // Insérer les leads
            int leadsInserted = jdbcTemplate.update(leadInsertSql);
            System.out.println("Nombre de leads insérés : " + leadsInserted);
            if (leadsInserted > 0) {
                jdbcTemplate.update(updateLeadIdSql);
            }
    
            // Insérer les tickets
            int ticketsInserted = jdbcTemplate.update(ticketInsertSql);
            System.out.println("Nombre de tickets insérés : " + ticketsInserted);
            if (ticketsInserted > 0) {
                jdbcTemplate.update(updateTicketIdSql);
            }
    
            // Insérer dans expense
            int expensesInserted = jdbcTemplate.update(expenseInsertSql);
            System.out.println("Nombre d'expenses insérées : " + expensesInserted);
    
            System.out.println("Données insérées dans les tables finales avec succès");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'insertion dans les tables finales : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'insertion dans les tables finales", e);
        }
    }

    public void cleanUpTempTable() {
        jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS temp_expense");
    }

    public static String generateRandomString() {
        Random random = new Random();
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int longueur = random.nextInt(20);
        
        StringBuilder chaine = new StringBuilder();
        for (int i = 0; i < longueur; i++) {
            int index = random.nextInt(caracteres.length());
            chaine.append(caracteres.charAt(index));
        }
        
        return chaine.toString();
    }

    public String generateTicketPriority() {
        Random random = new Random();
        List<String> priority= new ArrayList<String>();
        priority.add("Low");
        priority.add("Medium");
        priority.add("High");
        priority.add("Closed");
        priority.add("Urgent");
        priority.add("Critical");
        
        int randomIndex = random.nextInt(priority.size());
        
        return priority.get(randomIndex);
    }

    public Integer getRandomUserId() {
        Random random = new Random();
        long totalUsers = userRepository.count();
        
        if (totalUsers == 0) {
            throw new RuntimeException("Aucun utilisateur disponible dans la base");
        }
        
        List<User> allUsers = userRepository.findAll();
        int randomIndex = random.nextInt((int) totalUsers);
        
        return allUsers.get(randomIndex).getId();
    }

    public static String generatePhoneNumber() {
        Random random = new Random();
        StringBuilder numero = new StringBuilder("03");
        for (int i = 0; i < 8; i++) {
            numero.append(random.nextInt(10));
        }
        return numero.toString();
    }

    public static LocalDateTime getRandomDateTime() {
        // Date de début: 1er janvier 2023 à minuit
        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0);
        // Date de fin: date et heure actuelles
        LocalDateTime end = LocalDateTime.now();
        
        // Conversion des LocalDateTime en secondes depuis l'Epoch (1er janvier 1970)
        long startEpoch = start.toEpochSecond(ZoneOffset.UTC);
        long endEpoch = end.toEpochSecond(ZoneOffset.UTC);
        
        // Générer un nombre aléatoire entre les deux valeurs d'Epoch
        long randomEpoch = ThreadLocalRandom.current().nextLong(startEpoch, endEpoch);
        
        // Convertir l'Epoch aléatoire en LocalDateTime
        return LocalDateTime.ofEpochSecond(randomEpoch, 0, ZoneOffset.UTC);
    }


    public static class ExpenseCsvDto {

        @CsvBindByName(column = "customer_email")
        private String email;

        @CsvBindByName(column = "subject_or_name")
        private String subjectOrName;

        @CsvBindByName(column = "type")
        private String type;

        @CsvBindByName(column = "status")
        private String status;

        @CsvBindByName(column = "expense")
        private String expense;

        private BigDecimal amount;

        public ExpenseCsvDto() {
            if (this.expense != null && !this.expense.isEmpty()) {
                try {
                    this.amount = convertStringToBigDecimal(this.expense);
                    System.out.println("Conversion réussie : expense=" + this.expense + ", amount=" + this.amount);
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Échec de la conversion de expense en BigDecimal : " + this.expense, e);
                }
            } else {
                System.out.println("Expense vide ou null pour cet objet");
            }
        }
        
        public BigDecimal getAmount(){
            try {
                return convertStringToBigDecimal(this.expense);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Échec de la conversion de expense en BigDecimal : " + this.expense, e);
            }
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSubjectOrName() {
            return subjectOrName;
        }

        public void setSubjectOrName(String subjectOrName) {
            this.subjectOrName = subjectOrName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getExpense() {
            return expense;
        }

        public void setExpense(String expense) {
            this.expense = expense;
        }

        public static BigDecimal convertStringToBigDecimal(String value) throws ParseException {
            NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
            Number number = format.parse(value);
            
            return BigDecimal.valueOf(number.doubleValue());
        }

        
    }   
}