package com.xpertcash.controller;
import com.xpertcash.DTOs.UpdateUserRequest;
import com.xpertcash.DTOs.USER.AssignPermissionsRequest;
import com.xpertcash.DTOs.USER.RegisterResponse;
import com.xpertcash.DTOs.USER.ResendActivationRequest;
import com.xpertcash.DTOs.USER.UserDTO;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.DTOs.UserOptimalDTO;
import com.xpertcash.entity.User;
import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.DTOs.LoginRequest;
import com.xpertcash.DTOs.RegistrationRequest;
import com.xpertcash.service.UsersService;
import com.xpertcash.service.AuthenticationHelper;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UsersController {

    @Autowired
    private UsersService usersService;
    @Autowired
    private AuthenticationHelper authHelper;

    // Inscription
@PostMapping("/register")
public ResponseEntity<RegisterResponse> register(@RequestBody RegistrationRequest request) {
    RegisterResponse response = new RegisterResponse();
    try {
        response = usersService.registerUsers(
            request.getNomComplet(),
            request.getEmail(),
            request.getPassword(),
            request.getPhone(),
            request.getPays(),
            request.getNomEntreprise(),
            request.getNomBoutique()
        );
        return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
        response.setSuccess(false);
        response.setMessage(e.getMessage());
        response.setUser(null);
        return ResponseEntity.badRequest().body(response);
    }
}


    // Connexion
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            Map<String, String> tokens = usersService.login(
                request.getEmail(), 
                request.getPassword(),
                request.getDeviceId(), 
                request.getDeviceName(),
                ipAddress,
                userAgent 
            );
            tokens.put("message", "Connexion réussie");
            return ResponseEntity.ok(new HashMap<>(tokens));
        } catch (RuntimeException e) {
            if ("SESSION_LIMIT_REACHED".equals(e.getMessage())) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "SESSION_LIMIT_REACHED");
                response.put("message", "Vous avez déjà 2 sessions actives. Veuillez fermer une session avant de vous connecter.");

                try {
                    User user = usersService.findUserByEmail(request.getEmail());
                    if (user != null && usersService.verifyPassword(request.getPassword(), user.getPassword())) {
                        List<com.xpertcash.DTOs.UserSessionDTO> sessions = usersService.getActiveSessionsByUserUuid(user.getUuid());
                        response.put("sessions", sessions);
                    }
                } catch (Exception ex) {
                }
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    // Méthode utilitaire pour extraire l'adresse IP réelle du client
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    // Logout : déconnexion et invalidation du token courant
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            usersService.logout(request);
            response.put("message", "Déconnexion réussie. Le token a été invalidé.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


   
    // Endpoint pour renvoyer l'email d'activation
    @PostMapping("/resend-activation")
    public ResponseEntity<Map<String, String>> resendActivation(@RequestBody ResendActivationRequest request) {
    usersService.resendActivationEmail(request.getEmail());

    Map<String, String> response = new HashMap<>();
    response.put("message", "Email d'activation renvoyé avec succès.");
    response.put("email", request.getEmail());

     return ResponseEntity.ok(response);
    }

    // Endpoint pour renvoyer l'email d'employé avec les identifiants
    @PostMapping("/resend-employe-email")
    public ResponseEntity<Map<String, String>> resendEmployeEmail(@RequestBody ResendActivationRequest request) {
        usersService.resendEmployeEmail(request.getEmail());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Email avec les identifiants renvoyé avec succès.");
    response.put("email", request.getEmail());

     return ResponseEntity.ok(response);
    }


    // Activation du compte via le lien d'activation (GET avec paramètres dans l'URL)
    @GetMapping("/activate")
    public ResponseEntity<?> activate(@RequestParam("email") String email,
                                      @RequestParam("code") String code) {
        try {
            usersService.activateAccount(email, code);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("https://fere.tchakeda.com/connexion?message=Compte%20activ%C3%A9%20avec%20succ%C3%A8s"));
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Pour récupérer le statut du compte d'un utilisateur
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam("email") String email) {
        try {
            Map<String, Object> status = usersService.getAccountStatus(email);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Pour la mise en jour de user en multipart/form-data avec image
     @PatchMapping(value = "/updateUsers/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) 
    public ResponseEntity<?> updateUser(
        @PathVariable Long id,
        @RequestPart(value = "user", required = false) String userJson,
        @RequestPart(value = "photo", required = false) MultipartFile imageUserFile,
        @RequestPart(value = "deletePhoto", required = false) String deletePhotoStr,
        HttpServletRequest request) {
        
        try {  
            UpdateUserRequest dto = new UpdateUserRequest();
            if (userJson != null && !userJson.isBlank()) {
                dto = new ObjectMapper().readValue(userJson, UpdateUserRequest.class);
            }
            Boolean deletePhoto = "true".equalsIgnoreCase(deletePhotoStr);
            usersService.updateUser(id, dto, imageUserFile, deletePhoto);
            return ResponseEntity.ok("Utilisateur mis à jour avec succès !");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur : " + e.getMessage());
        }
    }

    // Déverrouillage du compte via le lien de déverrouillage (GET avec paramètres)
    /*@GetMapping("/unlock")
    public ResponseEntity<String> unlock(@RequestParam("email") String email,
                                         @RequestParam("code") String code) {
        try {
            usersService.unlockAccount(email, code);
            return ResponseEntity.ok("Compte déverrouillé avec succès.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }*/



   // Endpoint pour ajouter un utilisateur à l'entreprise de l'Admin

    @PostMapping("/addNewUsers")
    public User addUserToEntreprise(HttpServletRequest request, @RequestBody UserRequest userRequest) {
        return usersService.addUserToEntreprise(request, userRequest);
    }

    //Endpoint pour Delet un user a l'enreprise de l'admin
  @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Map<String, String>> deleteUserFromEntreprise(
            HttpServletRequest request,
            @PathVariable Long userId) {

        Map<String, String> response = new HashMap<>();
        try {
            usersService.deleteUserFromEntreprise(request, userId);
            response.put("message", "Utilisateur supprimé avec succès.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }



    @GetMapping("/user/info")
    public ResponseEntity<Object> getUserInfo(HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            UserRequest userInfo = usersService.getInfo(user.getId());
            return ResponseEntity.ok(userInfo);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

 @PostMapping("/{userId}/permissions")
public ResponseEntity<UserDTO> assignPermissionsToUser(
        @PathVariable Long userId,
        @RequestBody AssignPermissionsRequest request,
        HttpServletRequest httpRequest) {

    UserDTO updatedUserDTO = usersService.assignPermissionsToUser(userId, request, httpRequest);
    return ResponseEntity.ok(updatedUserDTO);
}



    //Get all users
 @GetMapping("/entreprise/{entrepriseId}/allusers")
    public ResponseEntity<List<UserDTO>> getAllUsersOfEntreprise(@PathVariable Long entrepriseId, HttpServletRequest request) {
        try {
            List<UserDTO> users = usersService.getAllUsersOfEntreprise(request);
            
            if (users.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(List.of());
        }
    }

 

    //Endpoint Get user by id
       @GetMapping("/user/{userId}")
        public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId, HttpServletRequest request) {
            UserDTO userDTO = usersService.getUserById(userId, request);
            return ResponseEntity.ok(userDTO);
        }




    // Endpoint pour suspendre ou réactiver un utilisateur
    @PutMapping("/suspendre/{userId}")
    public ResponseEntity<String> suspendUser(
            HttpServletRequest request,
            @PathVariable Long userId,
            @RequestParam boolean suspend) {
        usersService.suspendUser(request, userId, suspend);
        String message = suspend ? "Utilisateur suspendu avec succès." : "Utilisateur réactivé avec succès.";
        return ResponseEntity.ok(message);
    }

    //Endpoint qui recupere linformation de lentrprise de user connecter
      @GetMapping("/myEntreprise")
        public ResponseEntity<EntrepriseDTO> getEntrepriseOfConnectedUser(HttpServletRequest request) {
            EntrepriseDTO entrepriseDTO = usersService.getEntrepriseOfConnectedUser(request);
            return ResponseEntity.ok(entrepriseDTO);
        }

        // Endpoint pour nombre de utilisateurs dans l'entreprise countUsersInEntreprise
        @GetMapping("/entreprise/countUsers")
        public ResponseEntity<Long> countUsersInEntreprise(HttpServletRequest request) {
            long count = usersService.countUsersInEntreprise(request);
            return ResponseEntity.ok(count);
        }

        // Endpoint pour récupérer toutes les données du dashboard
        @GetMapping("/compte/dashboard")
        public ResponseEntity<?> getDashboard(HttpServletRequest request) {
            try {
                UserOptimalDTO dashboard = usersService.getDashboardData(request);
                return ResponseEntity.ok(dashboard);
            } catch (RuntimeException e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            } catch (Exception e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Erreur interne du serveur : " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        }

        // Endpoint pour fermer une session spécifique avant login (quand limite atteinte)
        @PostMapping("/sessions/close-before-login")
        public ResponseEntity<Map<String, String>> closeSessionBeforeLogin(
                @RequestBody Map<String, String> request) {
            try {
                String email = request.get("email");
                String password = request.get("password");
                String sessionIdStr = request.get("sessionId");
                
                if (email == null || password == null || sessionIdStr == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Email, mot de passe et sessionId sont requis");
                    return ResponseEntity.badRequest().body(error);
                }
                
                Long sessionId = Long.parseLong(sessionIdStr);
                usersService.closeSessionBeforeLogin(email, password, sessionId);
                
                Map<String, String> response = new HashMap<>();
                response.put("message", "Session fermée avec succès. Vous pouvez maintenant vous connecter.");
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", e.getMessage() != null ? e.getMessage() : "Erreur lors de la fermeture de la session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
        }
        
        // Endpoint pour lister toutes les sessions actives de l'utilisateur
        @GetMapping("/sessions")
        public ResponseEntity<List<com.xpertcash.DTOs.UserSessionDTO>> getActiveSessions(HttpServletRequest request) {
            try {
                List<com.xpertcash.DTOs.UserSessionDTO> sessions = usersService.getActiveSessions(request);
                return ResponseEntity.ok(sessions);
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(List.of());
            }
        }

        // Endpoint pour révoquer une session spécifique
        @DeleteMapping("/sessions/{sessionId}")
        public ResponseEntity<Map<String, String>> revokeSession(
                @PathVariable Long sessionId,
                HttpServletRequest request) {
            Map<String, String> response = new HashMap<>();
            try {
                usersService.revokeSession(sessionId, request);
                response.put("message", "Session révoquée avec succès.");
                return ResponseEntity.ok(response);
            } catch (RuntimeException e) {
                response.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }

        // Endpoint pour révoquer toutes les sessions sauf la session courante
        @DeleteMapping("/sessions/others")
        public ResponseEntity<Map<String, String>> revokeOtherSessions(HttpServletRequest request) {
            Map<String, String> response = new HashMap<>();
            try {
                usersService.revokeOtherSessions(request);
                response.put("message", "Toutes les autres sessions ont été révoquées avec succès.");
                return ResponseEntity.ok(response);
            } catch (RuntimeException e) {
                response.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }

        // Endpoint pour récupérer les utilisateurs ayant la permission de vendre
        @GetMapping("/vendeurs")
        public ResponseEntity<?> getVendeurs(HttpServletRequest request) {
            try {
                return ResponseEntity.ok(usersService.getVendeurs(request));
            } catch (RuntimeException e) {
                Map<String, String> response = new HashMap<>();
                response.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }
 
}
