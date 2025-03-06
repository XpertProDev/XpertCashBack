package com.xpertcash.controller;

import java.time.LocalDateTime;
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
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.DuplicateProductException;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockRepository;
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
    
            ObjectMapper objectMapper = new ObjectMapper();
            ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);
    
            String photo = null;
            if (imageFile != null && !imageFile.isEmpty()) {
                photo = imageStorageService.saveImage(imageFile);
                System.out.println("URL enregistrée dans photo : " + photo);
            }
    
            produitRequest.setPhoto(photo);
    
            // Mise à jour du produit
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
            produit.setNom(produitRequest.getNom());
            produit.setQuantite(produitRequest.getQuantite());
            produitRepository.save(produit);
    
            // Mise à jour du stock associé
            Stock stock = stockRepository.findByProduit(produit);
            if (stock == null) {
                stock = new Stock();
                stock.setProduit(produit);
                stock.setBoutique(produit.getBoutique());
                stock.setCreatedAt(LocalDateTime.now());
            }

    
            // Mise à jour du stock
            stock.setStockActuel(produit.getQuantite());
    
            stock.setQuantiteAjoute(0);
            stock.setQuantiteRetirer(0);
    
            // Calculer stockApres
            stock.setStockApres(stock.getStockActuel() + stock.getQuantiteAjoute());
    
            // Mettre à jour la date de mise à jour du stock
            stock.setLastUpdated(LocalDateTime.now());
            stockRepository.save(stock);
    
            return ResponseEntity.status(HttpStatus.OK).body(produit);
    
=======

            stock.setStockActuel(produit.getQuantite());

            // Recalculer stockApres (stock actuel + quantiteAjoute)
            stock.setStockApres(stock.getStockActuel() + stock.getQuantiteAjoute());

            // Mettre à jour la date de mise à jour du stock
            stock.setLastUpdated(LocalDateTime.now());

            // Sauvegarder le stock mis à jour
            stockRepository.save(stock);

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
        
                Stock updatedStock = produitService.ajouterStock(produitId, stockRequest.getQuantiteAjoute());
        
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
        
                Stock updatedStock = produitService.retirerStock(produitId, stockRequest.getQuantiteRetirer());
        
                return ResponseEntity.status(HttpStatus.OK).body(updatedStock);
        
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Une erreur est survenue lors de reduction du stock : " + e.getMessage());
            }
        }
        
}
