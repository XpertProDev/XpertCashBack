package com.xpertcash.controller;

import com.xpertcash.DTOs.TransfertProduitDTO;
import com.xpertcash.entity.TransfertHistorique;
import com.xpertcash.entity.TransfertProduit;
import com.xpertcash.repository.TransfertHistoriqueRepository;
import com.xpertcash.repository.TransfertProduitRepository;
import com.xpertcash.service.TransfertProduitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/")
public class TransfertProduitController {
    @Autowired
    private TransfertProduitService transfertProduitService;

    @Autowired
    private TransfertProduitRepository transfertProduitRepository;
    @Autowired
    private TransfertHistoriqueRepository transfertHistoriqueRepository;

    // Modifié pour accepter les données en format JSON via le DTO
    @PostMapping("/transfertProduit")
    public ResponseEntity<String> transfererProduit(@RequestBody TransfertProduitDTO transfertProduitDTO) {
        try {
            // Utilisation du DTO pour appeler la logique de transfert
            transfertProduitService.transfererProduit(
                    transfertProduitDTO.getMagasinId(),
                    transfertProduitDTO.getBoutiqueId(),
                    transfertProduitDTO.getProduitId(),
                    transfertProduitDTO.getQuantite()
            );
            return ResponseEntity.ok("Transfert effectué avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/transfertMagasin/{magasinId}")
    public List<TransfertProduit> getTransfertsByMagasin(@PathVariable Long magasinId) {
        return transfertProduitRepository.findByMagasinId(magasinId);
    }

    @GetMapping("/boutique/{boutiqueId}")
    public List<TransfertProduit> getTransfertsByBoutique(@PathVariable Long boutiqueId) {
        return transfertProduitRepository.findByBoutiqueId(boutiqueId);
    }

    // Récupérer tous les historiques des transferts
    @GetMapping("/historique-transferts")
    public ResponseEntity<List<TransfertHistorique>> getHistoriqueTransferts() {
        List<TransfertHistorique> historiqueTransferts = transfertHistoriqueRepository.findAll();
        return ResponseEntity.ok(historiqueTransferts);
    }

}
