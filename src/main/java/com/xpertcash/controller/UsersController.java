package com.xpertcash.controller;
import com.xpertcash.DTOs.UpdateUserRequest;
import com.xpertcash.DTOs.USER.RegisterResponse;
import com.xpertcash.DTOs.USER.ResendActivationRequest;
import com.xpertcash.DTOs.USER.UserDTO;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.DTOs.UserOptimalDTO;
import com.xpertcash.configuration.JwtConfig;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.DTOs.LoginRequest;
import com.xpertcash.DTOs.RegistrationRequest;
import com.xpertcash.service.UsersService;
import com.xpertcash.service.AuthenticationHelper;
import com.xpertcash.composant.Utilitaire;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
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
    private JwtUtil jwtUtil;
    @Autowired 
    JwtConfig jwtConfig;
    @Autowired
    private Utilitaire utilitaire;
    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private UsersRepository usersRepository;

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
        // Ici, response.success est déjà true ou false selon l'envoi de l'email
        return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
        // Erreurs métier (email, téléphone, entreprise déjà existants, rôle manquant, etc.)
        response.setSuccess(false);
        response.setMessage("L'inscription a échoué. Cause: " + e.getMessage());
        response.setUser(null);
        return ResponseEntity.badRequest().body(response);
    }
}


    // Connexion
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        try {
            Map<String, String> tokens = usersService.login(request.getEmail(), request.getPassword());
            tokens.put("message", "Connexion réussie");
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }


   
    // Endpoint pour renvoyer l'email d'activation
    @PostMapping("/resend-activation")
    public ResponseEntity<Map<String, String>> resendActivation(@RequestBody ResendActivationRequest request) {
    usersService.resendActivationEmail(request.getEmail());

    Map<String, String> response = new HashMap<>();
    response.put("message", "Email d’activation renvoyé avec succès.");
    response.put("email", request.getEmail());

     return ResponseEntity.ok(response);
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        System.out.println("🔁 Refresh Token reçu : " + refreshToken);

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le refresh token est manquant"));
        }

        try {
            Claims claims = jwtUtil.extractAllClaimsSafe(refreshToken);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Refresh token expiré ou invalide")
                );
            }

            Long userId = Long.parseLong(claims.getSubject());

            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            User admin = user.getEntreprise().getAdmin();
            boolean within24Hours = LocalDateTime.now().isBefore(user.getCreatedAt().plusHours(24));

            String newAccessToken = usersService.generateAccessToken(user, admin, within24Hours);

            return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "message", "Nouveau token généré avec succès"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("error", "Erreur lors du traitement du refresh token")
            );
        }
    }



    // Activation du compte via le lien d'activation (GET avec paramètres dans l'URL)
    @GetMapping("/activate")
    public ResponseEntity<String> activate(@RequestParam("email") String email,
                                           @RequestParam("code") String code) {
        try {
            usersService.activateAccount(email, code);
            return ResponseEntity.ok("Compte activé avec succès.");
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
            // Utiliser AuthenticationHelper avec fallback pour transition douce
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
        @RequestBody Map<PermissionType, Boolean> permissions,
        HttpServletRequest request) {

    UserDTO updatedUserDTO = usersService.assignPermissionsToUser(userId, permissions, request);
    return ResponseEntity.ok(updatedUserDTO);
}



    //Get all users
 @GetMapping("/entreprise/{entrepriseId}/allusers")
    public ResponseEntity<List<UserDTO>> getAllUsersOfEntreprise(@PathVariable Long entrepriseId, HttpServletRequest request) {
        try {
            // Appel du service pour récupérer la liste des utilisateurs transformée en UserDTO
            List<UserDTO> users = usersService.getAllUsersOfEntreprise(request);
            
            // Si la liste est vide
            if (users.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            
            // Retour de la liste des utilisateurs avec un statut HTTP 200 OK
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            // Si une exception est levée dans le service (ex : token invalide, permissions manquantes)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(List.of()); // Retourner une erreur claire ici si nécessaire
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
 
}
