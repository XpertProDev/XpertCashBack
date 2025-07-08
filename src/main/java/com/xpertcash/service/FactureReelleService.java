package com.xpertcash.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.FactureReelleDTO;
import com.xpertcash.DTOs.PaiementDTO;
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


    //Methode pour lister les factures Reel
    public List<FactureReelleDTO> listerMesFacturesReelles(HttpServletRequest request) {
        // 1. Récupérer le token et extraire l'ID utilisateur
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

        User utilisateur = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Entreprise entreprise = utilisateur.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        moduleActivationService.verifierAccesModulePourEntreprise(entreprise, "GESTION_FACTURATION");

        List<FactureReelle> factures = factureReelleRepository.findByEntrepriseOrderByDateCreationDesc(entreprise);

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
        dto.setUtilisateur(null);
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

    // 👤 Récupérer l'utilisateur et son entreprise
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    Long entrepriseId = user.getEntreprise().getId();

    // 📄 Récupérer la facture
    FactureReelle facture = factureReelleRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Aucune facture trouvée"));

    // 🔐 Vérifier que la facture appartient bien à l'entreprise de l'utilisateur
    if (!facture.getEntreprise().getId().equals(entrepriseId)) {
        throw new RuntimeException("Accès refusé : cette facture ne vous appartient pas !");
    }

    // 💰 Calculer le montant restant
    BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureId);
    if (totalPaye == null) totalPaye = BigDecimal.ZERO;

    BigDecimal montantRestant = totalFacture.subtract(totalPaye);

    // ✅ Retourner le DTO avec montantRestant
    return new FactureReelleDTO(facture, montantRestant);
}


   public FactureReelle enregistrerPaiement(Long factureId, BigDecimal montant, String modePaiement, HttpServletRequest request) {
    FactureReelle facture = factureReelleRepository.findById(factureId)
        .orElseThrow(() -> new RuntimeException("Facture introuvable"));

      // Bloquer tout paiement si facture déjà réglée
    if (facture.getStatutPaiement() == StatutPaiementFacture.PAYEE) {
        throw new RuntimeException("Cette facture est déjà totalement réglée.");
    }

    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    User utilisateur = usersRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    // Recalculer le total payé avant ce nouveau paiement
    BigDecimal totalPayeAvant = paiementRepository.sumMontantsByFactureReelle(factureId);
    if (totalPayeAvant == null) totalPayeAvant = BigDecimal.ZERO;

    BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());

    // Calcul du nouveau total après ajout de ce paiement
    BigDecimal totalApresPaiement = totalPayeAvant.add(montant);

    // On n'accepte pas un dépassement du montant total
    if (totalApresPaiement.compareTo(totalFacture) > 0) {
        BigDecimal montantRestant = totalFacture.subtract(totalPayeAvant);
        throw new RuntimeException("Le paiement dépasse le montant total de la facture. Montant restant dû : " + montantRestant + " FCFA");
    }

    // Création du paiement
    Paiement paiement = new Paiement();
    paiement.setMontant(montant);
    paiement.setDatePaiement(LocalDate.now());
    paiement.setFactureReelle(facture);
    paiement.setModePaiement(modePaiement);
    paiement.setEncaissePar(utilisateur);

    paiementRepository.save(paiement);

    // Recalculer après paiement
    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureId);

    // Mettre à jour le statut
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
    boolean hasGestionFacturePermission = user.getRole().hasPermission(PermissionType.Gestion_Facture);

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
    // 🔐 1. Extraire l'utilisateur connecté depuis le token
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

    Long entrepriseId = entreprise.getId();

    // 🔐 2. Vérification des rôles et permissions
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasGestionFacturePermission = user.getRole().hasPermission(PermissionType.Gestion_Facture);

    List<FactureReelle> factures;

    // 🔎 3. Sélectionner les factures impayées
    List<StatutPaiementFacture> statutsImpayes = List.of(
        StatutPaiementFacture.EN_ATTENTE,
        StatutPaiementFacture.PARTIELLEMENT_PAYEE
    );

    if (isAdminOrManager || hasGestionFacturePermission) {
        factures = factureReelleRepository.findByEntrepriseIdAndStatutPaiementIn(entrepriseId, statutsImpayes);
    } else {
        factures = factureReelleRepository.findByEntrepriseIdAndUtilisateurCreateurIdAndStatutPaiementIn(
            entrepriseId, userId, statutsImpayes
        );
    }

   // on mappe et filtrer les champs
    return factures.stream().map(facture -> {
        BigDecimal total = BigDecimal.valueOf(facture.getTotalFacture());
        BigDecimal paye = paiementRepository.sumMontantsByFactureReelle(facture.getId());
        if (paye == null) paye = BigDecimal.ZERO;

        BigDecimal montantRestant = total.subtract(paye);

        // DTO avec tous les champs remplis
        FactureReelleDTO dto = new FactureReelleDTO(facture, montantRestant);

        // Supprimer les champs non nécessaires pour cette route
        dto.setUtilisateur(null);
        dto.setEntrepriseClient(null);
        dto.setClient(null);
        dto.setLignesFacture(null);

        return dto;
    }).collect(Collectors.toList());
}



//Modifier le statut d'une facture
@Transactional
public FactureProForma annulerFactureReelle(FactureReelle modifications, HttpServletRequest request) {
    // 🔐 Extraction utilisateur depuis JWT
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

    FactureReelle factureReelle = factureReelleRepository.findById(modifications.getId())
            .orElseThrow(() -> new RuntimeException("Facture réelle introuvable !"));

    FactureProForma factureProForma = factureReelle.getFactureProForma();

    // érification du rôle et permission
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasGestionFacturePermission = user.getRole().hasPermission(PermissionType.Gestion_Facture);

    
    if (!isAdminOrManager && !hasGestionFacturePermission) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour annuler cette facture.");
    }
    if (!isAdminOrManager ) {
        throw new RuntimeException("Vous n'êtes pas autorisé à annuler cette facture.");
    }

    // 🔒 Blocage si déjà annulée
    if (factureProForma.getStatut() == StatutFactureProForma.ANNULE) {
        throw new RuntimeException("Cette facture est déjà annulée.");
    }

    // 🔒 Paiements associés ?
    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureReelle.getId());
    if (totalPaye != null && totalPaye.compareTo(BigDecimal.ZERO) > 0) {
        throw new RuntimeException("Impossible d’annuler : des paiements ont déjà été effectués.");
    }

    // ✅ Suppression via méthode utilitaire
    supprimerFactureReelleLiee(factureProForma);

    // ✅ Mise à jour de la proforma
    factureProForma.setStatut(StatutFactureProForma.ANNULE);
    factureProForma.setDateAnnulation(LocalDateTime.now());
    factureProForma.setUtilisateurAnnulateur(user);
    factureProForma.setDateRelance(null);
    factureProForma.setDernierRappelEnvoye(null);
    factureProForma.setNotifie(false);

    // 📝 Historique
    factProHistoriqueService.enregistrerActionHistorique(
            factureProForma,
            user,
            "Annulation",
            "Facture réelle supprimée et proforma annulée. Aucun paiement enregistré."
    );

    return factureProformaRepository.save(factureProForma);
}


}