package com.xpertcash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.ProduitRequest;
import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
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



   // Créer un produit et décider si il doit être ajouté au stock
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
       } catch (RuntimeException e) {
        // Gestion des erreurs détaillées dans la réponse
        String errorMessage = "Une erreur est survenue lors de la création du produit : " + e.getMessage();
        System.err.println(errorMessage);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(null);  // Cette réponse d'erreur générique peut être détaillée davantage selon les types d'erreurs
    }
   }

}
