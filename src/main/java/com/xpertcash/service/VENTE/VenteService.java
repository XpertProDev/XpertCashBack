package com.xpertcash.service.VENTE;

import com.xpertcash.DTOs.CLIENT.VenteParClientResponse;
import com.xpertcash.DTOs.VENTE.RemboursementRequest;
import com.xpertcash.DTOs.VENTE.RemboursementResponse;
import com.xpertcash.DTOs.VENTE.VenteLigneResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.VENTE.StatutCaisse;
import com.xpertcash.entity.VENTE.TypeMouvementCaisse;
import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.VENTE.VenteHistorique;
import com.xpertcash.entity.VENTE.VenteProduit;
import com.xpertcash.entity.VENTE.VenteStatus;

import jakarta.servlet.http.HttpServletRequest;

import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.service.AuthenticationHelper;

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

    @Autowired
    private FactureVenteService factureVenteService;

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;
    @Autowired
    private AuthenticationHelper authHelper;


 @Transactional
public VenteResponse enregistrerVente(VenteRequest request, HttpServletRequest httpRequest) {
    // 🔐 Extraction et vérification du token JWT
    String token = httpRequest.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }
    User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);

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

    // ✅ Ici, ajouter la gestion du client
    if (request.getClientId() != null) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        vente.setClient(client);
        vente.setEntrepriseClient(null);
    } else if (request.getEntrepriseClientId() != null) {
        EntrepriseClient entrepriseClient = entrepriseClientRepository.findById(request.getEntrepriseClientId())
                .orElseThrow(() -> new RuntimeException("Entreprise client introuvable"));
        vente.setEntrepriseClient(entrepriseClient);
        vente.setClient(null);
    } else {
        // Client passant
        vente.setClient(null);
        vente.setEntrepriseClient(null);
    }

    double montantTotalSansRemise = 0.0;
    List<VenteProduit> lignes = new ArrayList<>();

    // ✅ Vérification remise globale VS remises par ligne
    if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0
            && request.getRemises() != null && !request.getRemises().isEmpty()) {
        throw new RuntimeException("Vous ne pouvez pas appliquer une remise globale et des remises par ligne en même temps.");
    }

    // ✅ Charger d'un coup tous les produits nécessaires
    List<Long> produitIds = new ArrayList<>(request.getProduitsQuantites().keySet());
    Map<Long, Produit> produits = produitRepository.findAllById(produitIds).stream()
            .collect(Collectors.toMap(Produit::getId, p -> p));

    // ✅ Charger les stocks avec un verrou pessimiste
    List<Stock> stocks = stockRepository.findAllByProduitIdInWithLock(produitIds);
    Map<Long, Stock> stockMap = stocks.stream()
            .collect(Collectors.toMap(s -> s.getProduit().getId(), s -> s));

    // ✅ Boucle de création des lignes de vente
    for (Map.Entry<Long, Integer> entry : request.getProduitsQuantites().entrySet()) {
        Long produitId = entry.getKey();
        Integer quantiteVendue = entry.getValue();

        Produit produit = produits.get(produitId);
        if (produit == null) {
            throw new RuntimeException("Produit non trouvé");
        }

        Stock stock = stockMap.get(produitId);
        if (stock == null) {
            throw new RuntimeException("Stock non trouvé pour le produit " + produit.getNom());
        }

        // 🔒 Vérifier et mettre à jour le stock (protégé par PESSIMISTIC_WRITE)
        if (stock.getStockActuel() < quantiteVendue) {
            throw new RuntimeException("Stock insuffisant pour le produit " + produit.getNom());
        }
        stock.setStockActuel(stock.getStockActuel() - quantiteVendue);

        // Mettre à jour quantité dans Produit si utilisée
        if (produit.getQuantite() != null) {
            produit.setQuantite(produit.getQuantite() - quantiteVendue);
        }

        // 💰 Calcul des montants
        double prixUnitaire = produit.getPrixVente();
        double remisePct = 0.0;
        if (request.getRemises() != null && request.getRemises().containsKey(produitId)
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

    // ✅ Application de la remise globale si présente
    if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0) {
        double remiseGlobalePct = request.getRemiseGlobale();
        vente.setRemiseGlobale(remiseGlobalePct);

        for (VenteProduit ligne : lignes) {
            double proportion = ligne.getMontantLigne() / montantTotalSansRemise;
            double montantRemiseLigne = montantTotalSansRemise * (remiseGlobalePct / 100.0) * proportion;
            double nouveauMontantLigne = ligne.getMontantLigne() - montantRemiseLigne;
            ligne.setMontantLigne(nouveauMontantLigne);
            ligne.setPrixUnitaire(nouveauMontantLigne / ligne.getQuantite());
            ligne.setRemise(0.0); // ✅ on neutralise la remise ligne
        }

        montantTotal = lignes.stream().mapToDouble(VenteProduit::getMontantLigne).sum();
    } else {
        vente.setRemiseGlobale(0.0);
    }

    vente.setMontantTotal(montantTotal);
    vente.setProduits(lignes);
    vente.setStatus(VenteStatus.PAYEE);
    vente.setMontantPaye(montantTotal);
    vente.setCaisse(caisse);

    // ✅ Persist groupé
    venteRepository.save(vente);
    venteProduitRepository.saveAll(lignes);
    stockRepository.saveAll(stocks);
    produitRepository.saveAll(produits.values());

    // Historique de vente
    VenteHistorique historique = new VenteHistorique();
    historique.setVente(vente);
    historique.setDateAction(java.time.LocalDateTime.now());
    historique.setAction("ENREGISTREMENT_VENTE");
    historique.setDetails("Vente enregistrée par le vendeur " + vendeur.getNomComplet());
    historique.setMontant(vente.getMontantTotal());
    venteHistoriqueRepository.save(historique);

    // ✅ Facture (UUID unique)
    String numeroFacture = factureVenteService.genererNumeroFactureCompact(vente); 
    FactureVente facture = new FactureVente();
    facture.setVente(vente);
    // facture.setNumeroFacture("FV-" + UUID.randomUUID());
   facture.setNumeroFacture(numeroFacture);   
    facture.setDateEmission(java.time.LocalDateTime.now());
    facture.setMontantTotal(montantTotal);
    factureVenteRepository.save(facture);

    // Gestion du mode de paiement
    ModePaiement modePaiement = null;
    if (request.getModePaiement() != null) {
        try {
            modePaiement = ModePaiement.valueOf(request.getModePaiement());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Mode de paiement invalide : " + request.getModePaiement());
        }
    }
    vente.setModePaiement(modePaiement);

    // ✅ Encaissement : ajouter le montant de la vente à la caisse
    caisseService.ajouterMouvement(
        caisse,
        TypeMouvementCaisse.VENTE,
        montantTotal,
        "Encaissement vente ID " + vente.getId(),
        vente,
        modePaiement,
        montantTotal
    );

    // ✅ Réponse finale
    return toVenteResponse(vente);
}

    //Remboursement
    @Transactional
    public VenteResponse rembourserVente(RemboursementRequest request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);

        Vente vente = venteRepository.findById(request.getVenteId())
            .orElseThrow(() -> new RuntimeException("Vente non trouvée"));

        Caisse caisse = caisseService.getCaisseActive(vente.getBoutique().getId(), httpRequest)
            .orElseThrow(() -> new RuntimeException("Aucune caisse ouverte pour cette boutique/vendeur"));

            // ✅ Vérification de la caisse de la vente
        Caisse caisseVente = vente.getCaisse();
        if (caisseVente == null) {
            throw new RuntimeException("Impossible de rembourser, la caisse de la vente est fermée !");
        }
        if (caisseVente.getStatut() == StatutCaisse.FERMEE) {
            throw new RuntimeException("Impossible de rembourser : la caisse dans laquelle la vente a été encaissée est fermée !");
        }

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
            montantRembourse,
            "Remboursement vente ID " + vente.getId() + " : " + request.getMotif(),
            vente,
            vente.getModePaiement(),
            montantRembourse
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
        
        String userUuid;
        try {
            userUuid = jwtUtil.extractUserUuid(jwtToken.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'UUID de l'utilisateur depuis le token", e);
        }

        User user = usersRepository.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

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


 @Transactional(readOnly = true)
public VenteResponse getVenteById(Long id, HttpServletRequest httpRequest) {
    // 🔐 Récupération de l'utilisateur connecté via le token
    String token = httpRequest.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }
    User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);

    // 🔎 Charger la vente demandée
    Vente vente = venteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Vente non trouvée"));

    // 🔐 Vérification d’appartenance : la boutique de la vente doit appartenir à l’entreprise de l’utilisateur
    Long entrepriseVenteId = vente.getBoutique().getEntreprise().getId();
    if (!entrepriseVenteId.equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette vente n'appartient pas à votre entreprise.");
    }

    // 🔥 Si OK → on renvoie la réponse
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
         // ✅ Eviter récursion : ne pas renvoyer l’entité caisse entière
        if (vente.getCaisse() != null) {
            VenteResponse.CaisseDTO caisseDTO = new VenteResponse.CaisseDTO();
            caisseDTO.setId(vente.getCaisse().getId());
            caisseDTO.setMontantCourant(vente.getCaisse().getMontantCourant());
            caisseDTO.setStatut(vente.getCaisse().getStatut().name());
            caisseDTO.setDateOuverture(vente.getCaisse().getDateOuverture());
            caisseDTO.setDateFermeture(vente.getCaisse().getDateFermeture());
            response.setCaisse(caisseDTO);
        }
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
            // 🧾 Récupération du numéro de facture
        FactureVente facture = factureVenteRepository.findByVente(vente)
                .orElse(null);
        if (facture != null) {
            response.setNumeroFacture(facture.getNumeroFacture());
        }

        response.setNomVendeur(vente.getVendeur() != null ? vente.getVendeur().getNomComplet() : null);
        response.setNomBoutique(vente.getBoutique() != null ? vente.getBoutique().getNomBoutique() : null);
        return response;
    }


    // Methode pour recuperer montant total des vente de mon entreprise et seul admin et manager peuvent y acceder
    @Transactional(readOnly = true)
    public double getMontantTotalVentesDuJourConnecte(HttpServletRequest request) {
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

        return getMontantTotalVentesDuJour(entrepriseId);
    }

@Transactional(readOnly = true)
public double getMontantTotalVentesDuJour(Long entrepriseId) {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

    List<Vente> ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
            entrepriseId,
            startOfDay,
            endOfDay
    );

    // Optimisation N+1 : Récupérer tous les remboursements d'un coup
    List<Long> venteIds = ventes.stream().map(Vente::getId).collect(Collectors.toList());
    Map<Long, Double> remboursementsMap = venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds)
            .stream()
            .collect(Collectors.toMap(
                obj -> (Long) obj[0],
                obj -> ((Number) obj[1]).doubleValue()
            ));

    double total = 0.0;

    for (Vente vente : ventes) {
        double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
        double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
        total += montantVente - remboursements;
    }

    return total;
}

    // Vente du mois
    @Transactional(readOnly = true)
public double getMontantTotalVentesDuMoisConnecte(HttpServletRequest request) {
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

    return getMontantTotalVentesDuMois(entrepriseId);
}

@Transactional(readOnly = true)
public double getMontantTotalVentesDuMois(Long entrepriseId) {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
    LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

    List<Vente> ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
            entrepriseId,
            startOfMonth,
            endOfMonth
    );

    // Optimisation N+1 : Récupérer tous les remboursements d'un coup
    List<Long> venteIds = ventes.stream().map(Vente::getId).collect(Collectors.toList());
    Map<Long, Double> remboursementsMap = venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds)
            .stream()
            .collect(Collectors.toMap(
                obj -> (Long) obj[0],
                obj -> ((Number) obj[1]).doubleValue()
            ));

    double total = 0.0;

    for (Vente vente : ventes) {
        double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
        double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
        total += montantVente - remboursements;
    }

    return total;
}

    // Methode pour connaitre le benefiche net de lentreprise
    @Transactional(readOnly = true)
    public double calculerBeneficeNetEntrepriseConnecte(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        Long entrepriseId = user.getEntreprise().getId();
        
        
        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à cette information.");
        }

        return calculerBeneficeNetEntreprise(entrepriseId);
    }

    @Transactional(readOnly = true)
    public double calculerBeneficeNetEntreprise(Long entrepriseId) {
        List<Vente> ventes = venteRepository.findByBoutiqueEntrepriseId(entrepriseId);
        double beneficeNet = 0.0;

        for (Vente vente : ventes) {
            double beneficeVente = 0.0;

            for (VenteProduit vp : vente.getProduits()) {
                double prixVente = vp.getMontantLigne();
                double prixAchat = vp.getProduit().getPrixAchat() * vp.getQuantite();
                beneficeVente += prixVente - prixAchat;
            }

            // Déduire les remboursements
            double remboursements = venteHistoriqueRepository
                .findByVenteId(vente.getId())
                .stream()
                .filter(h -> h.getAction().equals("REMBOURSEMENT_VENTE"))
                .mapToDouble(VenteHistorique::getMontant)
                .sum();

            beneficeVente -= remboursements;
            beneficeNet += beneficeVente;
        }

        return beneficeNet;
    }
    
    //  Bénéfice net du jour
    @Transactional(readOnly = true)
    public double calculerBeneficeNetDuJourConnecte(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        

        Long entrepriseId = user.getEntreprise().getId();

         // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à cette information.");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<Vente> ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                entrepriseId, startOfDay, endOfDay
        );

        return calculerBeneficeNetVentes(ventes);
    }

    //Bénéfice net du mois
    @Transactional(readOnly = true)
    public double calculerBeneficeNetDuMoisConnecte(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Long entrepriseId = user.getEntreprise().getId();

        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à cette information.");
        }

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        LocalDateTime startOfMonth = firstDayOfMonth.atStartOfDay();
        LocalDateTime endOfMonth = lastDayOfMonth.atTime(LocalTime.MAX);

        List<Vente> ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                entrepriseId, startOfMonth, endOfMonth
        );

        return calculerBeneficeNetVentes(ventes);
    }

    private double calculerBeneficeNetVentes(List<Vente> ventes) {
        double beneficeNet = 0.0;

        for (Vente vente : ventes) {
            double beneficeVente = 0.0;

            for (VenteProduit vp : vente.getProduits()) {
                double prixVente = vp.getMontantLigne();
                double prixAchat = vp.getProduit().getPrixAchat() * vp.getQuantite();
                beneficeVente += prixVente - prixAchat;
            }

            // Déduire les remboursements
            double remboursements = venteHistoriqueRepository
                .findByVenteId(vente.getId())
                .stream()
                .filter(h -> h.getAction().equals("REMBOURSEMENT_VENTE"))
                .mapToDouble(VenteHistorique::getMontant)
                .sum();

            beneficeVente -= remboursements;
            beneficeNet += beneficeVente;
        }

        return beneficeNet;
    }

    //Benefice annuel
    @Transactional(readOnly = true)
public double calculerBeneficeNetAnnuelConnecte(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Long entrepriseId = user.getEntreprise().getId();
    // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à cette information.");
        }
    return calculerBeneficeNetAnnuel(entrepriseId);
}

@Transactional(readOnly = true)
public double calculerBeneficeNetAnnuel(Long entrepriseId) {
    LocalDate today = LocalDate.now();
    LocalDate firstDayOfYear = today.withDayOfYear(1);
    LocalDate lastDayOfYear = today.withDayOfYear(today.lengthOfYear());

    LocalDateTime startOfYear = firstDayOfYear.atStartOfDay();
    LocalDateTime endOfYear = lastDayOfYear.atTime(LocalTime.MAX);

    List<Vente> ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
            entrepriseId,
            startOfYear,
            endOfYear
    );

    // Optimisation N+1 : Récupérer tous les remboursements d'un coup
    List<Long> venteIds = ventes.stream().map(Vente::getId).collect(Collectors.toList());
    Map<Long, Double> remboursementsMap = venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds)
            .stream()
            .collect(Collectors.toMap(
                obj -> (Long) obj[0],
                obj -> ((Number) obj[1]).doubleValue()
            ));

    double beneficeNet = 0.0;

    for (Vente vente : ventes) {
        double beneficeVente = 0.0;

        for (VenteProduit vp : vente.getProduits()) {
            double prixVente = vp.getMontantLigne();
            double prixAchat = vp.getProduit().getPrixAchat() * vp.getQuantite();
            beneficeVente += prixVente - prixAchat;
        }

        // Déduire les remboursements
        double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
        beneficeVente -= remboursements;
        beneficeNet += beneficeVente;
    }

    return beneficeNet;
}


// Methode pour Achat de client 

@Transactional(readOnly = true)
public List<VenteParClientResponse> getVentesParClient(Long clientId, Long entrepriseClientId, HttpServletRequest request) {
    User user = utilitaire.getAuthenticatedUser(request);

    if (clientId == null && entrepriseClientId == null) {
        throw new RuntimeException("Vous devez fournir soit un clientId soit un entrepriseClientId.");
    }

    List<Vente> ventes;

    if (clientId != null) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        if (!client.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : ce client n'appartient pas à votre entreprise.");
        }
        ventes = venteRepository.findByClientId(clientId);
    } else {
        EntrepriseClient entrepriseClient = entrepriseClientRepository.findById(entrepriseClientId)
                .orElseThrow(() -> new RuntimeException("EntrepriseClient introuvable"));
        if (!entrepriseClient.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette entreprise cliente n'appartient pas à votre entreprise.");
        }
        ventes = venteRepository.findByEntrepriseClientId(entrepriseClientId);
    }

    return ventes.stream()
            .map(this::toVenteParClientResponse)
            .collect(Collectors.toList());
}

// Methode pour Achat de tout client confondu

@Transactional(readOnly = true)
public List<VenteParClientResponse> getVentesClientsAffilies(HttpServletRequest request) {
    User user = utilitaire.getAuthenticatedUser(request);

    // Récupère toutes les ventes de l'entreprise
    List<Vente> ventes = venteRepository.findAllByEntrepriseId(user.getEntreprise().getId());

    // Filtrer seulement les ventes avec client ou entreprise client
    List<Vente> ventesAffilies = ventes.stream()
        .filter(v -> (v.getClientNom() != null && v.getClientNumero() != null)
                  || v.getEntrepriseClient() != null)
        .collect(Collectors.toList());

    return ventesAffilies.stream()
            .map(this::toVenteParClientResponse)
            .collect(Collectors.toList());
}


private VenteParClientResponse toVenteParClientResponse(Vente vente) {
    VenteParClientResponse dto = new VenteParClientResponse();
    dto.setVenteId(vente.getId());
    dto.setCaisseId(vente.getCaisse() != null ? vente.getCaisse().getId() : null);
    dto.setBoutiqueId(vente.getBoutique() != null ? vente.getBoutique().getId() : null);
    dto.setVendeurId(vente.getVendeur() != null ? vente.getVendeur().getId() : null);
    dto.setDateVente(vente.getDateVente());
    dto.setMontantTotal(vente.getMontantTotal());
    dto.setDescription(vente.getDescription());
    dto.setNomVendeur(vente.getVendeur() != null ? vente.getVendeur().getNomComplet() : null);
    dto.setNomBoutique(vente.getBoutique() != null ? vente.getBoutique().getNomBoutique() : null);
    dto.setClientNom(vente.getClientNom());
    dto.setClientNumero(vente.getClientNumero());
    dto.setModePaiement(vente.getModePaiement());
    dto.setMontantPaye(vente.getMontantPaye());

    if (vente.getProduits() != null) {
        List<VenteLigneResponse> lignes = vente.getProduits().stream().map(vp -> {
            VenteLigneResponse ligne = new VenteLigneResponse();
            ligne.setProduitId(vp.getProduit() != null ? vp.getProduit().getId() : null);
            ligne.setNomProduit(vp.getProduit() != null ? vp.getProduit().getNom() : null);
            ligne.setQuantite(vp.getQuantite());
            ligne.setPrixUnitaire(vp.getPrixUnitaire());
            ligne.setMontantLigne(vp.getMontantLigne());
            ligne.setRemise(vp.getRemise());
            return ligne;
        }).collect(Collectors.toList());
        dto.setLignes(lignes);
    }

    dto.setRemiseGlobale(vente.getRemiseGlobale());
    FactureVente facture = factureVenteRepository.findByVente(vente).orElse(null);
    dto.setNumeroFacture(facture != null ? facture.getNumeroFacture() : null);

    return dto;
}   


}