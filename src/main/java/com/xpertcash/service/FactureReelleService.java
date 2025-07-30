package com.xpertcash.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Comparator;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.FactureReelleDTO;
import com.xpertcash.DTOs.PaiementDTO;
import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.LigneFactureReelle;
import com.xpertcash.entity.Paiement;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.StatutFactureProForma;
import com.xpertcash.entity.Enum.StatutFactureReelle;
import com.xpertcash.entity.Enum.StatutPaiementFacture;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.LigneFactureReelleRepository;
import com.xpertcash.repository.PaiementRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.Module.ModuleActivationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class FactureReelleService {

    @Autowired
    private FactureReelleRepository factureReelleRepository;

    @Autowired
    private LigneFactureReelleRepository ligneFactureReelleRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PaiementRepository paiementRepository;

     @Autowired
    private FactProHistoriqueService factProHistoriqueService;
    @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ModuleActivationService moduleActivationService;




    public FactureReelle genererFactureReelle(FactureProForma factureProForma) {
        FactureReelle factureReelle = new FactureReelle();

        // Copier les informations de la proforma
        factureReelle.setNumeroFacture(genererNumeroFactureReel(factureProForma.getEntreprise()));
        factureReelle.setDateCreation(LocalDate.now());
        factureReelle.setDateCreationPro(factureProForma.getDateCreation());
        factureReelle.setTotalHT(factureProForma.getTotalHT());
        factureReelle.setRemise(factureProForma.getRemise());
        factureReelle.setTauxRemise(factureProForma.getTauxRemise());
        factureReelle.setDescription(factureProForma.getDescription());

        factureReelle.setTva(factureProForma.isTva());
        factureReelle.setTotalFacture(factureProForma.getTotalFacture());
        factureReelle.setStatutPaiement(StatutPaiementFacture.EN_ATTENTE);
        factureReelle.setUtilisateurCreateur(factureProForma.getUtilisateurCreateur());
        factureReelle.setUtilisateurValidateur(factureProForma.getUtilisateurValidateur());
        factureReelle.setClient(factureProForma.getClient());
        factureReelle.setEntrepriseClient(factureProForma.getEntrepriseClient());
        factureReelle.setEntreprise(factureProForma.getEntreprise());
        factureReelle.setFactureProForma(factureProForma);

        // Sauvegarder la facture réelle AVANT d'ajouter les lignes (important pour les relations en base)
        FactureReelle factureReelleSauvegardee = factureReelleRepository.save(factureReelle);

        // Copier les lignes de facture
        List<LigneFactureReelle> lignesFacture = factureProForma.getLignesFacture().stream().map(ligneProForma -> {
            LigneFactureReelle ligneReelle = new LigneFactureReelle();
            ligneReelle.setProduit(ligneProForma.getProduit());
            ligneReelle.setQuantite(ligneProForma.getQuantite());
            ligneReelle.setPrixUnitaire(ligneProForma.getPrixUnitaire());
            ligneReelle.setLigneDescription(ligneProForma.getLigneDescription());
            ligneReelle.setMontantTotal(ligneProForma.getMontantTotal());
            ligneReelle.setFactureReelle(factureReelleSauvegardee);
            return ligneReelle;
        }).collect(Collectors.toList());

        ligneFactureReelleRepository.saveAll(lignesFacture);

        return factureReelleSauvegardee;
    }


    private String genererNumeroFactureReel(Entreprise entreprise) {
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));

        List<FactureReelle> facturesDeLAnnee = factureReelleRepository.findFacturesDeLAnnee(year);
        long newIndex = 1;

        if (!facturesDeLAnnee.isEmpty()) {
            String lastNumeroFacture = facturesDeLAnnee.get(0).getNumeroFacture();

            Pattern pattern = Pattern.compile("(\\d+)");
            Matcher matcher = pattern.matcher(lastNumeroFacture);

            if (matcher.find()) {
                try {
                    newIndex = Long.parseLong(matcher.group(1)) + 1;
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Impossible de parser l'index numérique dans le numéro : " + lastNumeroFacture, e);
                }
            } else {
                throw new RuntimeException("Format de numéro de facture invalide : " + lastNumeroFacture);
            }
        }

        String indexFormate = String.format("%03d", newIndex);

        String prefixe = entreprise != null && entreprise.getPrefixe() != null ? entreprise.getPrefixe().trim() : "";
        String suffixe = entreprise != null && entreprise.getSuffixe() != null ? entreprise.getSuffixe().trim() : "";

        StringBuilder numeroFacture = new StringBuilder("");

        if (!prefixe.isEmpty() && suffixe.isEmpty()) {
            numeroFacture.append(prefixe).append("-").append(indexFormate).append("-").append(formattedDate);
        } else if (prefixe.isEmpty() && !suffixe.isEmpty()) {
            numeroFacture.append(indexFormate).append("-").append(formattedDate).append("-").append(suffixe);
        } else if (!prefixe.isEmpty() && !suffixe.isEmpty()) {
            numeroFacture.append(prefixe).append("-").append(indexFormate).append("-").append(formattedDate);
        } else {
            numeroFacture.append(indexFormate).append("-").append(formattedDate);
        }

        return numeroFacture.toString();
    }

   // Methode pour Supprimer facturer deja generer une fois annuler
public void supprimerFactureReelleLiee(FactureProForma proforma) {
    List<FactureReelle> facturesReelles = factureReelleRepository.findAllByFactureProForma(proforma);
    if (facturesReelles.isEmpty()) {
        System.out.println("Aucune facture réelle associée à cette facture proforma.");
        return;
    }

    for (FactureReelle factureReelle : facturesReelles) {
        factureReelleRepository.delete(factureReelle);
        System.out.println("🗑️ Facture réelle supprimée suite à l'annulation.");
    }
}


   // Méthode pour lister les factures réelles
    public List<FactureReelleDTO> listerMesFacturesReelles(HttpServletRequest request) {
        // 🔐 Récupération et validation du token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long userId;
        try {
            userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'utilisateur", e);
        }

        // 👤 Utilisateur courant
        User utilisateur = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 🏢 Vérification de l'entreprise
        Entreprise entreprise = utilisateur.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // 🔐 Vérification des permissions
        boolean isAdminOrManager = CentralAccess.isSelfOrAdminOrManager(utilisateur, entreprise.getId());
        boolean hasPermission = utilisateur.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

        // 📦 Vérification de l'activation du module
        moduleActivationService.verifierAccesModulePourEntreprise(entreprise, "GESTION_FACTURATION");

        // 🔍 Récupération des factures
        List<FactureReelle> factures;
        if (isAdminOrManager || hasPermission) {
            // Peut voir toutes les factures de l’entreprise
            factures = factureReelleRepository.findByEntrepriseOrderByDateCreationDesc(entreprise);
        } else {
            // Peut voir uniquement ses propres factures
            factures = factureReelleRepository.findByEntrepriseAndUtilisateurCreateurOrderByDateCreationDesc(
                    entreprise, utilisateur
            );
        }

        // 🔄 Transformation en DTO
        return factures.stream()
                .map(facture -> {
                    BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
                    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(facture.getId());
                    if (totalPaye == null) totalPaye = BigDecimal.ZERO;
                    BigDecimal montantRestant = totalFacture.subtract(totalPaye);
                    return new FactureReelleDTO(facture, montantRestant);
                })
                .collect(Collectors.toList());
    }

    // Trier les facture par mois/année
    public ResponseEntity<?> filtrerFacturesParMoisEtAnnee(Integer mois, Integer annee, HttpServletRequest request) {
        // Extraire l'utilisateur à partir du token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long userId;
        try {
            userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur", e);
        }

        // Récupérer l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Long entrepriseId = user.getEntreprise().getId();

        // Récupérer les factures selon les filtres
        List<FactureReelle> factures;

        if (mois != null && annee != null) {
            factures = factureReelleRepository.findByMonthAndYearAndEntreprise(mois, annee, entrepriseId);
        } else if (mois != null) {
            factures = factureReelleRepository.findByMonthAndEntreprise(mois, entrepriseId);
        } else if (annee != null) {
            factures = factureReelleRepository.findByYearAndEntreprise(annee, entrepriseId);
        } else {
            factures = factureReelleRepository.findByEntrepriseId(entrepriseId);
        }

            // 4. Mapper vers DTO et nettoyer
    List<FactureReelleDTO> factureDTOs = factures.stream().map(facture -> {
        BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
        BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(facture.getId());
        if (totalPaye == null) totalPaye = BigDecimal.ZERO;

        BigDecimal montantRestant = totalFacture.subtract(totalPaye);
        FactureReelleDTO dto = new FactureReelleDTO(facture, montantRestant);

        // On ignior les champs inutiles
        // dto.setUtilisateur(null);
        dto.setEntrepriseClient(null);
        dto.setClient(null);
        dto.setLignesFacture(null);

        if (facture.getClient() != null) {
        dto.setNomClient(facture.getClient().getNomComplet());
        } else if (facture.getEntrepriseClient() != null) {
            dto.setNomEntrepriseClient(facture.getEntrepriseClient().getNom());
        }

        return dto;
    }).collect(Collectors.toList());

    if (factureDTOs.isEmpty()) {
        return ResponseEntity.ok("Aucune facture trouvée.");
    }

    return ResponseEntity.ok(factureDTOs);

    }

    // Methode Get facture reel by id
    public FactureReelleDTO getFactureReelleById(Long factureId, HttpServletRequest request) {
    // 🔐 Extraire le token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur", e);
    }

    // 👤 Récupérer l'utilisateur connecté
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    // 📄 Récupérer la facture
    FactureReelle facture = factureReelleRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Aucune facture trouvée"));

    // Charger explicitement la relation factureProForma
    Hibernate.initialize(facture.getFactureProForma());

    Long entrepriseFactureId = facture.getEntreprise() != null ? facture.getEntreprise().getId() : null;
    Long entrepriseUserId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;

     // 🔐 Vérification entreprise
    if (entrepriseFactureId == null || entrepriseUserId == null || !entrepriseFactureId.equals(entrepriseUserId)) {
        throw new RuntimeException("Accès refusé : cette facture ne vous appartient pas !");
    }

    // 🔒 Vérification des rôles et permissions
        // 🔒 Vérification des rôles et permissions
    boolean isAdminOrManagerOfEntreprise = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseFactureId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
    boolean isCreateur = facture.getUtilisateurCreateur().getId().equals(userId);

    if (!(isAdminOrManagerOfEntreprise || hasPermission || isCreateur)) {
        throw new RuntimeException("Accès interdit : vous n'avez pas les droits pour consulter cette facture !");
    }


    // 💰 Calculer le montant restant
    BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureId);
    if (totalPaye == null) totalPaye = BigDecimal.ZERO;

    BigDecimal montantRestant = totalFacture.subtract(totalPaye);

    // ✅ Retour DTO
    return new FactureReelleDTO(facture, montantRestant);
}


  public FactureReelle enregistrerPaiement(Long factureId, BigDecimal montant, String modePaiement, HttpServletRequest request) {
    // 🔐 Vérification du token
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    User utilisateur = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    // 🧾 Récupération de la facture
    FactureReelle facture = factureReelleRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture introuvable"));

    Entreprise entrepriseUtilisateur = utilisateur.getEntreprise();
    Entreprise entrepriseFacture = facture.getEntreprise();

    if (entrepriseUtilisateur == null || entrepriseFacture == null ||
        !entrepriseUtilisateur.getId().equals(entrepriseFacture.getId())) {
        throw new RuntimeException("L'utilisateur et la facture ne sont pas dans la même entreprise.");
    }

    // 🔐 Vérification des droits
    boolean isAdmin = CentralAccess.isAdminOfEntreprise(utilisateur, entrepriseUtilisateur.getId());
    boolean hasPermission = utilisateur.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    if (!isAdmin && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour enregistrer un paiement.");
    }

    // ✅ Vérification du module
    moduleActivationService.verifierAccesModulePourEntreprise(entrepriseUtilisateur, "GESTION_FACTURATION");

    // ❌ Facture déjà réglée
    if (facture.getStatutPaiement() == StatutPaiementFacture.PAYEE) {
        throw new RuntimeException("Cette facture est déjà totalement réglée.");
    }

    // 💰 Recalcul du total payé avant ce nouveau paiement
    BigDecimal totalPayeAvant = paiementRepository.sumMontantsByFactureReelle(factureId);
    if (totalPayeAvant == null) totalPayeAvant = BigDecimal.ZERO;

    BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
    BigDecimal totalApresPaiement = totalPayeAvant.add(montant);

    if (totalApresPaiement.compareTo(totalFacture) > 0) {
        BigDecimal montantRestant = totalFacture.subtract(totalPayeAvant);
        throw new RuntimeException("Le paiement dépasse le montant total de la facture. Montant restant dû : " + montantRestant + " FCFA");
    }

    // 📝 Création du paiement
    Paiement paiement = new Paiement();
    paiement.setMontant(montant);
    paiement.setDatePaiement(LocalDate.now());
    paiement.setFactureReelle(facture);
    paiement.setModePaiement(modePaiement);
    paiement.setEncaissePar(utilisateur);

    paiementRepository.save(paiement);

    // 🔁 Mise à jour du statut de la facture
    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureId);
    if (totalPaye.compareTo(totalFacture) >= 0) {
        facture.setStatutPaiement(StatutPaiementFacture.PAYEE);
    } else if (totalPaye.compareTo(BigDecimal.ZERO) > 0) {
        facture.setStatutPaiement(StatutPaiementFacture.PARTIELLEMENT_PAYEE);
    } else {
        facture.setStatutPaiement(StatutPaiementFacture.EN_ATTENTE);
    }

    return factureReelleRepository.save(facture);
}

    //Facture impayer dune facture
    public BigDecimal getMontantRestant(Long factureId) {
        FactureReelle facture = factureReelleRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture introuvable"));

        BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
        BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureId);

        return totalFacture.subtract(totalPaye);
    }

    //Get les paiements d'une facture
  public List<PaiementDTO> getPaiementsParFacture(Long factureId, HttpServletRequest request) {
    // 1. Authentification
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

    User user = usersRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Utilisateur n'est associé à aucune entreprise");
    }

    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasGestionFacturePermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    // 2. Vérification de la facture
    FactureReelle facture = factureReelleRepository.findById(factureId)
        .orElseThrow(() -> new RuntimeException("Facture introuvable"));

    // 3. Vérification d'appartenance à l'entreprise
    if (!facture.getEntreprise().getId().equals(entreprise.getId())) {
        throw new RuntimeException("Accès interdit à une facture d'une autre entreprise");
    }

    // 4. Vérification des droits d'accès
    boolean isCreateur = facture.getUtilisateurCreateur().getId().equals(userId);
    if (!(isAdminOrManager || hasGestionFacturePermission || isCreateur)) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour consulter les paiements de cette facture");
    }

    // 5. Récupération et mapping
    List<Paiement> paiements = paiementRepository.findByFactureReelle(facture);
    return paiements.stream().map(PaiementDTO::new).collect(Collectors.toList());
}


    //Facture impayer all facture
   public List<FactureReelleDTO> listerFacturesImpayees(HttpServletRequest request) {
    // 🔐 1. Extraire le token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'utilisateur depuis le token", e);
    }

    // 👤 2. Récupérer l'utilisateur connecté
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    if (user.getEntreprise() == null) {
        throw new RuntimeException("Utilisateur n'est associé à aucune entreprise");
    }

    Long entrepriseId = user.getEntreprise().getId();

    // 🔐 3. Vérifier s'il a le droit de voir toutes les factures
    boolean isAuthorized = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId)
            || user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    // 🧾 4. Statuts concernés
    List<StatutPaiementFacture> statutsImpayes = List.of(
        StatutPaiementFacture.EN_ATTENTE,
        StatutPaiementFacture.PARTIELLEMENT_PAYEE
    );

    List<FactureReelle> factures;

    if (isAuthorized) {
        // Peut voir toutes les factures impayées de son entreprise
        factures = factureReelleRepository.findByEntrepriseIdAndStatutPaiementIn(entrepriseId, statutsImpayes);
    } else {
        // Peut seulement voir les factures qu'il a créées dans son entreprise
        factures = factureReelleRepository.findByEntrepriseIdAndUtilisateurCreateurIdAndStatutPaiementIn(
            entrepriseId, userId, statutsImpayes
        );
    }

    // 📦 5. Mapper vers des DTO filtrés
    return factures.stream().map(facture -> {
        BigDecimal total = BigDecimal.valueOf(facture.getTotalFacture());
        BigDecimal paye = paiementRepository.sumMontantsByFactureReelle(facture.getId());
        if (paye == null) paye = BigDecimal.ZERO;

        BigDecimal montantRestant = total.subtract(paye);

        FactureReelleDTO dto = new FactureReelleDTO(facture, montantRestant);

        // Nettoyage des infos sensibles ou inutiles
        dto.setEntrepriseClient(null);
        dto.setClient(null);
        dto.setLignesFacture(null);

        return dto;
    }).collect(Collectors.toList());
}


//Modifier le statut d'une facture
@Transactional
public FactureProForma annulerFactureReelle(FactureReelle modifications, HttpServletRequest request) {
    // 🔐 1. Extraction utilisateur depuis JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
    }

    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

    // 📄 2. Récupération de la facture réelle
    FactureReelle factureReelle = factureReelleRepository.findById(modifications.getId())
            .orElseThrow(() -> new RuntimeException("Facture réelle introuvable !"));

    FactureProForma factureProForma = factureReelle.getFactureProForma();
    Long entrepriseFactureId = factureReelle.getEntreprise().getId();

    // 🔐 3. Vérification des droits
    boolean isAuthorized = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseFactureId)
            || user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    if (!isAuthorized) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour annuler cette facture.");
    }

    // 🔒 4. Blocage si déjà annulée
    if (factureProForma.getStatut() == StatutFactureProForma.ANNULE) {
        throw new RuntimeException("Cette facture est déjà annulée.");
    }

    // 🔒 5. Vérifier les paiements
    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureReelle.getId());
    if (totalPaye != null && totalPaye.compareTo(BigDecimal.ZERO) > 0) {
        throw new RuntimeException("Impossible d’annuler : des paiements ont déjà été effectués.");
    }

    // ✅ 6. Suppression et mise à jour
    supprimerFactureReelleLiee(factureProForma);

    factureProForma.setStatut(StatutFactureProForma.ANNULE);
    factureProForma.setDateAnnulation(LocalDateTime.now());
    factureProForma.setUtilisateurAnnulateur(user);
    factureProForma.setDateRelance(null);
    factureProForma.setDernierRappelEnvoye(null);
    factureProForma.setNotifie(false);

    // 📝 7. Historique
    factProHistoriqueService.enregistrerActionHistorique(
            factureProForma,
            user,
            "Annulation",
            "Facture réelle supprimée et proforma annulée. Aucun paiement enregistré."
    );

    return factureProformaRepository.save(factureProForma);
}



//Trier

public List<Map<String, Object>> getFacturesParPeriode(Long userIdRequete, HttpServletRequest request,
                                                           String typePeriode, LocalDate dateDebut, LocalDate dateFin) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long userIdCourant = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        User currentUser = usersRepository.findById(userIdCourant)
                .orElseThrow(() -> new RuntimeException("Utilisateur courant introuvable"));
        User targetUser = usersRepository.findById(userIdRequete)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

        Entreprise entrepriseCourante = currentUser.getEntreprise();
        Entreprise entrepriseCible = targetUser.getEntreprise();

        if (entrepriseCourante == null || entrepriseCible == null
            || !entrepriseCourante.getId().equals(entrepriseCible.getId())) {
            throw new RuntimeException("Opération interdite : utilisateurs de différentes entreprises.");
        }

        

        LocalDateTime dateStart;
        LocalDateTime dateEnd;

        switch (typePeriode.toLowerCase()) {
            case "jour":
                dateStart = LocalDate.now().atStartOfDay();
                dateEnd = dateStart.plusDays(1);
                break;
            case "mois":
                dateStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                dateEnd = dateStart.plusMonths(1);
                break;
            case "annee":
                dateStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
                dateEnd = dateStart.plusYears(1);
                break;
            case "personnalise":
                if (dateDebut == null || dateFin == null) {
                    throw new RuntimeException("Dates de début et de fin requises pour une période personnalisée.");
                }
                dateStart = dateDebut.atStartOfDay();
                dateEnd = dateFin.plusDays(1).atStartOfDay();
                break;
            default:
                throw new RuntimeException("Type de période invalide.");
        }

        List<FactureReelle> factures = factureReelleRepository.findByEntrepriseIdAndDateCreationBetween(
                entrepriseCourante.getId(), dateStart.toLocalDate(), dateEnd.toLocalDate()
        );

        return factures.stream()
            .sorted(Comparator.comparing(FactureReelle::getDateCreation).reversed())
            .map(facture -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", facture.getId());
                map.put("numeroFacture", facture.getNumeroFacture());
                map.put("dateCreation", facture.getDateCreation());
                map.put("description", facture.getDescription());
                map.put("totalHT", facture.getTotalHT());
                map.put("remise", facture.getRemise());
                map.put("tva", facture.isTva());
                map.put("totalFacture", facture.getTotalFacture());
                map.put("statut", facture.getStatut());
                map.put("ligneFactureProforma", facture.getLignesFacture());
                map.put("client", facture.getClient() != null ? facture.getClient().getNomComplet() : null);
                map.put("entrepriseClient", facture.getEntrepriseClient() != null ? facture.getEntrepriseClient().getNom() : null);
                map.put("entreprise", facture.getEntreprise() != null ? facture.getEntreprise().getNomEntreprise() : null);
                return map;
            })
            .collect(Collectors.toList());
    }

}