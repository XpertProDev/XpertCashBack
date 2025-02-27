package com.xpertcash.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.service.BoutiqueService;
import com.xpertcash.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class BoutiqueController {

    @Autowired
    private UsersService usersService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthorizationService authorizationService;
     @Autowired
    private BoutiqueService boutiqueService;


      // Ajouter une boutique (requête JSON)
    @PostMapping("/ajouter")
    public ResponseEntity<Boutique> ajouterBoutique(
            HttpServletRequest request,
            @RequestBody Boutique boutiqueRequest) {

        // Utilisation du service pour ajouter la boutique
        Boutique boutique = boutiqueService.ajouterBoutique(request, boutiqueRequest.getNomBoutique(), boutiqueRequest.getAdresse());
        
        // Retourner la boutique créée avec un statut CREATED (201)
        return ResponseEntity.status(HttpStatus.CREATED).body(boutique);
    }

    // Récupérer toutes les boutiques d'une entreprise
    @GetMapping("/entreprise")
    public ResponseEntity<List<Boutique>> getBoutiquesByEntreprise(HttpServletRequest request) {
        // Utilisation du service pour récupérer les boutiques
        List<Boutique> boutiques = boutiqueService.getBoutiquesByEntreprise(request);

        // Retourner la liste des boutiques
        return ResponseEntity.ok(boutiques);
    }


}
