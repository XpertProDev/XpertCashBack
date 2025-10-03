package com.xpertcash.controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




@RestController
@RequestMapping("/api/auth")
public class StockController {
   
    

    // Ajouter un stock à une boutique (seul l'admin peut le faire)
    /*@PostMapping("/ajouterStock")
    public ResponseEntity<Stock> ajouterStock(
            HttpServletRequest request,
            @RequestParam Long boutiqueId,
            @RequestParam Long produitId,
            @RequestParam int quantite) {
        
        // Utilisation du service pour ajouter du stock
        Stock stock = stockService.ajouterStock(request, boutiqueId, produitId, quantite);

        // Retourner le stock créé avec un statut CREATED (201)
        return ResponseEntity.status(HttpStatus.CREATED).body(stock);
    }

    // Récupérer les stocks associés à une boutique
    @GetMapping("/boutique/{boutiqueId}")
    public ResponseEntity<List<Stock>> getStocksByBoutique(
            @PathVariable Long boutiqueId) {

        // Utilisation du service pour récupérer les stocks de la boutique
        List<Stock> stocks = stockService.getStocksByBoutique(boutiqueId);

        // Retourner la liste des stocks
        return ResponseEntity.ok(stocks);
    }*/

     



 

}
