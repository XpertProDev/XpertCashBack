package com.xpertcash.controller.VENTE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.DTOs.VENTE.FactureVentePaginatedDTO;
import com.xpertcash.DTOs.VENTE.ReceiptEmailRequest;
import com.xpertcash.DTOs.VENTE.StatistiquesVenteGlobalesDTO;
import com.xpertcash.DTOs.VENTE.StatistiquesVendeurDTO;
import com.xpertcash.service.MailService;
import com.xpertcash.service.VENTE.FactureVenteService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class FactureVenteController {

    @Autowired
    private FactureVenteService factureVenteService;

    @Autowired
    private MailService mailService;

    

    @GetMapping("/factureVente/entreprise/paginated")
    public ResponseEntity<FactureVentePaginatedDTO> getFacturesForEntrepriseWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateEmission") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false, defaultValue = "aujourdhui") String periode,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateDebut,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateFin,
            HttpServletRequest request) {
        
        FactureVentePaginatedDTO factures = factureVenteService.getAllFacturesWithPagination(
            page, size, sortBy, sortDir, periode, dateDebut, dateFin, request);
        return ResponseEntity.ok(factures);
    }

    // Endpoint pour envoyer une facture de vente par email (sans pièces jointes - pour compatibilité)
    @PostMapping("/factureVente/envoyer-email")
    public ResponseEntity<?> envoyerFactureVenteEmail(
            @RequestBody ReceiptEmailRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'adresse email est requise");
            }
            
            if (request.getVenteId() == null || request.getVenteId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'ID de la vente est requis");
            }

            ReceiptEmailRequest factureData = factureVenteService.getFactureDataForEmail(
                request.getVenteId(), 
                request.getEmail(),
                httpRequest
            );

            mailService.sendReceiptEmail(factureData);
            
            return ResponseEntity.ok("Facture envoyée par email avec succès");
            
        } catch (MessagingException e) {
            return ResponseEntity.internalServerError()
                .body("Erreur lors de l'envoi de l'email : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Erreur interne du serveur : " + e.getMessage());
        }
    }

    // Endpoint pour envoyer une facture de vente par email avec pièces jointes (PDF)
    @PostMapping(value = "/factureVente/envoyer-email-avec-pieces-jointes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> envoyerFactureVenteEmailAvecPiecesJointes(
            @RequestParam("venteId") String venteId,
            @RequestParam("email") String email,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
            HttpServletRequest httpRequest) {
        
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'adresse email est requise");
            }
            
            if (venteId == null || venteId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'ID de la vente est requis");
            }

            ReceiptEmailRequest factureData = factureVenteService.getFactureDataForEmail(
                venteId, 
                email,
                httpRequest
            );

            List<MultipartFile> attachmentsList = attachments != null 
                ? Arrays.asList(attachments) 
                : Collections.emptyList();

            mailService.sendReceiptEmailWithAttachments(factureData, attachmentsList);
            
            return ResponseEntity.ok("Facture envoyée par email avec succès");
            
        } catch (MessagingException e) {
            return ResponseEntity.internalServerError()
                .body("Erreur lors de l'envoi de l'email : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Erreur interne du serveur : " + e.getMessage());
        }
    }

    /**
     * Endpoint pour récupérer les statistiques globales de vente
     * Inclut : Total ventes, Nombre d'articles, Montant Total, Top 3 produits, Top 3 vendeurs
     * 
     * periode Filtre de période : aujourdhui, hier, semaine, mois, annee (défaut: aujourdhui)
     */
    @GetMapping("/vente/statistiques/globales")
    public ResponseEntity<?> getStatistiquesGlobales(
            @RequestParam(required = false, defaultValue = "aujourdhui") String periode,
            HttpServletRequest request) {
        try {
            StatistiquesVenteGlobalesDTO statistiques = factureVenteService.getStatistiquesGlobales(periode, request);
            return ResponseEntity.ok(statistiques);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(java.util.Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pour récupérer les statistiques de vente d'un vendeur spécifique
     * Inclut : Infos vendeur, Total ventes, Nombre d'articles, Montant Total, Produits vendus
     * 
     * vendeurId ID du vendeur
     * periode Filtre de période : aujourdhui, hier, semaine, mois, annee (défaut: aujourdhui)
     */
    @GetMapping("/vente/statistiques/vendeur/{vendeurId}")
    public ResponseEntity<?> getStatistiquesVendeur(
            @org.springframework.web.bind.annotation.PathVariable Long vendeurId,
            @RequestParam(required = false, defaultValue = "aujourdhui") String periode,
            HttpServletRequest request) {
        try {
            StatistiquesVendeurDTO statistiques = factureVenteService.getStatistiquesVendeur(vendeurId, periode, request);
            return ResponseEntity.ok(statistiques);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(java.util.Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }

}
