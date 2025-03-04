package com.xpertcash.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.xpertcash.service.IMAGES.ImageStorageService;

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
    @Autowired
    private ImageStorageService imageStorageService;


   /* @Autowired
    public ProduitController(ImageStorageService imageStorageService, ProduitService produitService) {
        this.imageStorageService = imageStorageService;
        this.produitService = produitService;
    }*/ 

    // Endpoint pour Créer un produit et décider si il doit être ajouté au stock
    @PostMapping(value = "/create/{boutiqueId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> createProduit(
            @PathVariable Long boutiqueId,
            @RequestPart("produit") String produitJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestParam boolean addToStock,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
        try {
            // Vérification de l'image reçue
            if (imageFile != null) {
                System.out.println("📷 Image reçue avec succès : " + imageFile.getOriginalFilename());
            } else {
                System.out.println("❌ Aucune image reçue !");
            }

            // Convertir le JSON en objet ProduitRequest
            ObjectMapper objectMapper = new ObjectMapper();
            ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);

            // Sauvegarde de l'image si elle est présente
            String photo = null;
            if (imageFile != null && !imageFile.isEmpty()) {
                photo = imageStorageService.saveImage(imageFile);
                System.out.println("URL enregistrée dans photo : " + photo);
            }

            produitRequest.setPhoto(photo);
            System.out.println("🔍 ProduitRequest après ajout de la photo : " + produitRequest);

            // Créer le produit en appelant le service
            ProduitDTO produitDTO = produitService.createProduit(request, boutiqueId, produitRequest, addToStock);

            // Retourner le produit créé avec son DTO
            return ResponseEntity.status(HttpStatus.CREATED).body(produitDTO);

        } catch (DuplicateProductException e) {
            // Gestion du cas où le produit existe déjà
            System.out.println("⚠️ Produit déjà existant : " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (Exception e) {
            // Erreur générique
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Une erreur est survenue : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    //Endpoint Update Produit
    @PatchMapping(value = "/updateProduit/{produitId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> updateProduit(
            @PathVariable Long produitId,
            @RequestPart("produit") String produitJson,  // Produit JSON en tant que part
            @RequestPart(value = "image", required = false) MultipartFile imageFile,  // Image en tant que part (optionnelle)
            @RequestParam boolean addToStock,  // Ajout au stock
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
        try {
            // Vérification de l'image reçue
            if (imageFile != null) {
                System.out.println("📷 Image reçue avec succès : " + imageFile.getOriginalFilename());
            } else {
                System.out.println("❌ Aucune image reçue !");
            }
    
            // Convertir le JSON en objet ProduitRequest
            ObjectMapper objectMapper = new ObjectMapper();
            ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);
    
            // Sauvegarde de l'image si elle est présente
            String photo = null;
            if (imageFile != null && !imageFile.isEmpty()) {
                photo = imageStorageService.saveImage(imageFile);  // Utilisation du service pour stocker l'image
                System.out.println("URL enregistrée dans photo : " + photo);
            }
    
            produitRequest.setPhoto(photo);  // Assigner l'URL de l'image au produit
    
            // Mise à jour du produit via le service
            ProduitDTO updatedProduitDTO = produitService.updateProduct(produitId, produitRequest, addToStock, request);
    
            // Retourner le produit mis à jour avec son DTO
            return ResponseEntity.status(HttpStatus.OK).body(updatedProduitDTO);
    
        } catch (Exception e) {
            // Gestion des erreurs
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Une erreur est survenue lors de la mise à jour du produit : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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

        //Lister les produit

        @GetMapping("/produits/{boutiqueId}/stock")
        public ResponseEntity<?> getProduitsParStock(@PathVariable Long boutiqueId) {
            try {
                // Récupérer les produits en stock
                List<ProduitDTO> produitsDTO = produitService.getProduitsParStock(boutiqueId);

                return ResponseEntity.ok(produitsDTO);
            } catch (Exception e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Une erreur est survenue lors de la récupération des produits.");

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }


        //Get Total Produit
        @GetMapping("/produits/{boutiqueId}/totaux-stock")
        public ResponseEntity<Map<String, Integer>> getTotalQuantitesParStock(@PathVariable Long boutiqueId) {
            try {
                // Récupérer les totaux des quantités en stock et non en stock
                Map<String, Integer> totals = produitService.getTotalQuantitesParStock(boutiqueId);
                return ResponseEntity.ok(totals);
            } catch (Exception e) {
                Map<String, Integer> errorResponse = new HashMap<>();
                errorResponse.put("totalEnStock", 0);
                errorResponse.put("totalNonEnStock", 0);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }

        //Get Produit by id
        @GetMapping("/produits/{produitId}")
        public ResponseEntity<ProduitDTO> getProduitById(@PathVariable("produitId") Long produitId) {
            ProduitDTO produitDTO = produitService.getProduitById(produitId);
            return ResponseEntity.ok(produitDTO);
        }
        

}
