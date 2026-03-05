package com.xpertcash.service;

import com.xpertcash.DTOs.AlerteStockDTO;
import com.xpertcash.DTOs.PaginatedResponseDTO;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.TypeProduit;
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

  
    @Transactional(readOnly = true)
    public List<AlerteStockDTO> getAlertesStockFaible(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à cette information.");
        }

        List<Produit> produitsActifs = produitRepository.findAllByEntrepriseId(entrepriseId)
                .stream()
                .filter(p -> p.getDeleted() == null || !p.getDeleted())
                .filter(p -> p.getTypeProduit() == null || p.getTypeProduit() == TypeProduit.PHYSIQUE)
                .toList();

        List<AlerteStockDTO> alertes = new ArrayList<>();

        for (Produit produit : produitsActifs) {
            Stock stock = stockRepository.findByProduit(produit);
            
            Integer stockActuel = null;
            Integer seuilAlert = null;
            
            if (stock != null) {
                stockActuel = stock.getStockActuel();
                seuilAlert = stock.getSeuilAlert() != null ? stock.getSeuilAlert() : produit.getSeuilAlert();
            } else {
                stockActuel = produit.getQuantite();
                seuilAlert = produit.getSeuilAlert();
            }
            
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

        alertes.sort((a1, a2) -> Integer.compare(a1.getStockActuel(), a2.getStockActuel()));

        return alertes;
    }

    /**
     * Version paginée en mémoire des alertes de stock faible.
     * Utile pour les grands écrans listes, tout en réutilisant la logique existante.
     */
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<AlerteStockDTO> getAlertesStockFaiblePaginated(
            HttpServletRequest request,
            int page,
            int size) {

        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        List<AlerteStockDTO> allAlertes = getAlertesStockFaible(request);
        int total = allAlertes.size();

        int fromIndex = page * size;
        if (fromIndex >= total) {
            return new PaginatedResponseDTO<>(
                    new ArrayList<>(),
                    page,
                    size,
                    total,
                    (int) Math.ceil(total / (double) size),
                    false,
                    page > 0,
                    page == 0,
                    true
            );
        }

        int toIndex = Math.min(fromIndex + size, total);
        List<AlerteStockDTO> content = allAlertes.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil(total / (double) size);

        return new PaginatedResponseDTO<>(
                content,
                page,
                size,
                total,
                totalPages,
                page < totalPages - 1,
                page > 0,
                page == 0,
                page >= totalPages - 1
        );
    }
}

