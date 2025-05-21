package com.xpertcash.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.USER.factureProEmail.EmailRequest;
import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.MethodeEnvoi;
import com.xpertcash.entity.StatutFactureProForma;
import com.xpertcash.service.FactureProformaService;
import com.xpertcash.service.MailService;
import com.xpertcash.service.UsersService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class FactureProformaController {

      @Autowired
    private FactureProformaService factureProformaService;

      @Autowired
    private UsersService usersService;
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MailService mailService;


    // Endpoint pour ajouter une facture pro forma
    @PostMapping("/ajouter")
    public ResponseEntity<?> ajouterFacture(
            @RequestBody FactureProForma facture,
            @RequestParam(defaultValue = "0") Double remisePourcentage,
            @RequestParam(defaultValue = "false") Boolean appliquerTVA,
            @RequestHeader("Authorization") String token,  // Récupération du token depuis l'en-tête
            HttpServletRequest request) {  // Passage du HttpServletRequest complet

        try {
            // Ajouter le token dans l'en-tête de la requête
            request.setAttribute("Authorization", token);

            // Appel du service pour ajouter la facture, en passant la requête avec le token
            FactureProForma nouvelleFacture = factureProformaService.ajouterFacture(facture, remisePourcentage, appliquerTVA, request);

            // Retourner la facture créée en réponse HTTP 201 (CREATED)
            return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleFacture);
        } catch (RuntimeException e) {
            Map<String,String> error = Collections.singletonMap("message", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error);
        }
    }

    
    
    // Endpoint pour modifier une facture pro forma
    @PutMapping("/updatefacture/{factureId}")
    public ResponseEntity<FactureProForma> updateFacture(
            @PathVariable Long factureId,
            @RequestParam(required = false) Double remisePourcentage,
            @RequestParam(required = false) Boolean appliquerTVA,
            @RequestParam(required = false) List<Long> idsApprobateurs,
            @RequestBody FactureProForma modifications, HttpServletRequest request) {
    
        FactureProForma factureModifiee = factureProformaService.modifierFacture(factureId, remisePourcentage, appliquerTVA, modifications,idsApprobateurs, request);
        return ResponseEntity.ok(factureModifiee);
    }

    
    //Endpoint Pour Envoyer une facture
    @PostMapping(value = "/factures/{id}/envoyer-email", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> envoyerFactureEmail(
            @PathVariable Long id,
            @RequestParam("to") String to,
            @RequestParam("cc") String cc,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
            HttpServletRequest httpRequest) {

        final Logger logger = LoggerFactory.getLogger(getClass());

        try {
            logger.info("=== REQUÊTE REÇUE ===");
            logger.info("ID Facture: {}", id);
            logger.info("Destinataire: {}", to);
            logger.info("Copie: {}", cc);
            logger.info("Sujet: {}", subject);
            logger.info("Corps (taille): {}", body.length());
            logger.info("Pièces jointes: {}", attachments != null ? attachments.length : 0);

            FactureProForma facture = factureProformaService.getFactureProformaById(id, httpRequest);

            // Validation du statut
            if (facture.getStatut() != StatutFactureProForma.ENVOYE ||
                    facture.getMethodeEnvoi() != MethodeEnvoi.EMAIL) {
                logger.error("Statut/Méthode invalide");
                return ResponseEntity.badRequest().body("Statut/Méthode invalide");
            }

            // Envoi de l'email
            mailService.sendEmailWithAttachments(to,cc, subject, body,
                    attachments != null ? Arrays.asList(attachments) : Collections.emptyList());

            logger.info("Email envoyé avec succès");
            return ResponseEntity.ok("Email envoyé");

        } catch (Exception e) {
            logger.error("ERREUR D'ENVOI", e);
            return ResponseEntity.internalServerError().body("Erreur: " + e.getMessage());
        }
    }



    // Endpoint pour recuperer la liste des factures pro forma dune entreprise
    @GetMapping("/mes-factures")
    public ResponseEntity<List<Map<String, Object>>> getFacturesParEntreprise(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    
        Long userId;
        try {
            userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    
        try {
            List<Map<String, Object>> factures = factureProformaService.getFacturesParEntrepriseParUtilisateur(userId);
            return ResponseEntity.ok(factures);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    

    // Endpoint Get bye id
    @GetMapping("/factureProforma/{id}")
    public ResponseEntity<FactureProForma> getFactureProformaById(@PathVariable Long id, HttpServletRequest request) {
        FactureProForma facture = factureProformaService.getFactureProformaById(id, request);
        return ResponseEntity.ok(facture);
    }
    
    


}
