package com.xpertcash.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.SuperAdminEntrepriseStatsDTO;
import com.xpertcash.entity.User;
import com.xpertcash.service.AuthenticationHelper;
import com.xpertcash.service.SuperAdminService;

import jakarta.servlet.http.HttpServletRequest;

 // Endpoints réservés au SUPER_ADMIN (propriétaire de la plateforme).
  
@RestController
@RequestMapping("/api/auth")
public class SuperAdminController {

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private AuthenticationHelper authHelper;

     // Retourne les infos de l'utilisateur SUPER_ADMIN connecté.
  
    @GetMapping("/me")
    public ResponseEntity<?> getSuperAdminInfo(HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            superAdminService.ensureSuperAdmin(user);

            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "uuid", user.getUuid(),
                    "email", user.getEmail(),
                    "nomComplet", user.getNomComplet(),
                    "role", user.getRole().getName().name()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne : " + e.getMessage()));
        }
    }

     // Liste toutes les entreprises (vue globale) – réservé au SUPER_ADMIN.
  
    @GetMapping("/allentreprises")
    public ResponseEntity<?> getAllEntreprises(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            var entreprisesPage = superAdminService.getAllEntreprisesAsSuperAdmin(user, page, size);

            long totalUsersAllEntreprises = entreprisesPage.getContent().stream()
                    .mapToLong(dto -> dto.getNombreUtilisateursEntreprise())
                    .sum();

            return ResponseEntity.ok(Map.of(
                    "totalEntreprises", entreprisesPage.getTotalElements(),
                    "totalUsersAllEntreprises", totalUsersAllEntreprises,
                    "content", entreprisesPage.getContent()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne : " + e.getMessage()));
        }
    }

     // Désactiver une entreprise (réservé au SUPER_ADMIN).
  
    @PatchMapping("/entreprises/{entrepriseId}/desactiver")
    public ResponseEntity<?> desactiverEntreprise(@PathVariable Long entrepriseId, HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            superAdminService.desactiverEntreprise(user, entrepriseId);
            return ResponseEntity.ok(Map.of("message", "Entreprise désactivée avec succès."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne : " + e.getMessage()));
        }
    }

     // Réactiver une entreprise (réservé au SUPER_ADMIN).
  
    @PatchMapping("/entreprises/{entrepriseId}/activer")
    public ResponseEntity<?> activerEntreprise(@PathVariable Long entrepriseId, HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            superAdminService.activerEntreprise(user, entrepriseId);
            return ResponseEntity.ok(Map.of("message", "Entreprise réactivée avec succès."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne : " + e.getMessage()));
        }
    }

    /**
     * Récupérer une entreprise par son id avec toutes les statistiques globales
     * (réservé au SUPER_ADMIN).
     */
    @GetMapping("/entreprises/{entrepriseId}/stats")
    public ResponseEntity<?> getEntrepriseStats(@PathVariable Long entrepriseId, HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            SuperAdminEntrepriseStatsDTO stats = superAdminService.getEntrepriseStats(user, entrepriseId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne : " + e.getMessage()));
        }
    }

     // Supprime un Admin et TOUTES les données associées à son entreprise.
  
    @DeleteMapping("/deleteAdminAndEntreprise/{adminId}")
    public ResponseEntity<?> deleteAdminAndEntreprise(@PathVariable Long adminId, HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            superAdminService.deleteAdminAndEntreprise(user, adminId);
            return ResponseEntity.ok(Map.of("message", "Admin et toutes les données de son entreprise ont été supprimés avec succès."));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Accès refusé")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Erreur lors de la suppression : " + e.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression : " + e.getMessage()));
        }
    }

    
     // Supprime toutes les données de l'entreprise mais garde l'admin et l'entreprise.
  
    @DeleteMapping("/vider-entreprise")
    public ResponseEntity<?> viderEntrepriseButKeepAdmin(HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            superAdminService.deleteEntrepriseDataButKeepAdmin(user);
            return ResponseEntity.ok(Map.of("message", "Toutes les données de l'entreprise ont été supprimées. L'admin et l'entreprise ont été conservés."));
        } catch (RuntimeException e) {
            // Différencier les erreurs d'accès (403) des autres erreurs (500)
            if (e.getMessage() != null && e.getMessage().contains("Accès refusé")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression : " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression : " + e.getMessage()));
        }
    }

     // Déconnecte tous les utilisateurs du système (réservé au SUPER_ADMIN).
  
    @PostMapping("/deconnecter-tous")
    public ResponseEntity<?> deconnecterTousLesUtilisateurs(HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            superAdminService.deconnecterTousLesUtilisateurs(user);
            return ResponseEntity.ok(Map.of("message", "Tous les utilisateurs ont été déconnectés avec succès. Ils devront se reconnecter."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la déconnexion : " + e.getMessage()));
        }
    }
}


