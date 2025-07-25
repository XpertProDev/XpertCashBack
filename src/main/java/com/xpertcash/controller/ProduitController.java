package com.xpertcash.controller;


import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xpertcash.DTOs.AjouterStockRequest;
import com.xpertcash.DTOs.FactureDTO;
import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.ProduitRequest;
import com.xpertcash.DTOs.RetirerStockRequest;
import com.xpertcash.DTOs.StockHistoryDTO;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.DuplicateProductException;
import com.xpertcash.service.ProduitService;
import com.xpertcash.service.IMAGES.ImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.type.TypeReference;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class ProduitController {

    @Autowired
    private ProduitService produitService;
    @Autowired
    private ImageStorageService imageStorageService;
   


    // Endpoint pour Créer un produit et décider si il doit être ajouté au stock
    @PostMapping(value = "/create", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> createProduit(
    @RequestPart("boutiqueIds") String boutiqueIdsJson,
    @RequestPart("quantites") String quantitesJson,
    @RequestPart("produit") String produitJson,
    @RequestPart("seuilAlert") String seuilAlertJson,
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
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Conversion des JSON reçus en listes ou objets
        List<Long> boutiqueIds = objectMapper.readValue(boutiqueIdsJson, new TypeReference<List<Long>>() {});
        List<Integer> quantites = objectMapper.readValue(quantitesJson, new TypeReference<List<Integer>>() {});
        List<Integer> seuilAlert = objectMapper.readValue(seuilAlertJson, new TypeReference<List<Integer>>() {});
        ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);

        // Validation que les quantités et les boutiques ont le même nombre d'éléments
        if (boutiqueIds.size() != quantites.size()) {
            throw new RuntimeException("Le nombre de boutiques ne correspond pas au nombre de quantités.");
        }

        if (boutiqueIds.size() != seuilAlert.size()) {
            throw new RuntimeException("Le nombre de boutiques ne correspond pas au nombre de seuils.");
        }

        // Sauvegarde de l'image si elle est présente
        String photo = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            photo = imageStorageService.saveImage(imageFile);
            System.out.println("✅ URL enregistrée : " + photo);
        }
        produitRequest.setPhoto(photo); // Ajouter l'URL de l'image au produit

        // Creation de produit pour toutes les boutiques spécifiées
        List<ProduitDTO> produitsAjoutes = produitService.createProduit(request, boutiqueIds, quantites, seuilAlert, produitRequest, addToStock, photo);

        // Retourner la liste des produits ajoutés
        return ResponseEntity.status(HttpStatus.CREATED).body(produitsAjoutes);

    } catch (DuplicateProductException e) {
        // Gestion des erreurs de duplication de produit
        System.out.println("⚠️ Produit déjà existant : " + e.getMessage());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

    } catch (Exception e) {
        // Gestion des autres erreurs 
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
            HttpServletRequest request) {
        try {
            // Désérialiser l'objet produit JSON reçu
            ObjectMapper objectMapper = new ObjectMapper();
            ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);
    
            // Appel à la méthode du service pour mettre à jour le produit
            ProduitDTO produitDTO = produitService.updateProduct(produitId, produitRequest, imageFile, addToStock, request);
    
            // Retourner la réponse avec le produit mis à jour
            return ResponseEntity.ok(produitDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur : " + e.getMessage()));
        }
    }
    
        //Endpoint pour Supprime le produit s’il n'est pas en stock
        @DeleteMapping("/corbeille/{produitId}")
        public ResponseEntity<Map<String, Object>> deleteProduit(
                @PathVariable Long produitId,
                HttpServletRequest request) { 
            
            Map<String, Object> response = new HashMap<>();
            
            try {
                produitService.corbeille(produitId, request);
                
                response.put("status", "success");
                response.put("message", "Produit déplacé dans la corbeille");
                return ResponseEntity.ok(response);
                
            } catch (RuntimeException e) {
                response.put("status", "error");
                response.put("message", e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        }

        //Endpoint pour lister produits en corbeille
        @GetMapping("/corbeille/{boutiqueId}")
        public ResponseEntity<?> getProduitsDansCorbeille(
                @PathVariable Long boutiqueId,
                HttpServletRequest request) {

            try {
                List<ProduitDTO> produits = produitService.getProduitsDansCorbeille(boutiqueId, request);

                if (produits.isEmpty()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "info");
                    response.put("message", "La corbeille est vide pour cette boutique.");
                    return ResponseEntity.ok(response);
                }

                return ResponseEntity.ok(produits);

            } catch (RuntimeException e) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        }

        //Endpoint restaure corbeille
        @PostMapping("/corbeille/restaurer/{boutiqueId}")
        public ResponseEntity<Map<String, Object>> restaurerProduitsDansBoutique(
                @PathVariable Long boutiqueId,
                @RequestBody List<Long> produitIds,
                HttpServletRequest request) {

            Map<String, Object> response = new HashMap<>();

            try {
                produitService.restaurerProduitsDansBoutique(boutiqueId, produitIds, request);

                response.put("status", "success");
                response.put("message", produitIds.size() + " produit(s) restauré(s) avec succès");
                return ResponseEntity.ok(response);

            } catch (RuntimeException e) {
                response.put("status", "error");
                response.put("message", e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        }

        //Endpoint pour vide la corbeille
        @DeleteMapping("/corbeille/vider/{boutiqueId}")
        public ResponseEntity<Map<String, Object>> viderCorbeille(
                @PathVariable Long boutiqueId,
                HttpServletRequest request) {

            Map<String, Object> response = new HashMap<>();

            try {
                produitService.viderCorbeille(boutiqueId, request);

                response.put("status", "success");
                response.put("message", "Corbeille vidée avec succès");
                return ResponseEntity.ok(response);

            } catch (RuntimeException e) {
                response.put("status", "error");
                response.put("message", e.getMessage());
                return ResponseEntity.badRequest().body(response);
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

     // Lister les produits par boutique
        // @GetMapping("/produits/{boutiqueId}/stock")
        // public ResponseEntity<?> getProduitsParStock(@PathVariable Long boutiqueId, HttpServletRequest request) {
        //     try {
        //         // Supposons que l'ID utilisateur est injecté via le middleware de sécurité (ex : JWT)
        //         Long userId = (Long) request.getAttribute("userId");

        //         if (userId == null) {
        //             Map<String, String> errorResponse = new HashMap<>();
        //             errorResponse.put("error", "Utilisateur non authentifié.");
        //             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        //         }

        //         List<ProduitDTO> produitsDTO = produitService.getProduitsParStock(boutiqueId, userId);
        //         return ResponseEntity.ok(produitsDTO);

        //     } catch (RuntimeException e) {
        //         Map<String, String> errorResponse = new HashMap<>();
        //         errorResponse.put("error", e.getMessage());

        //         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        //     } catch (Exception e) {
        //         Map<String, String> errorResponse = new HashMap<>();
        //         errorResponse.put("error", "Une erreur interne est survenue lors de la récupération des produits.");

        //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        //     }
        // }


        @GetMapping("/produits/{boutiqueId}/stock")
         public ResponseEntity<List<ProduitDTO>> getProduitsParStock(
        @PathVariable Long boutiqueId,
        HttpServletRequest request
        ) {
            List<ProduitDTO> produits = produitService.getProduitsParStock(boutiqueId, request);
            return ResponseEntity.ok(produits);
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
        public ResponseEntity<ProduitDTO> getProduitById(@PathVariable("produitId") Long produitId, HttpServletRequest request) {
            ProduitDTO produitDTO = produitService.getProduitById(produitId, request);
            return ResponseEntity.ok(produitDTO);
        }

        

        //Endpoint pour ajuster la quantiter du produit en stock
        @PatchMapping(value = "/ajouterStock", consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<?> ajouterStock(
                @RequestBody AjouterStockRequest request,
                @RequestHeader("Authorization") String token,
                HttpServletRequest httpRequest
        ) {
            System.out.println("➡️ Boutique ID: " + request.getBoutiqueId());
            System.out.println("➡️ Produits et quantités: " + request.getProduitsQuantites());
            System.out.println("➡️ Fournisseur ID: " + request.getFournisseurId());

            if (request.getBoutiqueId() == null) {
                return ResponseEntity.badRequest().body("Le champ 'boutiqueId' est obligatoire.");
            }

            try {
                Facture facture = produitService.ajouterStock(
                        request.getBoutiqueId(),
                        request.getProduitsQuantites(),
                        request.getDescription(),
                        request.getCodeFournisseur(),
                        request.getFournisseurId(),
                        httpRequest
                );

                return ResponseEntity.ok(new FactureDTO(facture));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Erreur lors de l'ajout du stock : " + e.getMessage());
            }
        }

            
            // Endpoint Stock Historique
            @GetMapping("/stockhistorique/{produitId}")
                    public ResponseEntity<List<StockHistoryDTO>> getStockHistory(@PathVariable Long produitId, HttpServletRequest request) {
                List<StockHistoryDTO> history = produitService.getStockHistory(produitId, request);
                return ResponseEntity.ok(history);
            }
            

            // Endpoint pour récupérer l'historique général des mouvements de stock
           @GetMapping("/stockhistorique")
            public ResponseEntity<?> getAllStockHistory(HttpServletRequest request) {
                try {
                    List<StockHistoryDTO> historique = produitService.getAllStockHistory(request);
                    return ResponseEntity.ok(historique);
                } catch (RuntimeException e) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
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
        public ResponseEntity<?> getAllStocks(HttpServletRequest request) {
            try {
                List<Stock> stocks = produitService.getAllStocks(request);
                return ResponseEntity.ok(stocks);
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
        }

      

        //Endpoint GetAll produits de toutes les boutiques d'une entreprise
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

        //
        @PostMapping(value = "/import-produits-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> importProduitsFromExcel(
                @RequestParam("file") MultipartFile file,
                @RequestParam Long entrepriseId,
                @RequestParam(value = "boutiqueIds", required = false) String boutiqueIdsJson,
                @RequestHeader("Authorization") String token, // Token complet avec "Bearer"
                HttpServletRequest request) {

            try {
                // Validation du token JWT
                if (token == null || !token.startsWith("Bearer ")) {
                    throw new RuntimeException("Token JWT manquant ou mal formaté");
                }

                // Vérification du type de fichier
                String contentType = file.getContentType();
                if (!Arrays.asList(
                        "application/vnd.ms-excel",          // .xls
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
                ).contains(contentType)) {
                    return ResponseEntity.badRequest().body(
                            Collections.singletonMap("error", "Format de fichier non supporté: " + contentType)
                    );
                }

                // Vérification de la signature magique du fichier
                byte[] fileBytes = file.getBytes();
                if (!isExcelFile(fileBytes)) {
                    return ResponseEntity.badRequest().body(
                            Collections.singletonMap("error", "Le fichier n'est pas un document Excel valide")
                    );
                }

                // Vérification de la taille du fichier
                if (file.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body(
                            Collections.singletonMap("error", "Le fichier est trop volumineux (max 5MB)")
                    );
                }

                // Désérialiser les IDs des boutiques
                List<Long> boutiqueIds = new ArrayList<>();
                if (boutiqueIdsJson != null && !boutiqueIdsJson.isEmpty()) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    boutiqueIds = objectMapper.readValue(boutiqueIdsJson, new TypeReference<List<Long>>() {});
                }

                // Passer le token COMPLET au service
                Map<String, Object> result = produitService.importProduitsFromExcel(
                        file.getInputStream(),
                        entrepriseId,
                        boutiqueIds,
                        token, // Token complet avec "Bearer"
                        request
                );

                System.out.println("Import terminé. Succès: " + result.get("successCount"));
                if (result.containsKey("errors")) {
                    List<String> errors = (List<String>) result.get("errors");
                    System.out.println("Erreurs (" + errors.size() + "):");
                    errors.forEach(System.out::println);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Importation réussie");
                response.put("count", result.get("successCount"));

                if (result.containsKey("errors")) {
                    response.put("errors", result.get("errors"));
                }

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "Erreur lors du traitement du fichier: " + e.getMessage()));
            }
        }

    private boolean isExcelFile(byte[] bytes) {
        // Vérifier la signature pour .xlsx (PK header)
        boolean isXlsx = bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04;

        // Vérifier la signature pour .xls (OLE header)
        boolean isXls = bytes[0] == 0xD0 && bytes[1] == 0xCF && bytes[2] == 0x11 && bytes[3] == 0xE0;

        return isXlsx || isXls;
    }

        @GetMapping("/generate-test-excel")
        public void generateTestExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=test-import.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Produits");

            // En-têtes
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nom produit", "Description", "Catégorie", "Prix Vente", "Prix Achat", "Quantité", "Unité", "Code Barre", "Type Produit", "Date Preemption", "Seuil Alert"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Données de test
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("Produit Test");
            dataRow.createCell(1).setCellValue("Description test");
            dataRow.createCell(2).setCellValue("Catégorie Test");
            dataRow.createCell(3).setCellValue(1000);
            dataRow.createCell(4).setCellValue(500);
            dataRow.createCell(5).setCellValue(50);
            dataRow.createCell(6).setCellValue("Unité Test");
            dataRow.createCell(7).setCellValue("123456789");
            dataRow.createCell(8).setCellValue("PHYSIQUE");
            dataRow.createCell(9).setCellValue(LocalDate.now().plusMonths(6).toString()); // Date préemption
            dataRow.createCell(10).setCellValue(10);

            workbook.write(response.getOutputStream());
        }
    }
       
} 
