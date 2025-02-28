package com.xpertcash.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.ProduitRequest;
import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.DuplicateProductException;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.ProduitService;
import com.xpertcash.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
public class ProduitController {

    @Autowired
    private ProduitService produitService;
    @Autowired
    private UsersService usersService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private UsersRepository usersRepository;



   // Endpoint pour Créer un produit et décider si il doit être ajouté au stock
    @PostMapping("/create/{boutiqueId}")
    public ResponseEntity<Produit> createProduit(
            @PathVariable Long boutiqueId,
            @RequestBody ProduitRequest produitRequest,
            @RequestParam boolean addToStock,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
        try {
            // Appeler le service pour créer le produit
            Produit produit = produitService.createProduit(request, boutiqueId, produitRequest, addToStock);

            // Retourner la réponse avec le produit créé
            return ResponseEntity.status(HttpStatus.CREATED).body(produit);
        } catch (DuplicateProductException e) {
            // Gestion des erreurs détaillées dans la réponse
            String errorMessage = "erreur  : " + e.getMessage();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            System.err.println(errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(null);  
        }
    }


   //Endpoint Update Produit
    @PatchMapping("/updateProduit/{produitId}")
    public ResponseEntity<ProduitDTO> updateProduit(
            @PathVariable Long produitId,
            @RequestBody ProduitRequest produitRequest,  // On récupère toutes les infos dans le body
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            // Vérifie si addToStock est null, pour éviter une erreur
            boolean addToStock = produitRequest.getEnStock() != null && produitRequest.getEnStock();

            // Appel au service pour modifier le produit
            ProduitDTO updatedProduit = produitService.updateProduct(produitId, produitRequest, addToStock, request);

            return ResponseEntity.status(HttpStatus.OK).body(updatedProduit);
        } catch (RuntimeException e) {
            String errorMessage = "Une erreur est survenue lors de la mise à jour du produit : " + e.getMessage();
            System.err.println(errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    //Endpoint pour Supprime le produit s’il n'est pas en stock
         @DeleteMapping("/deleteProduit/{produitId}")
    public ResponseEntity<String> deleteProduit(@PathVariable Long produitId) {
        try {
            produitService.deleteProduit(produitId);
            return ResponseEntity.ok("Produit supprimé avec succès !");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    //Endpoint pour Supprimer uniquement le stock
    @DeleteMapping("/deleteStock/{produitId}")
    public ResponseEntity<String> deleteStock(@PathVariable Long produitId) {
        try {
            produitService.deleteStock(produitId);
            return ResponseEntity.ok("Stock supprimé avec succès !");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


}
