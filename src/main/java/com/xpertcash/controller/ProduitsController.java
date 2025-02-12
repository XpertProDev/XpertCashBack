package com.xpertcash.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xpertcash.entity.Produits;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.service.ProduitsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class ProduitsController {

    @Autowired
    private ProduitsService produitsService;

    // Méthode pour Ajouter un new produit
    @PostMapping("/add/produit")
    public ResponseEntity<?> ajouterProduit(
            @RequestParam("produit") String produitString,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            Produits produit = objectMapper.readValue(produitString, Produits.class);
            Produits savedProduit = produitsService.ajouterProduit(produit, imageFile);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduit);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

    // Méthode pour la modification de produit
    @PutMapping("/update/produit/{id}")
    public ResponseEntity<?> modifierProduit(
            @PathVariable Long id,
            @RequestParam("produit") String produitString,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            Produits produitModifie = objectMapper.readValue(produitString, Produits.class);
            Produits updatedProduit = produitsService.modifierProduit(id, produitModifie, imageFile);

            return ResponseEntity.ok(updatedProduit);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

    // Endpoint pour récupérer tous les produits
    @GetMapping("/list/produits")
    public ResponseEntity<?> getAllProduits() {
        try {
            List<Produits> produits = produitsService.getAllProduits();
            return ResponseEntity.ok(produits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

    // Endpoint pour récupérer un produit par son ID
    @GetMapping("/list/produit/{id}")
    public ResponseEntity<?> getProduitById(@PathVariable Long id) {
        try {
            Produits produit = produitsService.getProduitById(id);
            return ResponseEntity.ok(produit);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

    // Endpoint pour supprimer un produit
    @DeleteMapping("/delete/produit/{id}")
    public ResponseEntity<?> deleteProduit(@PathVariable Long id) {
        try {
            String message = produitsService.deleteProduit(id);
            return new ResponseEntity<>(message, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la suppression du produit : " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }


}
