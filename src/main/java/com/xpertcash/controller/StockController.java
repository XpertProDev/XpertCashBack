package com.xpertcash.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.StockService;


@RestController
@RequestMapping("/api/auth")
public class StockController {


    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private UsersRepository usersRepository;
     @Autowired
    private StockService stockService;


    // Ajouter une quantité au stock d'un produit
        @PostMapping("/ajouter-quantite")
        public ResponseEntity<?> ajouterQuantite(
                @RequestHeader("Authorization") String token,
                @RequestBody Map<String, Object> request) {
            try {
                // Extraire l'ID de l'utilisateur à partir du token
                String jwtToken = token.substring(7);
                Long userId = jwtUtil.extractUserId(jwtToken);

                // Vérifier si l'utilisateur existe
                User user = usersRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

                // Vérifier si l'utilisateur est admin
                if (!user.getRole().getName().equals(RoleType.ADMIN)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Collections.singletonMap("error", "Vous n'avez pas les droits nécessaires."));
                }

                // Vérification des permissions pour gérer les produits
                authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

                // Récupérer les paramètres nécessaires
                Long produitId = Long.valueOf(request.get("produitId").toString());
                int quantiteAjoutee = Integer.parseInt(request.get("quantiteAjoutee").toString());

                // Ajouter la quantité au stock
                stockService.ajouterQuantite(produitId, quantiteAjoutee);
                return ResponseEntity.ok(Collections.singletonMap("message", "Quantité ajoutée avec succès."));
            } catch (NotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", e.getMessage()));
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", e.getMessage()));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
            }
        }

    // Mettre à jour la date d'expiration du stock d'un produit
        @PostMapping("/update-expiration")
        public ResponseEntity<?> mettreAJourExpiration(
                @RequestHeader("Authorization") String token,
                @RequestBody Map<String, Object> request) {
            try {
                // Extraire l'ID de l'utilisateur à partir du token
                String jwtToken = token.substring(7);
                Long userId = jwtUtil.extractUserId(jwtToken);

                // Vérifier si l'utilisateur existe
                User user = usersRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

                // Vérifier si l'utilisateur est admin
                if (!user.getRole().getName().equals(RoleType.ADMIN)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Collections.singletonMap("error", "Vous n'avez pas les droits nécessaires."));
                }

                // Vérification des permissions pour gérer les produits
                authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

                // Récupérer les paramètres nécessaires
                Long produitId = Long.valueOf(request.get("produitId").toString());
                LocalDate dateExpiration = LocalDate.parse(request.get("dateExpiration").toString());

                // Mettre à jour la date d'expiration du stock
                stockService.mettreAJourExpiration(produitId, dateExpiration);
                return ResponseEntity.ok(Collections.singletonMap("message", "Date d'expiration mise à jour avec succès."));
            } catch (NotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", e.getMessage()));
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", e.getMessage()));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
            }
        }


        // Endpoint pour récupérer tout le stock
         @GetMapping("/allstock")
         public ResponseEntity<?> recupererToutLeStock(
            @RequestHeader("Authorization") String token) {
        try {
            // Extraire l'ID de l'utilisateur à partir du token
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            // Vérifier si l'utilisateur existe
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            // Vérifier si l'utilisateur est admin
            if (!user.getRole().getName().equals(RoleType.ADMIN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("error", "Vous n'avez pas les droits nécessaires."));
            }

            // Vérification des permissions pour gérer les produits
            authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

            // Récupérer tout le stock
            List<Stock> stocks = stockService.recupererToutLeStock();

            if (stocks.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK) 
                        .body(Collections.singletonMap("message", "Aucun stock disponible."));
            }
            return ResponseEntity.ok(stocks);

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

}
