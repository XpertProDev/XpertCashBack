package com.xpertcash.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.xpertcash.DTOs.FactureDTO;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.User;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.FactureRepository;
import com.xpertcash.service.AuthenticationHelper;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class facturesController {

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private AuthenticationHelper authHelper;

    @GetMapping("/factures")
    public ResponseEntity<?> getAllFactures(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        // Récupérer toutes les factures de l'entreprise (isolé)
        List<Facture> factures = factureRepository.findAllByEntrepriseId(entreprise.getId());

        if (factures.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Aucune facture disponible.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        List<FactureDTO> factureDTOS = factures.stream()
            .map(FactureDTO::new)
            .collect(Collectors.toList());

        return ResponseEntity.ok(factureDTOS);
    }

    @GetMapping("/factures/{boutiqueId}")
    public ResponseEntity<?> getFacturesByBoutique(@PathVariable Long boutiqueId, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        // Vérifier que la boutique appartient à l'entreprise de l'utilisateur
        boutiqueRepository.findByIdAndEntrepriseId(boutiqueId, entreprise.getId())
                .orElseThrow(() -> new RuntimeException("Boutique introuvable ou n'appartient pas à votre entreprise"));

        // Récupérer les factures de la boutique (isolé par entreprise)
        List<Facture> factures = factureRepository.findByBoutiqueIdAndEntrepriseId(boutiqueId, entreprise.getId());

        System.out.println("Nombre de factures trouvées pour la boutique " + boutiqueId + " : " + factures.size());

        if (factures.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Aucune facture disponible pour cette boutique."));
        }

        List<FactureDTO> factureDTOS = factures.stream()
            .map(FactureDTO::new)
            .collect(Collectors.toList());

        return ResponseEntity.ok(factureDTOS);
    }

    
}
