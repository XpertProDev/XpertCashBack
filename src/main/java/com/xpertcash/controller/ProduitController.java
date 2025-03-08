package com.xpertcash.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.ProduitRequest;
import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.Unite;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.DuplicateProductException;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UniteRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.ProduitService;
import com.xpertcash.service.UsersService;
import com.xpertcash.service.IMAGES.ImageStorageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:4200")
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
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private CategorieRepository categorieRepository;
    @Autowired
    private UniteRepository uniteRepository;


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

    // Endpoint Update Produit
    @PatchMapping(value = "/updateProduit/{produitId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> updateProduit(
            @PathVariable Long produitId,
            @RequestPart("produit") String produitJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestParam boolean addToStock,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
        try {
            System.out.println("🔄 Début de la mise à jour du produit ID: " + produitId);
    
            // Vérification de l'image reçue
            if (imageFile != null) {
                System.out.println("📷 Image reçue : " + imageFile.getOriginalFilename());
            } else {
                System.out.println("❌ Aucune image reçue !");
            }
    
            // Désérialisation de l'objet JSON en ProduitRequest
            ObjectMapper objectMapper = new ObjectMapper();
            ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);
    
            // Vérification si le produit existe
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> new RuntimeException("❌ Produit non trouvé !"));
    
            // Gestion de l'image
            if (imageFile != null && !imageFile.isEmpty()) {
                String photo = imageStorageService.saveImage(imageFile);
                produitRequest.setPhoto(photo);
                System.out.println("📸 URL de l'image enregistrée : " + photo);
            }
    
            // Mise à jour des champs du produit
            if (produitRequest.getNom() != null) produit.setNom(produitRequest.getNom());
            if (produitRequest.getQuantite() != null) produit.setQuantite(produitRequest.getQuantite());
            if (produitRequest.getDescription() != null) produit.setDescription(produitRequest.getDescription());
            if (produitRequest.getSeuilAlert() != null) produit.setSeuilAlert(produitRequest.getSeuilAlert());
            if (produitRequest.getPhoto() != null) produit.setPhoto(produitRequest.getPhoto());
            if (produitRequest.getCodeBare() != null) produit.setCodeBare(produitRequest.getCodeBare());


            if (produitRequest.getCategorieId() != null) {
                Categorie categorie = categorieRepository.findById(produitRequest.getCategorieId())
                        .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
                produit.setCategorie(categorie);
            }

            if (produitRequest.getUniteId() != null) {
                Unite unite = uniteRepository.findById(produitRequest.getUniteId())
                        .orElseThrow(() -> new RuntimeException("Unité de mesure non trouvée"));
                produit.setUniteDeMesure(unite);
            }
    
            produitRepository.saveAndFlush(produit);
            System.out.println("✅ Produit mis à jour avec succès !");
    
            // Gestion du stock
            Stock stock = stockRepository.findByProduit(produit);
            if (addToStock) {
                if (stock == null) {
                    stock = new Stock();
                    stock.setProduit(produit);
                    stock.setBoutique(produit.getBoutique());
                    stock.setCreatedAt(LocalDateTime.now());
                }
    
                 // Si la quantité est modifiée, réinitialiser les valeurs du stock
                if (produitRequest.getQuantite() != null) {
                    stock.setStockActuel(produitRequest.getQuantite());
                    stock.setQuantiteAjoute(0);
                    stock.setQuantiteRetirer(0);
                    stock.setStockApres(stock.getStockActuel());
                    stock.setDescriptionAjout(null);
                    stock.setDescriptionRetire(null);
                    
                }
                if (produitRequest.getSeuilAlert() != null) {
                    stock.setSeuilAlert(produitRequest.getSeuilAlert());
                    System.out.println("🔔 Seuil d'alerte mis à jour : " + stock.getSeuilAlert());
                }
    
                stock.setLastUpdated(LocalDateTime.now());
                stockRepository.save(stock);
                produit.setEnStock(true);
            } else {
                // Suppression du stock si `addToStock` est `false`
                if (stock != null) {
                    stockRepository.delete(stock);
                    System.out.println("🗑️ Stock supprimé !");
                }
                produit.setEnStock(false);
            }
    
            produitRepository.saveAndFlush(produit);
            System.out.println("✅ Stock mis à jour avec succès !");
    
            return ResponseEntity.status(HttpStatus.OK).body(produit);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur lors de la mise à jour du produit : " + e.getMessage());
    
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
        

        //Endpoint pour ajuster la quantiter du produit en stock
        @PatchMapping(value = "/ajouterStock/{produitId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
        public ResponseEntity<?> ajouterStock(
                @PathVariable Long produitId,
                @RequestPart(value = "stock") String stockJson, 
                @RequestHeader("Authorization") String token) {
            try {
                if (stockJson == null || stockJson.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Erreur : Le champ 'stock' est obligatoire.");
                }
        
                ObjectMapper objectMapper = new ObjectMapper();
                Stock stockRequest = objectMapper.readValue(stockJson, Stock.class);
                String description = stockRequest.getDescriptionAjout() != null ? stockRequest.getDescriptionAjout() : null;
        
                Stock updatedStock = produitService.ajouterStock(produitId, stockRequest.getQuantiteAjoute(), description);
        
                return ResponseEntity.status(HttpStatus.OK).body(updatedStock);
        
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Une erreur est survenue lors de l'ajout du stock : " + e.getMessage());
            }
        }
        


        //Endpoint pour retirer la quantiter du produit en stock
        @PatchMapping(value = "/retirerStock/{produitId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
        public ResponseEntity<?> retirerStock(
                @PathVariable Long produitId,
                @RequestPart(value = "stock") String stockJson, 
                @RequestHeader("Authorization") String token) {
            try {
                if (stockJson == null || stockJson.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Erreur : Le champ 'stock' est obligatoire.");
                }
        
                ObjectMapper objectMapper = new ObjectMapper();
                Stock stockRequest = objectMapper.readValue(stockJson, Stock.class);

                String descriptionRetire = stockRequest.getDescriptionRetire() != null ? stockRequest.getDescriptionRetire() : null;
        
                Stock updatedStock = produitService.retirerStock(produitId, stockRequest.getQuantiteRetirer(), descriptionRetire);
        
                return ResponseEntity.status(HttpStatus.OK).body(updatedStock);
        
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Une erreur est survenue lors de reduction du stock : " + e.getMessage());
            }
        }
  
        //Endpoint List des Stock

        @GetMapping("/getAllStock")
        public ResponseEntity<List<Stock>> getAllStocks() {
            try {
                List<Stock> stocks = produitService.getAllStocks();
                if (stocks.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(stocks);
                }
                return ResponseEntity.status(HttpStatus.OK).body(stocks);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.emptyList());
            }
        }
}
