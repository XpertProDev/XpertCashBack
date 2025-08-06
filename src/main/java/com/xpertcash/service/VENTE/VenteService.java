package com.xpertcash.service.VENTE;

import com.xpertcash.DTOs.VENTE.VenteRequest;
import com.xpertcash.DTOs.VENTE.VenteResponse;
import com.xpertcash.composant.Utilitaire;
import com.xpertcash.entity.*;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.VenteHistoriqueRepository;
import com.xpertcash.repository.VENTE.VenteProduitRepository;
import com.xpertcash.repository.VENTE.VenteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.VENTE.TypeMouvementCaisse;
import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.VENTE.VenteHistorique;
import com.xpertcash.entity.VENTE.VenteProduit;

import jakarta.servlet.http.HttpServletRequest;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.service.UsersService;
import com.xpertcash.service.VENTE.CaisseService;

@Service
public class VenteService {
    @Autowired
    private VenteRepository venteRepository;
    @Autowired
    private VenteProduitRepository venteProduitRepository;
    @Autowired
    private BoutiqueRepository boutiqueRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private FactureVenteRepository factureVenteRepository;
    @Autowired
    private VenteHistoriqueRepository venteHistoriqueRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CaisseService caisseService;

    @Autowired
    private Utilitaire utilitaire;

    @Transactional
    public VenteResponse enregistrerVente(VenteRequest request, HttpServletRequest httpRequest) {
        // 🔐 Extraction et vérification du token JWT
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String jwtToken = token.substring(7);

        Long userId;
        try {
            userId = jwtUtil.extractUserId(jwtToken);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
        }

        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 🔐 Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour effectuer une vente !");
        }

        // 🔐 Vérification de la boutique
        Boutique boutique = boutiqueRepository.findById(request.getBoutiqueId())
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));
        if (!boutique.isActif()) {
            throw new RuntimeException("La boutique est désactivée, opération non autorisée.");
        }
        Long entrepriseId = boutique.getEntreprise().getId();
        if (!entrepriseId.equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Vérifier qu'une caisse OUVERTE existe pour ce vendeur/boutique
        Caisse caisse = caisseService.getCaisseActive(boutique.getId(), httpRequest)
            .orElseThrow(() -> new RuntimeException("Aucune caisse ouverte pour ce vendeur dans cette boutique. Veuillez ouvrir une caisse avant de vendre."));

        // Le vendeur est l'utilisateur connecté
        User vendeur = user;

        Vente vente = new Vente();
        vente.setBoutique(boutique);
        vente.setVendeur(vendeur);
        vente.setDateVente(java.time.LocalDateTime.now());
        vente.setDescription(request.getDescription());
        vente.setClientNom(request.getClientNom());
        vente.setClientNumero(request.getClientNumero());

        List<VenteProduit> lignes = new ArrayList<>();
        double montantTotal = 0.0;
        for (Map.Entry<Long, Integer> entry : request.getProduitsQuantites().entrySet()) {
            Produit produit = produitRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
            Integer quantiteVendue = entry.getValue();
            double prixUnitaire = produit.getPrixVente();
            double montantLigne = prixUnitaire * quantiteVendue;
            montantTotal += montantLigne;

            // Mise à jour du stock
            Stock stock = stockRepository.findByProduit(produit);
            if (stock == null || stock.getStockActuel() < quantiteVendue) {
                throw new RuntimeException("Stock insuffisant pour le produit: " + produit.getNom());
            }
            stock.setStockActuel(stock.getStockActuel() - quantiteVendue);
            stockRepository.save(stock);
            // Mise à jour de la quantité du produit
            if (produit.getQuantite() != null) {
                produit.setQuantite(produit.getQuantite() - quantiteVendue);
                produitRepository.save(produit);
            }

            VenteProduit ligne = new VenteProduit();
            ligne.setVente(vente);
            ligne.setProduit(produit);
            ligne.setQuantite(quantiteVendue);
            ligne.setPrixUnitaire(prixUnitaire);
            ligne.setMontantLigne(montantLigne);
            lignes.add(ligne);
        }
        vente.setMontantTotal(montantTotal);
        vente.setProduits(lignes);
        venteRepository.save(vente);
        venteProduitRepository.saveAll(lignes);

        // Historique de vente
        VenteHistorique historique = new VenteHistorique();
        historique.setVente(vente);
        historique.setDateAction(java.time.LocalDateTime.now());
        historique.setAction("ENREGISTREMENT_VENTE");
        historique.setDetails("Vente enregistrée par le vendeur " + vendeur.getNomComplet());
        venteHistoriqueRepository.save(historique);

        // Génération de la facture de vente
        FactureVente facture = new FactureVente();
        facture.setVente(vente);
        facture.setNumeroFacture("FV-" + vente.getId() + "-" + System.currentTimeMillis());
        facture.setDateEmission(java.time.LocalDateTime.now());
        facture.setMontantTotal(montantTotal);
        factureVenteRepository.save(facture);

        // Gestion du mode de paiement et du montant payé
        ModePaiement modePaiement = null;
        if (request.getModePaiement() != null) {
            try {
                modePaiement = ModePaiement.valueOf(request.getModePaiement());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Mode de paiement invalide : " + request.getModePaiement());
            }
        }
        vente.setModePaiement(modePaiement);
        vente.setMontantPaye(request.getMontantPaye());

        // Encaissement : ajouter le montant de la vente à la caisse
        caisseService.ajouterMouvement(
            caisse,
            TypeMouvementCaisse.VENTE,
            montantTotal,
            "Encaissement vente ID " + vente.getId(),
            vente,
            modePaiement,
            request.getMontantPaye()
        );

        // Construction de la réponse
        VenteResponse response = toVenteResponse(vente);
        return response;
    }

    public VenteResponse getVenteById(Long id) {
        Vente vente = venteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée"));
        return toVenteResponse(vente);
    }

    public List<VenteResponse> getAllVentes() {
        List<Vente> ventes = venteRepository.findAll();
        List<VenteResponse> responses = new ArrayList<>();
        for (Vente vente : ventes) {
            responses.add(toVenteResponse(vente));
        }
        return responses;
    }

 public List<VenteResponse> getVentesByBoutique(Long boutiqueId, HttpServletRequest request) {
    // 1️⃣ Récupération de l'utilisateur connecté
    User user = utilitaire.getAuthenticatedUser(request);

    utilitaire.validateAdminOrManagerAccess(boutiqueId, user);

    List<Vente> ventes = venteRepository.findByBoutiqueId(boutiqueId);

    return ventes.stream()
            .map(this::toVenteResponse)
            .collect(Collectors.toList());
}


public List<VenteResponse> getVentesByVendeur(Long vendeurId, HttpServletRequest request) {
        User user = utilitaire.getAuthenticatedUser(request);

        User vendeur = usersRepository.findById(vendeurId)
                .orElseThrow(() -> new RuntimeException("Vendeur introuvable"));

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;

        if (!isAdminOrManager) {
            if (!user.getId().equals(vendeurId)) {
                throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les ventes de ce vendeur !");
            }
        }

        // 4️⃣ Vérification que le vendeur appartient à la même entreprise
        if (!vendeur.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : ce vendeur n'appartient pas à votre entreprise.");
        }

        // 5️⃣ Récupération optimisée
        List<Vente> ventes = venteRepository.findByVendeurId(vendeurId);

        // 6️⃣ Transformation en DTO
        List<VenteResponse> responses = new ArrayList<>();
        for (Vente vente : ventes) {
            responses.add(toVenteResponse(vente));
        }

        return responses;
}


    private VenteResponse toVenteResponse(Vente vente) {
        VenteResponse response = new VenteResponse();
        response.setVenteId(vente.getId());
        response.setBoutiqueId(vente.getBoutique() != null ? vente.getBoutique().getId() : null);
        response.setVendeurId(vente.getVendeur() != null ? vente.getVendeur().getId() : null);
        response.setDateVente(vente.getDateVente());
        response.setMontantTotal(vente.getMontantTotal());
        response.setDescription(vente.getDescription());
        response.setClientNom(vente.getClientNom());
        response.setClientNumero(vente.getClientNumero());
        response.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
        response.setMontantPaye(vente.getMontantPaye());
        List<VenteResponse.LigneVenteDTO> lignesDTO = new ArrayList<>();
        if (vente.getProduits() != null) {
            for (VenteProduit ligne : vente.getProduits()) {
                VenteResponse.LigneVenteDTO dto = new VenteResponse.LigneVenteDTO();
                dto.setProduitId(ligne.getProduit().getId());
                dto.setNomProduit(ligne.getProduit().getNom());
                dto.setQuantite(ligne.getQuantite());
                dto.setPrixUnitaire(ligne.getPrixUnitaire());
                dto.setMontantLigne(ligne.getMontantLigne());
                lignesDTO.add(dto);
            }
        }
        response.setLignes(lignesDTO);
        response.setNomVendeur(vente.getVendeur() != null ? vente.getVendeur().getNomComplet() : null);
        response.setNomBoutique(vente.getBoutique() != null ? vente.getBoutique().getNomBoutique() : null);
        return response;
    }

}