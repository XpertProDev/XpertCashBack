package com.xpertcash.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.FactureProFormaDTO;
import com.xpertcash.DTOs.FactureProformaPaginatedResponseDTO;
import com.xpertcash.service.AuthenticationHelper;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.MethodeEnvoi;
import com.xpertcash.entity.NoteFactureProForma;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.StatutFactureProForma;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.NoteFactureProFormaRepository;
import com.xpertcash.service.FactureProformaService;
import com.xpertcash.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class FactureProformaController {

      @Autowired
    private FactureProformaService factureProformaService;

    @Autowired
    private FactureProformaRepository factureProformaRepository;
    @Autowired
    private NoteFactureProFormaRepository noteFactureProFormaRepository;

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private MailService mailService;


    // Endpoint pour ajouter une facture pro forma
    @PostMapping("/ajouter")
    public ResponseEntity<?> ajouterFacture(
            @RequestBody FactureProForma facture,
            @RequestParam(defaultValue = "0") Double remisePourcentage,
            @RequestParam(defaultValue = "false") Boolean appliquerTVA,
            HttpServletRequest request) {

        try {
            // Appel du serviceupdateFactureProforma pour ajouter la facture
            FactureProForma nouvelleFacture = factureProformaService.ajouterFacture(facture, remisePourcentage, appliquerTVA, request);

            // Convertir l'entité en DTO pour éviter les problèmes de sérialisation avec les proxies Hibernate
            FactureProFormaDTO factureDTO = new FactureProFormaDTO(nouvelleFacture);

            // Retourner la facture créée en réponse HTTP 201 (CREATED)
            return ResponseEntity.status(HttpStatus.CREATED).body(factureDTO);
        } catch (RuntimeException e) {
            Map<String,String> error = Collections.singletonMap("message", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error);
        }
    }

    
    // Endpoint pour modifier une facture pro forma
    @PutMapping("/updatefacture/{factureId}")
    public ResponseEntity<FactureProFormaDTO> updateFacture(
            @PathVariable Long factureId,
            @RequestParam(required = false) Double remisePourcentage,
            @RequestParam(required = false) Boolean appliquerTVA,
            @RequestParam(required = false) List<Long> idsApprobateurs,
            @RequestBody FactureProForma modifications, HttpServletRequest request) {
    
        FactureProFormaDTO factureModifiee = factureProformaService.modifierFacture(factureId, remisePourcentage, appliquerTVA, modifications,idsApprobateurs, request);
        return ResponseEntity.ok(factureModifiee);
    }
    
    //Suprimer une facture
    @DeleteMapping("/deletefactureproforma/{factureId}")
    public ResponseEntity<?> supprimerFactureProforma(
            @PathVariable("factureId") Long factureId,
            HttpServletRequest request) {
        try {
            factureProformaService.supprimerFactureProforma(factureId, request);

            // ✅ Retourner un objet JSON
            Map<String, String> response = new HashMap<>();
            response.put("message", "Facture supprimée avec succès.");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur lors de la suppression de la facture.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
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

           FactureProForma facture = factureProformaService.getFactureProformaEntityById(id, httpRequest);


            // Validation du statut
            if (facture.getStatut() != StatutFactureProForma.ENVOYE ||
                    facture.getMethodeEnvoi() != MethodeEnvoi.EMAIL) {
                logger.error("Statut/Méthode invalide");
                return ResponseEntity.badRequest().body("Statut/Méthode invalide");
            }

            // Envoi de l'email avec le compte facture
            mailService.sendFactureEmailWithAttachments(to, cc, subject, body,
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
    public ResponseEntity<Object> getFacturesParEntreprise(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        try {
            List<Map<String, Object>> factures = factureProformaService
                    .getFacturesParEntrepriseParUtilisateur(user.getId(), request);
            return ResponseEntity.ok(factures);
        } catch (Exception e) {
            e.printStackTrace(); // Pour loguer l’erreur réelle côté serveur
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne lors de la récupération des factures",
                                "details", e.getMessage()));
        }
    }




    // Endpoint Get bye id
   @GetMapping("/factureProforma/{id}")
    public ResponseEntity<FactureProFormaDTO> getFactureProformaById(@PathVariable Long id, HttpServletRequest request) {
        FactureProFormaDTO factureDTO = factureProformaService.getFactureProformaById(id, request);
        return ResponseEntity.ok(factureDTO);
    }

    
     //Endpoint pour recuperer les notes d'une facture pro forma
     @GetMapping("/factures/{id}/notes")
    public ResponseEntity<List<Map<String, Object>>> getNotesPourFacture(
            @PathVariable Long id,
            HttpServletRequest request) {

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        // Récupération de la facture
        FactureProForma facture = factureProformaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée !"));

        // Vérification de l'appartenance à la même entreprise
        Entreprise entrepriseDeLaFacture = facture.getEntreprise();
        if (entrepriseDeLaFacture == null) {
            throw new RuntimeException("Entreprise non définie pour cette facture.");
        }

        if (!entrepriseDeLaFacture.getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à accéder aux notes de cette facture.");
        }


        // Récupération et simplification des notes
        List<NoteFactureProForma> notes = noteFactureProFormaRepository.findByFacture(facture);

           if (notes.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Aucune note trouvée pour cette facture.");
                return ResponseEntity.status(HttpStatus.OK).body(List.of(response));
            }

        List<Map<String, Object>> notesSimplifiees = notes.stream().map(note -> {
            Map<String, Object> noteMap = new HashMap<>();
            noteMap.put("id", note.getId());
            noteMap.put("auteur", note.getAuteur().getNomComplet());
            noteMap.put("note", note.getContenu());
            noteMap.put("dateCreation", note.getDateCreation());
            noteMap.put("modifiee", note.isModifiee());
            noteMap.put("numeroIdentifiant", note.getNumeroIdentifiant());
            return noteMap;
        }).toList();

        return ResponseEntity.ok(notesSimplifiees);
    }

    // Endpoint pour modifier une note d'une facture pro forma
    @PutMapping("/{factureId}/notes/{noteId}")
    public ResponseEntity<Map<String, String>> modifierNoteFacture(
            @PathVariable Long factureId,
            @PathVariable Long noteId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
 
        String nouveauContenu = requestBody.get("nouveauContenu");

        if (nouveauContenu == null || nouveauContenu.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le contenu de la note ne peut pas être vide."));
        }

        factureProformaService.modifierNoteFacture(factureId, noteId, nouveauContenu, request);

        return ResponseEntity.ok(Map.of("message", "La note a été modifiée avec succès."));
    }

    // Endpoint pour supprimer une note d'une facture pro forma que user lui meme a creer
    @DeleteMapping("/{factureId}/notes/{noteId}/supprimer")
    public ResponseEntity<Map<String, String>> supprimerNoteFacture(
            @PathVariable Long factureId,
            @PathVariable Long noteId,
            HttpServletRequest request
    ) {
        try {
            factureProformaService.supprimerNoteFacture(factureId, noteId, request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Note supprimée avec succès.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    //Endpoint pour get note dune facture by id
    @GetMapping("/{factureId}/notes/{noteId}")
    public ResponseEntity<NoteFactureProForma> getNoteById(
            @PathVariable Long factureId,
            @PathVariable Long noteId,
            HttpServletRequest request) {
        
        NoteFactureProForma note = factureProformaService.getNotesByFactureId(factureId, noteId, request);
        return ResponseEntity.ok(note);
    }

    //Endpoint pour get facture lier a un client ou entreprise client
   @GetMapping("/factures/client")
    public ResponseEntity<List<Map<String, Object>>> getFacturesClient(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long entrepriseClientId) {
        
        List<FactureProForma> factures = factureProformaService.getFacturesParClient(clientId, entrepriseClientId);

        List<Map<String, Object>> result = factures.stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("numeroFacture", f.getNumeroFacture());
            map.put("dateFacture", f.getDateFacture());
            map.put("statut", f.getStatut());
            map.put("totalFacture", f.getTotalFacture());

            if (f.getClient() != null) {
                map.put("clientNom", f.getClient().getNomComplet());
            }

            if (f.getEntrepriseClient() != null) {
                map.put("entrepriseClientNom", f.getEntrepriseClient().getNom());
            }

            return map; 
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }


    // Endpoint pour trier
    @GetMapping("/mes-factures/par-periode")
public ResponseEntity<?> getFacturesParPeriode(
        @RequestParam String type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        HttpServletRequest request
) {
    try {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        List<FactureProFormaDTO> facturesDTO = factureProformaService.getFacturesParPeriode(
                user.getId(), request, type, dateDebut, dateFin
        );

        return ResponseEntity.ok(facturesDTO);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "message", "Une erreur interne est survenue : " + e.getMessage()
        ));
    }
}


    // Endpoint scalable avec pagination pour récupérer les factures proforma de l'utilisateur connecté
    @GetMapping("/mes-factures/paginated")
    public ResponseEntity<FactureProformaPaginatedResponseDTO> getFacturesParEntrepriseParUtilisateurPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        try {
            // Validation des paramètres
            if (page < 0) page = 0;
            if (size <= 0) size = 20;
            if (size > 100) size = 100; // Limite maximale
            
            // Récupérer l'ID de l'utilisateur depuis le token
            User currentUser = authHelper.getAuthenticatedUserWithFallback(request);
            Long userId = currentUser.getId();
            
            FactureProformaPaginatedResponseDTO response = factureProformaService.getFacturesParEntrepriseParUtilisateurPaginated(
                    userId, page, size, request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des factures paginées: " + e.getMessage());
        }
    }

    /**
     * Endpoint pour récupérer les factures proforma en attente
     * @param limit Nombre de factures à retourner (par défaut 10)
     */
    @GetMapping("/factureProforma/en-attente")
    public ResponseEntity<?> getFacturesProformaEnAttente(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        try {
            List<FactureProFormaDTO> factures = factureProformaService.getFacturesProformaEnAttente(limit, request);
            return ResponseEntity.ok(factures);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne du serveur : " + e.getMessage()));
        }
    }

}
    
   
    

