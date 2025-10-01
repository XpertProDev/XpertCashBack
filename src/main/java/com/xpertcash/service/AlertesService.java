package com.xpertcash.service;

import com.xpertcash.DTOs.AlerteStockDTO;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertesService {

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private AuthenticationHelper authHelper;

    /**
     * Récupère les alertes de stock faible pour l'entreprise
     * Retourne les produits dont le stock actuel est inférieur ou égal au seuil d'alerte
     */
    @Transactional(readOnly = true)
    public List<AlerteStockDTO> getAlertesStockFaible(HttpServletRequest request) {
        // 🔐 Récupération de l'utilisateur connecté
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à cette information.");
        }

        // Récupérer tous les produits actifs de l'entreprise
        List<Produit> produitsActifs = produitRepository.findAllByEntrepriseId(entrepriseId)
                .stream()
                .filter(p -> p.getDeleted() == null || !p.getDeleted())
                .toList();

        List<AlerteStockDTO> alertes = new ArrayList<>();

        // Vérifier le stock de chaque produit
        for (Produit produit : produitsActifs) {
            Stock stock = stockRepository.findByProduit(produit);
            
            // Déterminer le stock actuel et le seuil d'alerte
            Integer stockActuel = null;
            Integer seuilAlert = null;
            
            if (stock != null) {
                stockActuel = stock.getStockActuel();
                // Priorité au seuil d'alerte du Stock, sinon celui du Produit
                seuilAlert = stock.getSeuilAlert() != null ? stock.getSeuilAlert() : produit.getSeuilAlert();
            } else {
                // Si pas de Stock, utiliser la quantité du Produit
                stockActuel = produit.getQuantite();
                seuilAlert = produit.getSeuilAlert();
            }
            
            // Vérifier si une alerte doit être créée
            if (stockActuel != null && seuilAlert != null && stockActuel <= seuilAlert) {
                AlerteStockDTO alerte = new AlerteStockDTO();
                alerte.setProduitId(produit.getId());
                alerte.setNomProduit(produit.getNom());
                alerte.setCodeGenerique(produit.getCodeGenerique());
                alerte.setStockActuel(stockActuel);
                alerte.setSeuilAlert(seuilAlert);
                alerte.setNomBoutique(produit.getBoutique() != null ? produit.getBoutique().getNomBoutique() : null);
                alerte.setBoutiqueId(produit.getBoutique() != null ? produit.getBoutique().getId() : null);
                
                alertes.add(alerte);
            }
        }

        // Trier par stock actuel croissant (les plus critiques en premier)
        alertes.sort((a1, a2) -> Integer.compare(a1.getStockActuel(), a2.getStockActuel()));

        return alertes;
    }
}

