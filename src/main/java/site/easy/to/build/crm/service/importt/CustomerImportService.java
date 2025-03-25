package site.easy.to.build.crm.service.importt;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import site.easy.to.build.crm.entity.User;
import site.easy.to.build.crm.repository.UserRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class CustomerImportService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public CustomerImportService(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @Transactional
    public ImportResult importCustomers(MultipartFile file) throws IOException {
        List<CustomerCsvDto> customers = new ArrayList<>();
        try {
            System.out.println("Début de l'importation");
            createTempCustomerTable();
            customers = parseCsv(file);
            insertIntoTempTable(customers);
            validateData();
            insertIntoCustomerTable();
            System.out.println("Importation terminée");
            return new ImportResult(true, "Importation réussie", null);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            System.err.println("Erreur lors de l'importation : " + errorMessage);
            e.printStackTrace();
            return new ImportResult(false, errorMessage, null); // Pas besoin de findErrorLine ici
        } finally {
            cleanUpTempTable();
        }
    }

    public void createTempCustomerTable() {
        System.out.println("Creation table temporaire");
        String sql = "CREATE TEMPORARY TABLE temp_customer ("
                + "line_number INT NOT NULL," // Colonne pour le numéro de ligne
                + "name VARCHAR(255) DEFAULT NULL,"
                + "phone VARCHAR(20) DEFAULT NULL,"
                + "country VARCHAR(255) DEFAULT NULL,"
                + "user_id INT DEFAULT NULL,"
                + "email VARCHAR(255) DEFAULT NULL,"
                + "profile_id INT DEFAULT NULL,"
                + "created_at DATETIME DEFAULT NULL"
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

    public List<CustomerCsvDto> parseCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier CSV est vide ou non fourni");
        }
    
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            System.out.println("Début du parsing du CSV");
            CsvToBean<CustomerCsvDto> csvToBean = new CsvToBeanBuilder<CustomerCsvDto>(reader)
                    .withType(CustomerCsvDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withThrowExceptions(true)
                    .build();
    
            List<CustomerCsvDto> customers = csvToBean.parse();
            //System.out.println("Nombre de lignes parsées : " + customers.size());
            for (int i = 0; i < customers.size(); i++) {
                CustomerCsvDto customer = customers.get(i);
                //System.out.println("Ligne " + (i + 1) + " parsée : name=" + customer.getName() + 
                        //", email=" + customer.getEmail());
            }
            if (customers.isEmpty()) {
                throw new IOException("Aucune donnée valide trouvée dans le fichier CSV");
            }
            return customers;
        } catch (RuntimeException e) {
            throw new IOException("Erreur lors du parsing du CSV : " + e.getMessage(), e);
        }
    }

    public void insertIntoTempTable(List<CustomerCsvDto> customers) throws SQLException {
        System.out.println("Insertion dans table temporaire");
        String sql = "INSERT INTO temp_customer (line_number, name, email, phone, country, user_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    CustomerCsvDto customer = customers.get(i);
                    System.out.println("Insertion de la ligne " + (i + 1) + ": " + 
                            customer.getName() + ", " + customer.getEmail());
                    ps.setInt(1, i + 1); // Numéro de ligne (commence à 1)
                    ps.setString(2, customer.getName());
                    ps.setString(3, customer.getEmail());
                    ps.setString(4, generatePhoneNumber());
                    ps.setString(5, generateRandomString());
                    ps.setInt(6, getRandomUserId());
                    ps.setObject(7, LocalDateTime.now());
                }
        
                @Override
                public int getBatchSize() {
                    return customers.size();
                }
            });
            System.out.println("Insertion dans table temporaire terminée");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'insertion : " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("Erreur lors de l'insertion à la ligne : " + e.getMessage());
        }
    }

    public void validateData() {
        System.out.println("Validation des données");
        validateRequiredFields();
        validateEmailFormat();
        System.out.println("Validation terminée");
    }

    public void validateRequiredFields() {
        String sql = "SELECT line_number, name, email FROM temp_customer " +
                "WHERE name IS NULL OR name = '' OR email IS NULL OR email = '' OR user_id IS NULL " +
                "LIMIT 1";
        
        List<String> invalidRows = jdbcTemplate.query(sql, (rs, rowNum) -> 
                "Customer CSV- Ligne " + rs.getInt("line_number") + ": name=" + rs.getString("name") + 
                ", email=" + rs.getString("email"));
        if (!invalidRows.isEmpty()) {
            throw new RuntimeException("Champs obligatoires manquants : " + invalidRows.get(0));
        }
    }

    public void validateEmailFormat() {
        String sql = "SELECT line_number, email FROM temp_customer " +
                "WHERE email NOT LIKE '%@%.%' " +
                "OR email IS NULL " +
                "OR TRIM(email) = '' " +
                "LIMIT 1";
        
        List<String> invalidEmails = jdbcTemplate.query(sql, (rs, rowNum) -> 
                "Customer CSV- Ligne " + rs.getInt("line_number") + ": email=" + rs.getString("email"));
        if (!invalidEmails.isEmpty()) {
            throw new RuntimeException("Format d'email invalide : " + invalidEmails.get(0));
        }
    }

    public void insertIntoCustomerTable() {
        String sql = "INSERT INTO customer (name, email, phone, country, user_id, created_at, profile_id) " +
                "SELECT name, email, phone, country, user_id, created_at, profile_id FROM temp_customer";
        
        jdbcTemplate.execute(sql);
        System.out.println("Données insérées dans la table customer");
    }

    public void cleanUpTempTable() {
        jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS temp_customer");
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

    public static String generateRandomString() {
        Random random = new Random();
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int longueur = random.nextInt(16);
        
        StringBuilder chaine = new StringBuilder();
        for (int i = 0; i < longueur; i++) {
            int index = random.nextInt(caracteres.length());
            chaine.append(caracteres.charAt(index));
        }
        
        return chaine.toString();
    }

    public String findErrorLine(Exception e, List<CustomerCsvDto> customers) {
        if (e instanceof SQLException) {
            String message = e.getMessage();
            int lineIndex = findErrorIndex(e, customers);
            if (lineIndex >= 0 && lineIndex < customers.size()) {
                CustomerCsvDto customer = customers.get(lineIndex);
                return "Ligne " + (lineIndex + 2) + ": name=" + customer.getName() + 
                        ", email=" + customer.getEmail();
            }
        }
        return "Impossible de déterminer la ligne exacte";
    }

    public int findErrorIndex(Exception e, List<CustomerCsvDto> customers) {
        String message = e.getMessage();
        try {
            if (message.contains("à la ligne")) {
                String[] parts = message.split("à la ligne ");
                if (parts.length > 1) {
                    return Integer.parseInt(parts[1].split(":")[0].trim()) - 1;
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    // Classe pour retourner le résultat de l'importation
    public static class ImportResult {
        private final boolean success;
        private final String message;
        private final String errorLine;

        public ImportResult(boolean success, String message, String errorLine) {
            this.success = success;
            this.message = message;
            this.errorLine = errorLine;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorLine() {
            return errorLine;
        }
    }

    public static class CustomerCsvDto {

        @CsvBindByName(column = "customer_email")
        private String email;

        @CsvBindByName(column = "customer_name")
        private String name;

        private String phone;
        private String country;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
        
        


    }   
}