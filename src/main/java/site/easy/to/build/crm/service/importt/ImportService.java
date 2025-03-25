package site.easy.to.build.crm.service.importt;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

@Service
public class ImportService {

    private final JdbcTemplate jdbcTemplate;

    public ImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static String generateRandomString() {
        Random random = new Random();
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int longueur = random.nextInt(16); // Génère une longueur entre 0 et 15
        
        StringBuilder chaine = new StringBuilder();
        for (int i = 0; i < longueur; i++) {
            int index = random.nextInt(caracteres.length());
            chaine.append(caracteres.charAt(index));
        }
        
        return chaine.toString();
    }

  
}