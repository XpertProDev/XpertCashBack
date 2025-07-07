package com.xpertcash.controller;

//import com.xpertcash.DTOs.RegistrationResponse;
import com.xpertcash.DTOs.UpdateUserRequest;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;
import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.DTOs.LoginRequest;
import com.xpertcash.DTOs.RegistrationRequest;
import com.xpertcash.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;



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
    private AuthorizationService authorizationService;

    // Inscription
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegistrationRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            usersService.registerUsers(
                    request.getNomComplet(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getPhone(),
                    request.getPays(),
                    request.getNomEntreprise(),
                    request.getNomBoutique()
            );

            response.put("message", "Compte créé avec succès. Un lien d'activation vous a été envoyé par email.");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.err.println("Erreur lors de l'inscription : " + e.getMessage());

            response.put("error", "L'inscription a échoué. Veuillez vérifier votre connexion Internet ou réessayer plus tard.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Connexion
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
                try {
                    String token = usersService.login(request.getEmail(), request.getPassword());

                    // Construire une réponse JSON avec le message et le token
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Connexion réussie");
                    response.put("token", token);

                    return ResponseEntity.ok(response);
                } catch (Exception e) {
                    // Construire une réponse JSON en cas d'erreur
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", e.getMessage());

                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
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
     public ResponseEntity<?>updateUser(
            @PathVariable Long id,
            @RequestPart(value = "user", required = false) String userJson,
            @RequestPart(value = "photo", required = false) MultipartFile imageUserFile,
            HttpServletRequest request) {
        try {  
            UpdateUserRequest dto = new UpdateUserRequest();
            if (userJson != null && !userJson.isBlank()) {
                dto = new ObjectMapper().readValue(userJson, UpdateUserRequest.class);
            }
            usersService.updateUser(id, dto, imageUserFile);
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
    public ResponseEntity<String> deleteUserFromEntreprise(HttpServletRequest request, @PathVariable Long userId) {
        usersService.deleteUserFromEntreprise(request, userId);
        return ResponseEntity.ok("Utilisateur supprimé avec succès.");
    }


    @GetMapping("/user/info")
    public ResponseEntity<Object> getUserInfo(@RequestHeader("Authorization") String token) {
        String jwtToken = token.substring(7); // Enlever "Bearer "
        Long userId = jwtUtil.extractUserId(jwtToken);

        try {
            UserRequest userInfo = usersService.getInfo(userId);
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
    public ResponseEntity<User> assignPermissionsToUser(
            @PathVariable Long userId,
            @RequestBody Map<PermissionType, Boolean> permissions) {
        User updatedUser = usersService.assignPermissionsToUser(userId, permissions);
        return ResponseEntity.ok(updatedUser);
    }

    //Get all users
    @GetMapping("/entreprise/{entrepriseId}/allusers")
    public ResponseEntity<List<User>> getAllUsersOfEntreprise(@PathVariable Long entrepriseId) {
        List<User> users = usersService.getAllUsersOfEntreprise(entrepriseId);
        return ResponseEntity.ok(users);
    }

    //Endpoint Get user by id
        @GetMapping("/user/{userId}")
        public ResponseEntity<User> getUserById(@PathVariable Long userId, HttpServletRequest request) {
            User user = usersService.getUserById(userId, request);
            return ResponseEntity.ok(user);
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

    
}
