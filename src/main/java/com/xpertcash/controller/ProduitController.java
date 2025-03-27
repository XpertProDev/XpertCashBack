package com.xpertcash.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpertcash.DTOs.AjouterStockRequest;
import com.xpertcash.DTOs.FactureDTO;
import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.ProduitRequest;
import com.xpertcash.DTOs.RetirerStockRequest;
import com.xpertcash.DTOs.StockHistoryDTO;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.StockHistory;
import com.xpertcash.entity.Unite;
import com.xpertcash.exceptions.DuplicateProductException;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockHistoryRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UniteRepository;
import com.xpertcash.service.ProduitService;
import com.xpertcash.service.IMAGES.ImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
public class ProduitController {

    @Autowired
    private ProduitService produitService;
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
    @Autowired
    private StockHistoryRepository stockHistoryRepository;


    // Endpoint pour Créer un produit et décider si il doit être ajouté au stock
    @PostMapping(value = "/produit/create", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }) 
    public ResponseEntity<?> createProduit(
        @RequestPart("boutiqueIds") String boutiqueIdsJson, // Liste des IDs des boutiques
        @RequestPart("produit") String produitJson,
        @RequestPart(value = "image", required = false) MultipartFile imageFile,
        @RequestParam boolean addToStock,
        @RequestHeader("Authorization") String token,
        HttpServletRequest request) {
    try {
        // Vérification de l'image reçue
        if (imageFile != null) {
            System.out.println("📸 Image reçue : " + imageFile.getOriginalFilename());
        } else {
            System.out.println("🚫 Aucune image reçue !");
        }

        ObjectMapper objectMapper = new ObjectMapper();

        // conversion boutiqueIdsJson en liste
        List<Long> boutiqueIds = objectMapper.readValue(boutiqueIdsJson, new TypeReference<List<Long>>() {});
        
        ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);
        
        // Sauvegarde de l'image si elle est présente
        String photo = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            photo = imageStorageService.saveImage(imageFile);
            System.out.println("✅ URL enregistrée : " + photo);
        }
        produitRequest.setPhoto(photo);

        // Creation de produit pour toutes les boutiques spécifiées
        List<ProduitDTO> produitsAjoutes = produitService.createProduit(request, boutiqueIds, produitRequest, addToStock);

        // Retourner la liste des produits ajoutés
        return ResponseEntity.status(HttpStatus.CREATED).body(produitsAjoutes);

    } catch (DuplicateProductException e) {
        System.out.println("⚠️ Produit déjà existant : " + e.getMessage());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

    } catch (Exception e) {
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
            System.out.println("Début de la mise à jour du produit ID: " + produitId);
    
            // Vérification de l'image reçue
            if (imageFile != null) {
                System.out.println("📷 Image reçue : " + imageFile.getOriginalFilename());
            } else {
                System.out.println("Aucune image reçue !");
            }
    
            // Désérialisation de l'objet JSON en ProduitRequest
            ObjectMapper objectMapper = new ObjectMapper();
            ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);
    
            // Vérification si le produit existe
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé !"));
    
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
            System.out.println("Produit mis à jour avec succès !");
    
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

                    
                }
                if (produitRequest.getSeuilAlert() != null) {
                    stock.setSeuilAlert(produitRequest.getSeuilAlert());
                    System.out.println("Seuil d'alerte mis à jour : " + stock.getSeuilAlert());
                }
    
                stock.setLastUpdated(LocalDateTime.now());
                stockRepository.save(stock);
                produit.setEnStock(true);
            } else {
                // Suppression du stock si `addToStock` est `false`
                 List<StockHistory> historyRecords = stockHistoryRepository.findByStock(stock);
                    if (!historyRecords.isEmpty()) {
                        stockHistoryRepository.deleteAll(historyRecords);
                    }
                if (stock != null) {
                    stockRepository.delete(stock);
                    System.out.println("🗑️ Stock supprimé !");
                }
                produit.setEnStock(false);
            }
    
            produitRepository.saveAndFlush(produit);
            System.out.println("Stock mis à jour avec succès !");
    
            return ResponseEntity.status(HttpStatus.OK).body(produit);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la mise à jour du produit : " + e.getMessage());
    
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
        @PatchMapping(value = "/ajouterStock", consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<?> ajouterStock(
                @RequestBody AjouterStockRequest request, 
                @RequestHeader("Authorization") String token, 
                HttpServletRequest httpRequest) {
            try {
                if (request.getProduitsQuantites() == null || request.getProduitsQuantites().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Erreur : La liste des produits et quantités est obligatoire.");
                }
        
                // Appel du service pour ajouter plusieurs produits en stock
                Facture facture = produitService.ajouterStock(
                    request.getBoutiqueId(),
                    request.getProduitsQuantites(),
                    request.getDescription(),
                    request.getCodeFournisseur(),
                    httpRequest);

        
                return ResponseEntity.status(HttpStatus.OK).body(new FactureDTO(facture));
        
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Erreur lors de l'ajout du stock : " + e.getMessage());
            }
        }
        
            // Endpoint Stock Historique
            @GetMapping("/stockhistorique/{produitId}")
            public ResponseEntity<?> getStockHistory(@PathVariable Long produitId) {
                try {
                    List<StockHistoryDTO> stockHistoryDTOs = produitService.getStockHistory(produitId);
                    return ResponseEntity.ok(stockHistoryDTOs);
                } catch (NoSuchElementException e) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Collections.singletonMap("message", "Produit non trouvé avec l'ID : " + produitId));
                } catch (RuntimeException e) {
                    return ResponseEntity.status(HttpStatus.OK)
                            .body(Collections.singletonMap("message", e.getMessage()));
                }
            }

            // Endpoint pour récupérer l'historique général des mouvements de stock
            @GetMapping("/stockhistorique")
            public ResponseEntity<?> getAllStockHistory() {
                try {
                    List<StockHistoryDTO> stockHistories = produitService.getAllStockHistory();
            
                    if (stockHistories.isEmpty()) {
                        Map<String, String> response = new HashMap<>();
                        response.put("message", "Aucun historique de stock disponible.");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    }
            
                    return ResponseEntity.ok(stockHistories);
                } catch (Exception e) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Erreur interne du serveur.");
                    errorResponse.put("details", e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                }
            }
            


            



        // Endpoint pour retirer la quantité du produit en stock (un ou plusieurs produits)
        @PatchMapping(value = "/retirerStock", consumes = { MediaType.APPLICATION_JSON_VALUE })
        public ResponseEntity<?> retirerStock(
                @RequestBody RetirerStockRequest retirerStockRequest,
                @RequestHeader("Authorization") String token,
                HttpServletRequest request) {
            try {
                if (retirerStockRequest.getProduitsQuantites() == null || retirerStockRequest.getProduitsQuantites().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Erreur : Le champ 'produitsQuantites' est obligatoire.");
                }
        
                String descriptionRetire = retirerStockRequest.getDescription() != null ? retirerStockRequest.getDescription() : null;
                Long boutiqueId = retirerStockRequest.getBoutiqueId();
        
                FactureDTO factureDTO = produitService.retirerStock(boutiqueId, retirerStockRequest.getProduitsQuantites(), descriptionRetire, request);
        
                // Retourner la factureDTO dans la réponse
                return ResponseEntity.status(HttpStatus.OK).body(factureDTO);
        
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Une erreur est survenue lors de la réduction du stock : " + e.getMessage());
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
      

        //Récupérer tous les produits de toutes les boutiques d'une entreprise
        @GetMapping("/produits/entreprise/{entrepriseId}")
        public ResponseEntity<?> getProduitsParEntreprise(@PathVariable Long entrepriseId) {
            try {
                List<ProduitDTO> produitsDTO = produitService.getProduitsParEntreprise(entrepriseId);
                return ResponseEntity.ok(produitsDTO);
            } catch (Exception e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Une erreur est survenue lors de la récupération des produits.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
       
}
