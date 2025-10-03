package com.xpertcash.service;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Stock;
import com.xpertcash.repository.StockRepository;


@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;



   

    // Ajouter du stock à une boutique (seul l'admin peut le faire)
    /*@Transactional
    public Stock ajouterStock(HttpServletRequest request, Long boutiqueId, Long produitId, int quantite) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'admin depuis le token
        Long adminId = null;
        try {
            adminId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'admin depuis le token", e);
        }

        // Récupérer l'admin par son ID
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // Vérifier que l'admin est bien un Admin
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut ajouter du stock !");
        }

        // Vérifier si la boutique et le produit existent
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("La boutique avec l'ID " + boutiqueId + " n'existe pas"));

        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Le produit avec l'ID " + produitId + " n'existe pas"));

        // Créer un nouvel enregistrement de stock
        Stock stock = new Stock();
        stock.setQuantite(quantite);
        stock.setBoutique(boutique);
        stock.setProduit(produit);
        stock.setCreatedAt(LocalDateTime.now());

        // Sauvegarder le stock en base de données
        return stockRepository.save(stock);
    }*/

    // Récupérer les stocks associés à une boutique
    public List<Stock> getStocksByBoutique(Long boutiqueId) {
        return stockRepository.findByBoutiqueId(boutiqueId);
    }

    

    
}
