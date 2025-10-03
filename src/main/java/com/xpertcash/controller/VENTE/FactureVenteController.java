package com.xpertcash.controller.VENTE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.VENTE.FactureVentePaginatedDTO;
import com.xpertcash.DTOs.VENTE.ReceiptEmailRequest;
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
            HttpServletRequest request) {
        
        FactureVentePaginatedDTO factures = factureVenteService.getAllFacturesWithPagination(
            page, size, sortBy, sortDir, request);
        return ResponseEntity.ok(factures);
    }

    // Endpoint pour envoyer une facture de vente par email
    @PostMapping("/factureVente/envoyer-email")
    public ResponseEntity<?> envoyerFactureVenteEmail(
            @RequestBody ReceiptEmailRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Validation des données requises
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'adresse email est requise");
            }
            
            if (request.getVenteId() == null || request.getVenteId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'ID de la vente est requis");
            }

            // Récupérer les données complètes de la facture
            ReceiptEmailRequest factureData = factureVenteService.getFactureDataForEmail(
                request.getVenteId(), 
                request.getEmail()
            );

            // Envoi de l'email
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

}
