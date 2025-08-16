package com.xpertcash.service.VENTE;

import com.xpertcash.DTOs.VENTE.RemboursementRequest;
import com.xpertcash.DTOs.VENTE.RemboursementResponse;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.VENTE.TypeMouvementCaisse;
import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.VENTE.VenteHistorique;
import com.xpertcash.entity.VENTE.VenteProduit;
import com.xpertcash.entity.VENTE.VenteStatus;

import jakarta.servlet.http.HttpServletRequest;

import com.xpertcash.configuration.JwtUtil;

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

    double montantTotalSansRemise = 0.0;
List<VenteProduit> lignes = new ArrayList<>();

// Vérification : ne pas avoir remise globale ET remises par ligne simultanément
if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0
        && request.getRemises() != null && !request.getRemises().isEmpty()) {
    throw new RuntimeException("Vous ne pouvez pas appliquer une remise globale et des remises par ligne en même temps.");
}

// Création des lignes de vente (avec remise ligne uniquement si pas remise globale)
for (Map.Entry<Long, Integer> entry : request.getProduitsQuantites().entrySet()) {
    Long produitId = entry.getKey();
    Integer quantiteVendue = entry.getValue();

    Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

    // Vérifier et mettre à jour le stock
    Stock stock = stockRepository.findByProduit(produit);
    if (stock == null) {
        throw new RuntimeException("Stock non trouvé pour le produit " + produit.getNom());
    }
    if (stock.getStockActuel() < quantiteVendue) {
        throw new RuntimeException("Stock insuffisant pour le produit " + produit.getNom());
    }
    stock.setStockActuel(stock.getStockActuel() - quantiteVendue);
    stockRepository.save(stock);

    // Mettre à jour quantité dans Produit si utilisée
    if (produit.getQuantite() != null) {
        produit.setQuantite(produit.getQuantite() - quantiteVendue);
        produitRepository.save(produit);
    }

    // Calcul des montants
    double prixUnitaire = produit.getPrixVente();
    double remisePct = 0.0;
    if ((request.getRemises() != null && request.getRemises().containsKey(produitId))
            && (request.getRemiseGlobale() == null || request.getRemiseGlobale() == 0)) {
        remisePct = request.getRemises().get(produitId);
    }

    double prixApresRemise = prixUnitaire * (1 - remisePct / 100.0);
    double montantLigne = prixApresRemise * quantiteVendue;

    montantTotalSansRemise += montantLigne;

    VenteProduit ligne = new VenteProduit();
    ligne.setVente(vente);
    ligne.setProduit(produit);
    ligne.setQuantite(quantiteVendue);
    ligne.setPrixUnitaire(prixUnitaire);
    ligne.setRemise(remisePct);
    ligne.setMontantLigne(montantLigne);
    lignes.add(ligne);
}

    double montantTotal = montantTotalSansRemise;

    // Si remise globale, on la répartit sur les lignes
    if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0) {
        double remiseGlobalePct = request.getRemiseGlobale();
        vente.setRemiseGlobale(remiseGlobalePct);

        for (VenteProduit ligne : lignes) {
            double proportion = ligne.getMontantLigne() / montantTotalSansRemise;
            double montantRemiseLigne = montantTotalSansRemise * (remiseGlobalePct / 100.0) * proportion;
            double nouveauMontantLigne = ligne.getMontantLigne() - montantRemiseLigne;
            ligne.setMontantLigne(nouveauMontantLigne);

            // Mise à jour du prix unitaire avec remise globale répartie
            double prixUnitaireAvecRemiseGlobale = nouveauMontantLigne / ligne.getQuantite();
            ligne.setPrixUnitaire(prixUnitaireAvecRemiseGlobale);

            // On considère que la remise ligne est nulle si remise globale
            ligne.setRemise(0.0);
        }
        // Recalcul du total après remise globale répartie
        montantTotal = lignes.stream().mapToDouble(VenteProduit::getMontantLigne).sum();

    } else {
        // Pas de remise globale
        vente.setRemiseGlobale(0.0);
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
    historique.setMontant(vente.getMontantTotal());

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

    // On ignore le montant saisi manuellement, on prend le montant calculé
    vente.setMontantPaye(montantTotal);

    // Encaissement : ajouter le montant de la vente à la caisse
    caisseService.ajouterMouvement(
        caisse,
        TypeMouvementCaisse.VENTE,
        montantTotal,
        "Encaissement vente ID " + vente.getId(),
        vente,
        modePaiement,
        montantTotal // on enregistre ce qu’on a réellement vendu
    );

    // Construction de la réponse
    VenteResponse response = toVenteResponse(vente);
    return response;
}

    //Remboursement
    @Transactional
        public VenteResponse rembourserVente(RemboursementRequest request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        Long userId = jwtUtil.extractUserId(token.substring(7));
        User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Vente vente = venteRepository.findById(request.getVenteId())
            .orElseThrow(() -> new RuntimeException("Vente non trouvée"));

        Caisse caisse = caisseService.getCaisseActive(vente.getBoutique().getId(), httpRequest)
            .orElseThrow(() -> new RuntimeException("Aucune caisse ouverte pour cette boutique/vendeur"));

        if (request.getRescodePin() == null || request.getRescodePin().isBlank()) {
            throw new RuntimeException("Vous devez rentrer le code d'un responsable de l'entreprise");
        }
        User responsable = usersRepository.findByEntrepriseIdAndRole_NameIn(
                user.getEntreprise().getId(),
                Arrays.asList(RoleType.MANAGER, RoleType.ADMIN)
            ).orElseThrow(() -> new RuntimeException("Aucun responsable trouvé pour cette entreprise"));
        if (!request.getRescodePin().equals(responsable.getPersonalCode())) {
            throw new RuntimeException("Code PIN du responsable invalide");
        }

        Map<Long, Integer> produitsQuantites = request.getProduitsQuantites();
        if (produitsQuantites == null) {
            produitsQuantites = new HashMap<>();
            for (VenteProduit vp : vente.getProduits()) {
                produitsQuantites.put(vp.getProduit().getId(), vp.getQuantite());
            }
        }

        List<VenteProduit> lignesRemboursees = new ArrayList<>();
        double totalSansRemise = vente.getProduits().stream()
            .mapToDouble(vp -> vp.getPrixUnitaire() * vp.getQuantite())
            .sum();

        // Calculer montant exact en centimes pour éviter flottants
        List<Double> montantsLignes = new ArrayList<>();
        double montantRembourse = 0.0;
        int i = 0;
        int taille = produitsQuantites.size();

        for (Map.Entry<Long, Integer> entry : produitsQuantites.entrySet()) {
            Long produitId = entry.getKey();
            Integer quantiteARembourser = entry.getValue();

            VenteProduit vp = venteProduitRepository.findByVenteIdAndProduitId(vente.getId(), produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé dans la vente"));

            if (quantiteARembourser > vp.getQuantite()) {
                throw new RuntimeException("Quantité à rembourser supérieure à la quantité vendue pour le produit " + produitId);
            }

            double prixUnitaire = vp.getPrixUnitaire();
        double remiseLigne = vp.getRemise();
            double montantProduit = prixUnitaire * quantiteARembourser * (1 - remiseLigne / 100.0);

            if (vente.getRemiseGlobale() != null && vente.getRemiseGlobale() > 0) {
                double proportion = (prixUnitaire * quantiteARembourser) / totalSansRemise;
                montantProduit *= (1 - vente.getRemiseGlobale() / 100.0 * proportion);
            }

            // Conversion en centimes et arrondi
            montantProduit = Math.round(montantProduit * 100.0) / 100.0;
            montantsLignes.add(montantProduit);
            montantRembourse += montantProduit;

            // Mise à jour stock et produit
            Produit produit = vp.getProduit();
            Stock stock = stockRepository.findByProduit(produit);
            if (stock == null) throw new RuntimeException("Stock non trouvé pour le produit " + produit.getNom());
            stock.setStockActuel(stock.getStockActuel() + quantiteARembourser);
            stockRepository.save(stock);

            if (produit.getQuantite() != null) {
                produit.setQuantite(produit.getQuantite() + quantiteARembourser);
                produitRepository.save(produit);
            }

            vp.setQuantite(vp.getQuantite() - quantiteARembourser);
            venteProduitRepository.save(vp);

            lignesRemboursees.add(vp);

            // Ajuster dernier produit pour corriger arrondi
            if (i == taille - 1) {
                double sommeLignes = montantsLignes.stream().mapToDouble(Double::doubleValue).sum();
                double difference = Math.round(montantRembourse * 100.0) / 100.0 - sommeLignes;
                montantRembourse += difference;
            }
            i++;
        }

        caisseService.ajouterMouvement(
            caisse,
            TypeMouvementCaisse.REMBOURSEMENT,
            -montantRembourse,
            "Remboursement vente ID " + vente.getId() + " : " + request.getMotif(),
            vente,
            vente.getModePaiement(),
            -montantRembourse
        );

        VenteHistorique historique = new VenteHistorique();
        historique.setVente(vente);
        historique.setDateAction(LocalDateTime.now());
        historique.setAction("REMBOURSEMENT_VENTE");
        historique.setDetails("Remboursement effectué par " + user.getNomComplet() + ", raison: " + request.getMotif());
        historique.setMontant(montantRembourse);
        venteHistoriqueRepository.save(historique);

       // Calculer remboursement total pour déterminer le statut
    double totalRembourse = venteHistoriqueRepository
        .findByVenteId(vente.getId())
        .stream()
        .filter(h -> h.getAction().equals("REMBOURSEMENT_VENTE"))
        .mapToDouble(VenteHistorique::getMontant)
        .sum();

         double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;

         if (totalRembourse >= montantVente) {
        vente.setStatus(VenteStatus.REMBOURSEE);
    } else if (totalRembourse > 0) {
        vente.setStatus(VenteStatus.PARTIELLEMENT_REMBOURSEE);
    } else {
        vente.setStatus(VenteStatus.EN_COURS);
    }

        venteRepository.save(vente);


        return toVenteResponse(vente);
    }


   // Lister tous les remboursements pour l'utilisateur connecté
    public List<RemboursementResponse> getMesRemboursements(String jwtToken) {
        // 🔐 Vérification et extraction de l'utilisateur
        if (jwtToken == null || !jwtToken.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        Long userId = jwtUtil.extractUserId(jwtToken.substring(7));
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 🔐 Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour voir les remboursements !");
        }

        // 🔐 Récupérer toutes les ventes du vendeur
        List<Vente> ventes = venteRepository.findByVendeur(user);

        // 🔐 Filtrer uniquement les ventes de l'entreprise de l'utilisateur
        ventes = ventes.stream()
                .filter(v -> v.getBoutique().getEntreprise().getId().equals(user.getEntreprise().getId()))
                .collect(Collectors.toList());

        // Chercher tous les historiques de remboursement pour ces ventes
        List<VenteHistorique> remboursements = ventes.stream()
                .flatMap(v -> venteHistoriqueRepository.findByVenteAndAction(v, "REMBOURSEMENT_VENTE").stream())
                .collect(Collectors.toList());

        // Mapper en DTO
        List<RemboursementResponse> response = new ArrayList<>();
        for (VenteHistorique vh : remboursements) {
            RemboursementResponse dto = new RemboursementResponse();
            dto.setDateRemboursement(vh.getDateAction());
            dto.setDetails(vh.getDetails());
            dto.setMontant(vh.getMontant());
            response.add(dto);
        }

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
                dto.setRemise(ligne.getRemise());
                lignesDTO.add(dto);
                

            }
        }
        response.setRemiseGlobale(vente.getRemiseGlobale());
        response.setLignes(lignesDTO);
        response.setNomVendeur(vente.getVendeur() != null ? vente.getVendeur().getNomComplet() : null);
        response.setNomBoutique(vente.getBoutique() != null ? vente.getBoutique().getNomBoutique() : null);
        return response;
    }

}