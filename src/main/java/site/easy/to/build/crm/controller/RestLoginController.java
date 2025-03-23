package site.easy.to.build.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.easy.to.build.crm.entity.Role;
import site.easy.to.build.crm.entity.User;

import site.easy.to.build.crm.service.user.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/rest/auth")
public class RestLoginController {

    @Autowired
    UserService userService;



    // DTO pour recevoir l'email dans la requête
    public static class LoginRequest {
        private String email;

        // Getter et Setter
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user;
        try {
            // Récupérer l'utilisateur par email
            user = userService.findByEmail(loginRequest.getEmail());

            // Si l'utilisateur existe, renvoyer une réponse OK avec les détails de l'utilisateur
        } catch (Exception e) {
            // Si l'utilisateur n'est pas trouvé, renvoyer une réponse Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email incorrect");
        }
        if (user != null && isManager(user)) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email incorrect");
    }

    public boolean isManager(User user) {
        List<Role> roles = user.getRoles();
        for (Role role : roles) {
            if (role.getName().equals("ROLE_MANAGER")) {
                return true;
            }
        }
        return false;
    }
}

