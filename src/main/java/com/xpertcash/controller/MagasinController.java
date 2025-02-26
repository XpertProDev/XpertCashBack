package com.xpertcash.controller;

import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Magasin;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.MagasinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/")
public class MagasinController {

    @Autowired
    private MagasinService magasinService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthorizationService authorizationService;


    @Autowired
    private UsersRepository usersRepository;

    //Ajouter un magasin (Admin seulement)
    @PostMapping("/ajouterMagasin")
    public ResponseEntity<?> ajouterMagasin(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        try {
            // Extraire l'ID de l'utilisateur à partir du token
            String jwtToken = token.substring(7); // Supposons que le token commence par "Bearer "
            Long userId = jwtUtil.extractUserId(jwtToken);

            // Vérifier si l'utilisateur est un administrateur
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            if (!user.getRole().getName().equals(RoleType.ADMIN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous n'avez pas les droits nécessaires.");
            }

            // Extraire le nom du magasin à partir du corps de la requête
            String nomMagasin = (String) body.get("nomMagasin");

            if (nomMagasin == null || nomMagasin.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Le nom du magasin est requis.");
            }

            // Appel du service pour ajouter un magasin
            Magasin magasin = magasinService.ajouterMagasin(userId, nomMagasin);
            return ResponseEntity.ok(magasin);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Erreur d'authentification : " + e.getMessage());
        }
    }


    // Récupérer tous les magasins d'une entreprise
    @GetMapping("/list")
    public ResponseEntity<?> getMagasinsByEntreprise(@RequestHeader("Authorization") String token) {
        try {
            // ✅ Extraire l'ID utilisateur du token
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            List<Magasin> magasins = magasinService.getMagasinsByEntreprise(userId);
            return ResponseEntity.ok(magasins);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Erreur d'authentification : " + e.getMessage());
        }
    }


    // Supprimer un magasin (Admin seulement)
    @DeleteMapping("/supprimer/{magasinId}")
    public ResponseEntity<?> supprimerMagasin(
            @RequestHeader("Authorization") String token,
            @PathVariable Long magasinId) {
        try {
            // Extraire l'ID utilisateur du token
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            // Vérifier si l'utilisateur est un administrateur
            if (user.getRole().getName() != RoleType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous n'avez pas les droits nécessaires.");
            }

            magasinService.supprimerMagasin(userId, magasinId);
            return ResponseEntity.ok("Magasin supprimé avec succès.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Erreur d'authentification : " + e.getMessage());
        }
    }


    //Modifier Magasin
    @PostMapping("/modifierMagasin/{magasinId}")
    public ResponseEntity<?> modifierMagasin(
            @RequestHeader("Authorization") String token,
            @PathVariable Long magasinId,
            @RequestBody Map<String, Object> body) {
        try {
            // Extraire l'ID de l'utilisateur à partir du token
            String jwtToken = token.substring(7); // Supposons que le token commence par "Bearer "
            Long userId = jwtUtil.extractUserId(jwtToken);

            // Vérifier si l'utilisateur est un administrateur
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            if (!user.getRole().getName().equals(RoleType.ADMIN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous n'avez pas les droits nécessaires.");
            }

            // Extraire le nouveau nom du magasin à partir du corps de la requête
            String nouveauNomMagasin = (String) body.get("nouveauNomMagasin");

            if (nouveauNomMagasin == null || nouveauNomMagasin.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Le nouveau nom du magasin est requis.");
            }

            // Appel du service pour modifier le magasin
            Magasin magasin = magasinService.modifierMagasin(userId, magasinId, nouveauNomMagasin);
            return ResponseEntity.ok(magasin);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Erreur d'authentification : " + e.getMessage());
        }
    }


    //Get Magasin par son ID:
    @GetMapping("/magasin/{magasinId}")
    public ResponseEntity<?> getMagasinById(
            @RequestHeader("Authorization") String token,
            @PathVariable Long magasinId) {
        try {
            // Extraire l'ID de l'utilisateur à partir du token
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            // Vérifier si l'utilisateur est administrateur
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            if (!user.getRole().getName().equals(RoleType.ADMIN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous n'avez pas les droits nécessaires.");
            }

            // Appel du service pour récupérer le magasin par ID
            Magasin magasin = magasinService.getMagasinById(magasinId);
            return ResponseEntity.ok(magasin);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Erreur d'authentification : " + e.getMessage());
        }
    }


    //Get Magasin par son Nom:
    @GetMapping("/magasin")
    public ResponseEntity<?> getMagasinByNom(@RequestParam String nomMagasin) {
        try {
            // Appel du service pour récupérer le magasin par son nom
            Magasin magasin = magasinService.getMagasinByNom(nomMagasin);
            return ResponseEntity.ok(magasin);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Magasin non trouvé.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la récupération du magasin.");
        }
    }


}
