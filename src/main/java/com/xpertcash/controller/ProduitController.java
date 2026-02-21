package com.xpertcash.controller;


import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

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
import com.xpertcash.DTOs.CompteurBoutiqueDTO;
import com.xpertcash.DTOs.FavoriVenteRequest;
import com.xpertcash.DTOs.FactureDTO;
import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.ProduitEntreprisePaginatedResponseDTO;
import com.xpertcash.DTOs.PaginatedResponseDTO;
import com.xpertcash.DTOs.ProduitStockPaginatedResponseDTO;
import com.xpertcash.DTOs.RetirerStockRequest;
import com.xpertcash.DTOs.StockHistoryDTO;
import com.xpertcash.DTOs.StockListDTO;
import com.xpertcash.DTOs.PRODUIT.ProduitRequest;
import com.xpertcash.entity.Facture;
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
    HttpServletRequest request) {
    try {
        if (imageFile != null) {
            System.out.println(" Image reçue : " + imageFile.getOriginalFilename());
        } else {
            System.out.println(" Aucune image reçue !");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<Long> boutiqueIds = objectMapper.readValue(boutiqueIdsJson, new TypeReference<List<Long>>() {});
        List<Integer> quantites = objectMapper.readValue(quantitesJson, new TypeReference<List<Integer>>() {});
        List<Integer> seuilAlert = objectMapper.readValue(seuilAlertJson, new TypeReference<List<Integer>>() {});
        ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);

        if (boutiqueIds.size() != quantites.size()) {
            throw new RuntimeException("Le nombre de boutiques ne correspond pas au nombre de quantités.");
        }

        if (boutiqueIds.size() != seuilAlert.size()) {
            throw new RuntimeException("Le nombre de boutiques ne correspond pas au nombre de seuils.");
        }

        String photo = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            photo = imageStorageService.saveImage(imageFile);
            System.out.println(" URL enregistrée : " + photo);
        }
        produitRequest.setPhoto(photo);

        List<ProduitDTO> produitsAjoutes = produitService.createProduit(request, boutiqueIds, quantites, seuilAlert, produitRequest, addToStock, photo);

        return ResponseEntity.status(HttpStatus.CREATED).body(produitsAjoutes);

    } catch (DuplicateProductException e) {
        System.out.println(" Produit déjà existant : " + e.getMessage());
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
        HttpServletRequest request) {
    try {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ProduitRequest produitRequest = objectMapper.readValue(produitJson, ProduitRequest.class);

        ProduitDTO produitDTO = produitService.updateProduct(produitId, produitRequest, imageFile, addToStock, request);

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
        public ResponseEntity<Map<String, Integer>> getTotalQuantitesParStock(@PathVariable Long boutiqueId, HttpServletRequest request) {
            try {
                Map<String, Integer> totals = produitService.getTotalQuantitesParStock(boutiqueId, request);
                return ResponseEntity.ok(totals);
            } catch (RuntimeException e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new HashMap<>());
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
    

    /** Historique des mouvements de stock de l'entreprise, paginé en base (isolation multi-tenant). */
    @GetMapping("/stockhistorique")
    public ResponseEntity<?> getStockHistoryPaginated(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponseDTO<StockHistoryDTO> result = produitService.getStockHistoryPaginated(request, page, size);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

            

        // Endpoint pour retirer la quantité du produit en stock (un ou plusieurs produits)
        @PatchMapping(value = "/retirerStock", consumes = { MediaType.APPLICATION_JSON_VALUE })
        public ResponseEntity<?> retirerStock(
                @RequestBody RetirerStockRequest retirerStockRequest,
                HttpServletRequest request) {
            try {
                if (retirerStockRequest.getProduitsQuantites() == null || retirerStockRequest.getProduitsQuantites().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Erreur : Le champ 'produitsQuantites' est obligatoire.");
                }
        
                String descriptionRetire = retirerStockRequest.getDescription() != null ? retirerStockRequest.getDescription() : null;
                Long boutiqueId = retirerStockRequest.getBoutiqueId();
        
                FactureDTO factureDTO = produitService.retirerStock(boutiqueId, retirerStockRequest.getProduitsQuantites(), descriptionRetire, request);
        
                return ResponseEntity.status(HttpStatus.OK).body(factureDTO);
        
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Une erreur est survenue lors de la réduction du stock : " + e.getMessage());
            }
        }
        
    /** Liste des stocks de l'entreprise, paginée en base (isolation multi-tenant). */
    @GetMapping("/getAllStock")
    public ResponseEntity<?> getStocksPaginated(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponseDTO<StockListDTO> result = produitService.getStocksPaginated(request, page, size);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

        /** Produits de l'entreprise paginés. search : filtre par nom, code_generique, code_bare ou catégorie (côté serveur). Isolation multi-tenant. */
        @GetMapping("/produits/entreprise/paginated")
        public ResponseEntity<?> getProduitsEntreprisePaginated(
                HttpServletRequest request,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "20") int size,
                @RequestParam(defaultValue = "codeGenerique") String sortBy,
                @RequestParam(defaultValue = "asc") String sortDir,
                @RequestParam(required = false) String search) {
            return handleRequest(() -> produitService.getProduitsEntreprisePaginated(request, page, size, sortBy, sortDir, search));
        }



        //
        @PostMapping(value = "/import-produits-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> importProduitsFromExcel(
                @RequestParam("file") MultipartFile file,
                @RequestParam Long entrepriseId,
                @RequestParam(value = "boutiqueIds", required = false) String boutiqueIdsJson,
                HttpServletRequest request) {

            try {
                String token = request.getHeader("Authorization");
                if (token == null || !token.startsWith("Bearer ")) {
                    throw new RuntimeException("Token JWT manquant ou mal formaté");
                }

                String contentType = file.getContentType();
                if (!Arrays.asList(
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ).contains(contentType)) {
                    return ResponseEntity.badRequest().body(
                            Collections.singletonMap("error", "Format de fichier non supporté: " + contentType)
                    );
                }

                byte[] fileBytes = file.getBytes();
                if (!isExcelFile(fileBytes)) {
                    return ResponseEntity.badRequest().body(
                            Collections.singletonMap("error", "Le fichier n'est pas un document Excel valide")
                    );
                }

                if (file.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body(
                            Collections.singletonMap("error", "Le fichier est trop volumineux (max 5MB)")
                    );
                }

                List<Long> boutiqueIds = new ArrayList<>();
                if (boutiqueIdsJson != null && !boutiqueIdsJson.isEmpty()) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    boutiqueIds = objectMapper.readValue(boutiqueIdsJson, new TypeReference<List<Long>>() {});
                }

                Map<String, Object> result = produitService.importProduitsFromExcel(
                        file.getInputStream(),
                        entrepriseId,
                        boutiqueIds,
                        token,
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
        boolean isXlsx = bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04;

        boolean isXls = bytes[0] == 0xD0 && bytes[1] == 0xCF && bytes[2] == 0x11 && bytes[3] == 0xE0;

        return isXlsx || isXls;
    }

        @GetMapping("/generate-test-excel")
        public void generateTestExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=test-import.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Produits");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nom produit", "Description", "Catégorie", "Prix Vente", "Prix Achat", "Quantité", "Unité", "Code Barre", "Type Produit", "Date Preemption", "Seuil Alert"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

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

    // Endpoint  avec pagination pour récupérer les produits d'une boutique
    @GetMapping("/boutique/{boutiqueId}/produits/paginated")
    public ResponseEntity<ProduitStockPaginatedResponseDTO> getProduitsParStockPaginated(
            @PathVariable Long boutiqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        try {
            if (page < 0) page = 0;
            if (size <= 0) size = 10;
            if (size > 100) size = 100; 
            
            ProduitStockPaginatedResponseDTO response = produitService.getProduitsParStockPaginated(
                    boutiqueId, page, size, request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des produits paginés: " + e.getMessage());
        }
    }

    /**
     * POS / Vente : tous les produits en stock d'une boutique en un seul appel (pagination, tri, recherche).
     * Remplacer les appels multiples par catégorie par cet endpoint.
     */
    @GetMapping("/boutique/{boutiqueId}/produits/vente/paginated")
    public ResponseEntity<ProduitStockPaginatedResponseDTO> getProduitsBoutiquePourVentePaginated(
            @PathVariable Long boutiqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchField,
            HttpServletRequest request) {
        try {
            ProduitStockPaginatedResponseDTO response = produitService.getProduitsBoutiquePourVentePaginated(
                    boutiqueId, page, size, sort, search, searchField, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des produits pour la vente: " + e.getMessage());
        }
    }

    /** Marquer / retirer un produit comme favori pour la vente dans cette boutique (vendeur). Les favoris s'affichent en premier au POS. */
    @PatchMapping("/boutique/{boutiqueId}/produits/{produitId}/favori-vente")
    public ResponseEntity<?> setProduitFavoriPourVente(
            @PathVariable Long boutiqueId,
            @PathVariable Long produitId,
            @RequestBody(required = false) FavoriVenteRequest body,
            HttpServletRequest request) {
        Boolean favori = body != null ? body.getFavori() : true;
        Integer ordre = body != null ? body.getOrdre() : null;
        return handleRequest(() -> produitService.setProduitFavoriPourVente(
                boutiqueId, produitId, favori, ordre, request));
    }

     // Endpoint pour récupérer les compteurs de produits par boutique pour l'entreprise de l'utilisateur connecté
    @GetMapping("/produits/compteurs-boutiques")
    public ResponseEntity<?> getCompteursBoutiques(HttpServletRequest request) {
        try {
            List<CompteurBoutiqueDTO> compteurs = produitService.getCompteursBoutiques(request);
            return ResponseEntity.ok(compteurs);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private ResponseEntity<?> handleRequest(Supplier<Object> supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur : " + e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
} 
