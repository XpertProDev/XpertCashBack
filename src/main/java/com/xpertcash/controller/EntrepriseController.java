package com.xpertcash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpertcash.DTOs.UpdateEntrepriseDTO;
import com.xpertcash.service.EntrepriseService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class EntrepriseController {

     @Autowired
    private EntrepriseService entrepriseService;
  
    @PatchMapping(value = "/updateEntreprise/{entrepriseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) 
    public ResponseEntity<?> updateEntreprise(
            @PathVariable Long entrepriseId,
            @RequestPart(value = "entreprise", required = false) String entrepriseJson,
            @RequestPart(value = "logo", required = false) MultipartFile imageLogoFile,
            @RequestPart(value = "siganture", required = false) MultipartFile imageSignatureFile,
            @RequestPart(value = "cachet", required = false) MultipartFile imageCachetFile,

            HttpServletRequest request) {
    
        try {
            UpdateEntrepriseDTO dto = new UpdateEntrepriseDTO();
    
            if (entrepriseJson != null && !entrepriseJson.isBlank()) {
                dto = new ObjectMapper().readValue(entrepriseJson, UpdateEntrepriseDTO.class);
            }
    
            entrepriseService.updateEntreprise(entrepriseId, dto, imageLogoFile, imageSignatureFile, imageCachetFile, request);
    
            return ResponseEntity.ok("Entreprise mise à jour avec succès !");
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Accès refusé")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur : " + e.getMessage());
        }
    }
    
   
}
