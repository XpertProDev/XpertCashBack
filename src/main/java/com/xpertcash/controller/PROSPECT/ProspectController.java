package com.xpertcash.controller.PROSPECT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.PROSPECT.ConvertProspectRequestDTO;
import com.xpertcash.DTOs.PROSPECT.CreateInteractionRequestDTO;
import com.xpertcash.DTOs.PROSPECT.CreateProspectRequestDTO;
import com.xpertcash.DTOs.PROSPECT.InteractionDTO;
import com.xpertcash.DTOs.PROSPECT.ProspectDTO;
import com.xpertcash.DTOs.PROSPECT.ProspectPaginatedResponseDTO;
import com.xpertcash.DTOs.PROSPECT.UpdateProspectRequestDTO;
import com.xpertcash.service.PROSPECT.ProspectService;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class ProspectController {

    @Autowired
    private ProspectService prospectService;

    /**
     * Créer un nouveau prospect
     */
    @PostMapping("/creat-prospects")
    public ResponseEntity<?> createProspect(@RequestBody CreateProspectRequestDTO request, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            ProspectDTO prospect = prospectService.createProspect(request, httpRequest);
            response.put("message", "Prospect créé avec succès");
            response.put("prospect", prospect);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de la création du prospect: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer un prospect par son ID avec ses interactions
     */
    @GetMapping("/prospects/{id}")
    public ResponseEntity<?> getProspectById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            ProspectDTO prospect = prospectService.getProspectById(id, httpRequest);
            response.put("prospect", prospect);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de la récupération du prospect: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer tous les prospects avec pagination
     */
    @GetMapping("/Allprospects")
    public ResponseEntity<?> getAllProspects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            ProspectPaginatedResponseDTO prospects = prospectService.getAllProspects(page, size, httpRequest);
            response.put("prospects", prospects);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de la récupération des prospects: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }




    /**
     * Mettre à jour un prospect
     */
    @PutMapping("/Updateprospects/{id}")
    public ResponseEntity<?> updateProspect(
            @PathVariable Long id,
            @RequestBody UpdateProspectRequestDTO request,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            ProspectDTO prospect = prospectService.updateProspect(id, request, httpRequest);
            response.put("message", "Prospect mis à jour avec succès");
            response.put("prospect", prospect);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de la mise à jour du prospect: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Supprimer un prospect
     */
    @DeleteMapping("/deletprospects/{id}")
    public ResponseEntity<?> deleteProspect(@PathVariable Long id, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            prospectService.deleteProspect(id, httpRequest);
            response.put("message", "Prospect supprimé avec succès");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de la suppression du prospect: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Ajouter une interaction à un prospect
     */
    @PostMapping("/add-prospects/{prospectId}/interactions")
    public ResponseEntity<?> addInteraction(
            @PathVariable Long prospectId,
            @RequestBody CreateInteractionRequestDTO request,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            InteractionDTO interaction = prospectService.addInteraction(prospectId, request, httpRequest);
            response.put("message", "Interaction ajoutée avec succès");
            response.put("interaction", interaction);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de l'ajout de l'interaction: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les interactions d'un prospect
     */
    @GetMapping("/prospects/{prospectId}/interactions")
    public ResponseEntity<?> getProspectInteractions(@PathVariable Long prospectId, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<InteractionDTO> interactions = prospectService.getProspectInteractions(prospectId, httpRequest);
            response.put("interactions", interactions);
            response.put("prospectId", prospectId);
            response.put("total", interactions.size());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de la récupération des interactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Supprimer une interaction
     */
    @DeleteMapping("/delet-interactions/{interactionId}")
    public ResponseEntity<?> deleteInteraction(@PathVariable Long interactionId, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            prospectService.deleteInteraction(interactionId, httpRequest);
            response.put("message", "Interaction supprimée avec succès");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de la suppression de l'interaction: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Convertir un prospect en client (après achat)
     */
    @PostMapping("/convert-prospect/{prospectId}")
    public ResponseEntity<?> convertProspectToClient(
            @PathVariable Long prospectId, 
            @RequestBody ConvertProspectRequestDTO conversionRequest,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> conversionResult = prospectService.convertProspectToClient(prospectId, conversionRequest, httpRequest);
            return ResponseEntity.ok(conversionResult);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de la conversion du prospect: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


   


    /**
     * Ajouter un nouvel achat à un prospect déjà converti
     */
    @PostMapping("/add-achat-prospect/{prospectId}")
    public ResponseEntity<?> addAchatToConvertedProspect(
            @PathVariable Long prospectId,
            @RequestBody ConvertProspectRequestDTO conversionRequest,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = prospectService.addAchatToConvertedProspect(prospectId, conversionRequest, httpRequest);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de l'ajout de l'achat: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
