package com.xpertcash.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Client;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.LigneFactureProforma;
import com.xpertcash.entity.MethodeEnvoi;
import com.xpertcash.entity.NoteFactureProForma;
import com.xpertcash.entity.Permission;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.StatutFactureProForma;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.NoteFactureProFormaRepository;
import com.xpertcash.repository.PaiementRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FactureProformaService {

    private static final Logger log = LoggerFactory.getLogger(FactureProformaService.class);

    @Autowired
    private FactureProformaRepository factureProformaRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;
    @Autowired
    private FactureReelleService factureReelleService;

    @Autowired
    private FactProHistoriqueService factProHistoriqueService;

     @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private FactureReelleRepository factureReelleRepository;

    @Autowired
    private NoteFactureProFormaRepository noteFactureProFormaRepository;
    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private JwtUtil jwtUtil;
    
    // Methode pour creer une facture pro forma
    public FactureProForma ajouterFacture(FactureProForma facture, Double remisePourcentage, Boolean appliquerTVA, HttpServletRequest request) {
    if (facture == null) {
        throw new RuntimeException("La facture ne peut pas être vide !");
    }

    // Vérifier la présence du token JWT et récupérer l'ID de l'utilisateur connecté
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = null;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
    }

    // Récupérer l'utilisateur par son ID
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

    // Vérifier que l'utilisateur a une entreprise associée (entreprise créatrice de la facture)
    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    facture.setEntreprise(entrepriseUtilisateur);


    // Vérifier la présence d'un client ou entreprise destinataire pour la facture
    if ((facture.getClient() == null || facture.getClient().getId() == null) &&
        (facture.getEntrepriseClient() == null || facture.getEntrepriseClient().getId() == null)) {
        throw new RuntimeException("Un client ou une entreprise doit être spécifié pour la facture !");
    }

    // Génération du numéro de la facture automatiquement
    facture.setNumeroFacture(generateNumeroFacture(entrepriseUtilisateur));


    // Vérifier que la remise est comprise entre 0 et 100%
    if (remisePourcentage == null) {
        remisePourcentage = 0.0;
    } else if (remisePourcentage < 0 || remisePourcentage > 100) {
        throw new RuntimeException("Le pourcentage de remise doit être compris entre 0 et 100 !");
    }

    Long clientId = (facture.getClient() != null) ? facture.getClient().getId() : null;
    Long entrepriseClientId = (facture.getEntrepriseClient() != null) ? facture.getEntrepriseClient().getId() : null;

    // Vérifier si une facture similaire existe déjà
    List<FactureProForma> facturesExistantes = factureProformaRepository.findExistingFactures(clientId, entrepriseClientId, StatutFactureProForma.BROUILLON);

    for (FactureProForma fExistante : facturesExistantes) {
        List<Long> produitsExistants = fExistante.getLignesFacture()
                                                .stream()
                                                .map(l -> l.getProduit().getId())
                                                .collect(Collectors.toList());

        List<Long> nouveauxProduits = facture.getLignesFacture()
                                             .stream()
                                             .map(l -> l.getProduit().getId())
                                             .collect(Collectors.toList());

        if (new HashSet<>(produitsExistants).equals(new HashSet<>(nouveauxProduits))) {
            throw new RuntimeException("Une facture avec ces produits existe déjà pour ce client ou cette entreprise !");
        }
    }

    // Associer le Client ou l'Entreprise destinataire à la facture
    if (clientId != null) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable !"));
        facture.setClient(client);
    }

    if (entrepriseClientId != null) {
        EntrepriseClient entrepriseClient = entrepriseClientRepository.findById(entrepriseClientId)
                .orElseThrow(() -> new RuntimeException("Entreprise destinataire introuvable !"));
        facture.setEntrepriseClient(entrepriseClient);  // C'est ici que nous associons l'entreprise destinataire
    }

    // Initialisation des valeurs
    facture.setStatut(StatutFactureProForma.BROUILLON);
    facture.setDateCreation(LocalDate.now());

    if (facture.getDateFacture() == null) {
        facture.setDateFacture(LocalDate.now());
    }

    double montantTotalHT = 0;
    if (facture.getLignesFacture() != null) {
        for (LigneFactureProforma ligne : facture.getLignesFacture()) {
            Produit produit = produitRepository.findById(ligne.getProduit().getId())
                    .orElseThrow(() -> new RuntimeException("Produit avec ID " + ligne.getProduit().getId() + " introuvable !"));

            ligne.setFactureProForma(facture);
            ligne.setProduit(produit);
            ligne.setPrixUnitaire(produit.getPrixVente());

            ligne.setMontantTotal(ligne.getQuantite() * ligne.getPrixUnitaire());

            if (ligne.getLigneDescription() != null) {
                ligne.setLigneDescription(ligne.getLigneDescription());
            } else {
                ligne.setLigneDescription(produit.getDescription());
            }

            // Ajout au montant total HT
            montantTotalHT += ligne.getMontantTotal();
        }
    }

    // Calcul de la remise
    double remiseMontant = (remisePourcentage > 0) ? montantTotalHT * (remisePourcentage / 100) : 0;

    // Appliquer la TVA uniquement si elle est activée
    boolean tvaActive = appliquerTVA != null && appliquerTVA;
    double montantTVA = 0;
    if (tvaActive) {
        Double tauxTva = entrepriseUtilisateur.getTauxTva();
        if (tauxTva == null) {
            throw new RuntimeException("Le taux de TVA de l'entreprise n'est pas défini !");
        }
        montantTVA = (montantTotalHT - remiseMontant) * tauxTva;
    }


    // Calcul du montant total à payer
    double montantTotalAPayer = (montantTotalHT - remiseMontant) + montantTVA;

    // Assigner les montants calculés à la facture
    facture.setTotalHT(montantTotalHT);
    facture.setRemise(remiseMontant);
    facture.setTauxRemise(remisePourcentage);
    facture.setTva(tvaActive);
    facture.setTotalFacture(montantTotalAPayer);


    facture.setUtilisateurCreateur(user);


    return factureProformaRepository.save(facture);
}

    // Méthode pour générer un numéro de facture unique
    private String generateNumeroFacture(Entreprise entreprise) {
    LocalDate currentDate = LocalDate.now();
    int year = currentDate.getYear();
    String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));

    List<FactureProForma> facturesDeLAnnee = factureProformaRepository.findFacturesDeLAnnee(year);

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

    String indexFormatte = String.format("%03d", newIndex);

    String prefixe = entreprise.getPrefixe() != null ? entreprise.getPrefixe().trim() : "";
    String suffixe = entreprise.getSuffixe() != null ? entreprise.getSuffixe().trim() : "";

    StringBuilder numeroFacture = new StringBuilder();

    if (!prefixe.isEmpty() && suffixe.isEmpty()) {
        numeroFacture.append(prefixe).append("-");
        numeroFacture.append(indexFormatte).append("-");
        numeroFacture.append(formattedDate);
    } else if (prefixe.isEmpty() && !suffixe.isEmpty()) {
        numeroFacture.append(indexFormatte).append("-");
        numeroFacture.append(formattedDate).append("-");
        numeroFacture.append(suffixe);
    } else if (!prefixe.isEmpty() && !suffixe.isEmpty()) {
        // Choix : on garde uniquement prefixe ici
        numeroFacture.append(prefixe).append("-");
        numeroFacture.append(indexFormatte).append("-");
        numeroFacture.append(formattedDate);
    } else {
        // Pas de prefixe ni suffixe
        numeroFacture.append(indexFormatte).append("-");
        numeroFacture.append(formattedDate);
    }

    return numeroFacture.toString();
}

    // Méthode pour modifier une facture pro forma
    @Transactional
    public FactureProForma modifierFacture(Long factureId, Double remisePourcentage, Boolean appliquerTVA, FactureProForma modifications, List<Long> idsApprobateurs, HttpServletRequest request) {
        // 🔐 Récupération de la facture
        FactureProForma facture = factureProformaRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée !"));

        // Stocker l'ancien montant total HT avant toute modification
        double ancienTotalHT = facture.getTotalHT();

        // 🔐 Extraction de l'utilisateur depuis le token JWT
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

        // 🔒 Blocage total si facture annulée
        if (facture.getStatut() == StatutFactureProForma.ANNULE) {
            throw new RuntimeException("Cette facture est annulée. Elle ne peut plus être modifiée.");
        }

        // 🔒 Traitement spécial si facture VALIDÉE
        if (facture.getStatut() == StatutFactureProForma.VALIDE) {
            boolean tentativeModification = modifications.getLignesFacture() != null
                    || remisePourcentage != null
                    || appliquerTVA != null
                    || (modifications.getStatut() != null && modifications.getStatut() != StatutFactureProForma.ANNULE);

            if (tentativeModification) {
                throw new RuntimeException("Impossible de modifier une facture VALIDÉE, sauf pour l’annuler.");
            }
        }

        // Si demande d’annulation 
        if (modifications.getStatut() == StatutFactureProForma.ANNULE) {

                // si paiements existants
            Optional<FactureReelle> factureReelleOpt = factureReelleRepository.findByFactureProForma(facture);
            if (factureReelleOpt.isPresent()) {
                FactureReelle factureReelle = factureReelleOpt.get();
                BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureReelle.getId());

                if (totalPaye != null && totalPaye.compareTo(BigDecimal.ZERO) > 0) {
                    throw new RuntimeException("Impossible d’annuler : des paiements ont déjà été effectués sur la facture.");
                }
            }


            facture.setStatut(StatutFactureProForma.ANNULE);
            facture.setDateAnnulation(LocalDateTime.now());
            facture.setUtilisateurAnnulateur(user);
            facture.setDateRelance(null);
            facture.setDernierRappelEnvoye(null);
            facture.setNotifie(false);

            factureReelleRepository.findByFactureProForma(facture).ifPresent(factureReelle -> {
                factureReelleRepository.delete(factureReelle);
                System.out.println("🗑️ Facture réelle supprimée.");
            });

            factProHistoriqueService.enregistrerActionHistorique(
                    facture,
                    user,
                    "Annulation",
                    "La facture a été annulée. La facture réelle associée a été supprimée."
            );

            return factureProformaRepository.save(facture);
        }

        // 🔁 Application des modifications normales
        facture.setUtilisateurModificateur(user);
        System.out.println("Modification effectuée par l'utilisateur ID: " + userId);
        System.out.println("Modifications reçues: " + modifications);

        // 💡 Génération de facture réelle si passage à VALIDE
        if (modifications.getStatut() == StatutFactureProForma.VALIDE && facture.getStatut() != StatutFactureProForma.VALIDE) {
            FactureReelle factureReelle = factureReelleService.genererFactureReelle(facture);
            System.out.println("✅ Facture Réelle générée avec succès : " + factureReelle.getNumeroFacture());

            factProHistoriqueService.enregistrerActionHistorique(
                    facture,
                    user,
                    "Validation",
                    "Facture validée définitivement. Facture réelle générée: " + factureReelle.getNumeroFacture()
            );
        }

        // ✅ Approbation de la facture
        if (modifications.getStatut() == StatutFactureProForma.APPROUVE) {
            boolean dejaApprouvee = facture.getDateApprobation() != null;

            facture.setDateAnnulation(null);
            facture.setUtilisateurAnnulateur(null);

            if (!dejaApprouvee) {
                boolean estModificateur = facture.getUtilisateurModificateur() != null &&
                        facture.getUtilisateurModificateur().getId().equals(user.getId());

                List<User> approbateurs = facture.getApprobateurs();
                boolean estApprobateur = approbateurs != null &&
                        approbateurs.stream().anyMatch(a -> a.getId().equals(user.getId()));

                if (!estApprobateur && !estModificateur) {
                    throw new RuntimeException("Vous n'êtes pas autorisé à approuver cette facture.");
                }

                if (!estApprobateur && estModificateur) {
                    if (approbateurs == null) {
                        approbateurs = new ArrayList<>();
                        facture.setApprobateurs(approbateurs);
                    }
                    approbateurs.add(user);
                }

                if (!estModificateur && facture.getStatut() != StatutFactureProForma.APPROBATION) {
                    throw new RuntimeException("La facture doit d'abord passer par le statut APPROBATION avant d'être APPROUVÉE.");
                }

                facture.setUtilisateurApprobateur(user);
                facture.setDateApprobation(LocalDateTime.now());

            } else {
                System.out.println("ℹ Facture déjà approuvée une fois. Appropriation directe autorisée.");
            }

            factProHistoriqueService.enregistrerActionHistorique(
                    facture,
                    user,
                    "Approbation",
                    "Facture approuvée par " + user.getNomComplet()
            );
        }

      
    if (modifications.getStatut() == StatutFactureProForma.BROUILLON
            && facture.getStatut() != StatutFactureProForma.BROUILLON
            && !(modifications.getNoteModification() != null
                && (modifications.getLignesFacture() == null || modifications.getLignesFacture().isEmpty())
                && modifications.getDescription() == null
                && modifications.getDateRelance() == null
                && modifications.getMethodeEnvoi() == null)) {

        facture.setStatut(StatutFactureProForma.BROUILLON);

        factProHistoriqueService.enregistrerActionHistorique(
                facture,
                user,
                "Retour au brouillon",
                "La facture est revenue au statut brouillon"
        );
    }


        /*
        // 💡 Bloc de retour automatique en brouillon (trop permissif)
        if (modifications.getStatut() == StatutFactureProForma.BROUILLON
                && !facture.getStatut().equals(StatutFactureProForma.BROUILLON)) {
            facture.setStatut(StatutFactureProForma.BROUILLON);
            factProHistoriqueService.enregistrerActionHistorique(
                    facture,
                    user,
                    "Retour au brouillon",
                    "La facture est revenue au statut brouillon"
            );
        }
         */

        // ✅ Ajout des approbateurs
        if (modifications.getStatut() == StatutFactureProForma.APPROBATION) {
            if (idsApprobateurs == null || idsApprobateurs.isEmpty()) {
                throw new RuntimeException("Vous devez fournir au moins un utilisateur pour approuver cette facture.");
            }

            List<User> approbateurs = usersRepository.findAllById(idsApprobateurs);
            if (approbateurs.size() != idsApprobateurs.size()) {
                throw new RuntimeException("Un ou plusieurs approbateurs sont introuvables !");
            }

            for (User approbateur : approbateurs) {
                if (!user.getEntreprise().getId().equals(approbateur.getEntreprise().getId())) {
                    throw new RuntimeException("Tous les approbateurs doivent appartenir à la même entreprise.");
                }
            }

            facture.setApprobateurs(approbateurs);
            System.out.println("👥 Approbateurs ajoutés : " + approbateurs.stream().map(User::getId).toList());

            factProHistoriqueService.enregistrerActionHistorique(
                    facture,
                    user,
                    "Demande Approbation",
                    "Demande d'approbation envoyée à : " +
                            approbateurs.stream().map(User::getNomComplet).collect(Collectors.joining(", "))
            );
        }

        // 🔁 Mise à jour de la date de relance
        if (modifications.getDateRelance() != null) {
            if (modifications.getDateRelance().isBefore(facture.getDateCreation().atStartOfDay())) {
                throw new RuntimeException("La date de relance ne peut pas être antérieure à la date de création de la facture !");
            }

            facture.setDernierRappelEnvoye(null);
            facture.setNotifie(false);
            facture.setDateRelance(modifications.getDateRelance());
        }

        // Réinitialisation de relance pour certains statuts
        if (modifications.getStatut() != null &&
                List.of(StatutFactureProForma.BROUILLON, StatutFactureProForma.APPROUVE, StatutFactureProForma.VALIDE).contains(modifications.getStatut())) {
            facture.setDateRelance(null);
            facture.setDernierRappelEnvoye(null);
            facture.setDateAnnulation(null);
            facture.setUtilisateurAnnulateur(null);
        }

        // 📩 Passage au statut ENVOYÉ
        if (modifications.getStatut() == StatutFactureProForma.ENVOYE) {
            if (modifications.getMethodeEnvoi() == null) {
                throw new IllegalArgumentException("Veuillez spécifier la méthode d’envoi : PHYSIQUE ou EMAIL.");
            }

            facture.setStatut(StatutFactureProForma.ENVOYE);
            facture.setMethodeEnvoi(modifications.getMethodeEnvoi());

            facture.setDateAnnulation(null);
            facture.setUtilisateurAnnulateur(null);

            if (facture.getDateRelance() == null) {
                facture.setDateRelance(LocalDateTime.now().plusHours(72));
            }

            facture.setUtilisateurRelanceur(facture.getUtilisateurModificateur());

            if (modifications.getMethodeEnvoi() == MethodeEnvoi.EMAIL) {
                log.info("📨 La facture {} est marquée ENVOYÉE par EMAIL. Le front doit appeler le service d'envoi de mail.", facture.getNumeroFacture());
            }

            factProHistoriqueService.enregistrerActionHistorique(
                    facture,
                    user,
                    "Envoi",
                    "Facture envoyée au client via " + facture.getMethodeEnvoi()
            );
        }

        // 🧾 Mise à jour des lignes de facture
        if (modifications.getLignesFacture() != null) {
            facture.getLignesFacture().clear();
            for (LigneFactureProforma ligne : modifications.getLignesFacture()) {
                Produit produit = produitRepository.findById(ligne.getProduit().getId())
                        .orElseThrow(() -> new RuntimeException("Produit introuvable !"));

                if (produit.getPrixVente() == null) {
                    throw new RuntimeException("Le prix de vente du produit avec l'ID " + produit.getId() + " est nul.");
                }

                ligne.setPrixUnitaire(produit.getPrixVente());
                ligne.setMontantTotal(ligne.getQuantite() * ligne.getPrixUnitaire());
                ligne.setFactureProForma(facture);
                ligne.setProduit(produit);
                ligne.setLigneDescription(Optional.ofNullable(ligne.getLigneDescription()).orElse(produit.getDescription()));

                facture.getLignesFacture().add(ligne);
            }
        }

        // ✏️ Description
        if (modifications.getDescription() != null) {
            facture.setDescription(modifications.getDescription());
        }

        // 💰 Calcul des totaux
        remisePourcentage = (remisePourcentage == null) ? 0.0 : remisePourcentage;
        if (remisePourcentage < 0 || remisePourcentage > 100) {
            throw new RuntimeException("Le pourcentage de remise doit être compris entre 0 et 100 !");
        }

        double montantTotalHT = facture.getLignesFacture().stream().mapToDouble(LigneFactureProforma::getMontantTotal).sum();
        double remiseMontant = montantTotalHT * (remisePourcentage / 100);
        boolean tvaActive = appliquerTVA != null && appliquerTVA;
        double montantTVA = tvaActive ? (montantTotalHT - remiseMontant) * 0.18 : 0;
        double montantTotalAPayer = (montantTotalHT - remiseMontant) + montantTVA;

        facture.setTotalHT(montantTotalHT);
        facture.setRemise(remiseMontant);
        facture.setTauxRemise(remisePourcentage);
        facture.setTva(tvaActive);
        facture.setTotalFacture(montantTotalAPayer);

        // ✅ Mise à jour du statut (hors VALIDÉ déjà traité)
//        if (modifications.getStatut() != null && facture.getStatut() != StatutFactureProForma.VALIDE) {
//            facture.setStatut(modifications.getStatut());
//        }

        if (modifications.getStatut() != null && facture.getStatut() != StatutFactureProForma.VALIDE) {
            // Ne change le statut que si ce n'est pas une facture APPROUVE, ou si le changement est explicite
            if (facture.getStatut() != StatutFactureProForma.APPROUVE || modifications.getStatut() == StatutFactureProForma.ANNULE) {
                facture.setStatut(modifications.getStatut());
            }
        }

        // 📝 Enregistrement de l'action "Modification" uniquement si le montant a changé
        if (montantTotalHT != ancienTotalHT) {
            factProHistoriqueService.enregistrerActionHistorique(
                    facture,
                    user,
                    "Modification",
                    "La facture a été modifiée (montant: " + montantTotalHT + ")"
            );
        }

        if (modifications.getNoteModification() != null && !modifications.getNoteModification().isBlank()) {
            NoteFactureProForma note = new NoteFactureProForma();
            note.setFacture(facture);
            note.setAuteur(user);
            note.setContenu(modifications.getNoteModification());
            note.setDateCreation(LocalDateTime.now());
            note.setNumeroIdentifiant(genererNumeroNotePourFacture(facture));


            noteFactureProFormaRepository.save(note);

            System.out.println("📝 Note ajoutée à la facture : " + modifications.getNoteModification());

        }


        return factureProformaRepository.save(facture);
    }
   
    //Methode pour recuperer les factures pro forma dune entreprise
    public List<Map<String, Object>> getFacturesParEntrepriseParUtilisateur(Long userId) {
        User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Utilisateur n'est associé à aucune entreprise");
        }
    
        Long entrepriseId = entreprise.getId();
        List<FactureProForma> factures;

        // S’il est ADMIN ou MANAGER, ou sil a la permisson il peut voir toutes les factures de son entreprise
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasGestionFacturePermission = user.getRole().hasPermission(PermissionType.Gestion_Facture);

        if (isAdminOrManager || hasGestionFacturePermission) {
            factures = factureProformaRepository.findByEntrepriseId(entrepriseId);
        } else {
            factures = factureProformaRepository.findByEntrepriseIdAndUtilisateur(userId, entrepriseId);
        }

        // Tri manuel par date de création décroissante
//        factures.sort(Comparator
//                .comparing(FactureProForma::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder()))
//                .thenComparing(FactureProForma::getId, Comparator.reverseOrder()));

        factures = factures.stream()
                .sorted((f1, f2) -> {
                    // Comparaison par date
                    int dateCompare = f2.getDateCreation().compareTo(f1.getDateCreation());
                    if (dateCompare != 0) return dateCompare;

                    // Si dates égales, comparer par ID
                    return f2.getId().compareTo(f1.getId());
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> factureMaps = new ArrayList<>();
    
        for (FactureProForma facture : factures) {
            Map<String, Object> factureMap = new HashMap<>();
    
            // Informations principales
            factureMap.put("id", facture.getId());
            factureMap.put("numeroFacture", facture.getNumeroFacture());
            factureMap.put("dateCreation", facture.getDateCreation());
            factureMap.put("description", facture.getDescription());
            factureMap.put("totalHT", facture.getTotalHT());
            factureMap.put("remise", facture.getRemise());
            factureMap.put("tva", facture.isTva());
            factureMap.put("totalFacture", facture.getTotalFacture());
            factureMap.put("statut", facture.getStatut());
            factureMap.put("ligneFactureProforma", facture.getLignesFacture());
    
            // Client
            factureMap.put("client", facture.getClient() != null ? facture.getClient().getNomComplet() : null);
    
            // Entreprise cliente
            factureMap.put("entrepriseClient", facture.getEntrepriseClient() != null
                ? facture.getEntrepriseClient().getNom()
                : null);
    
            // Entreprise émettrice
            factureMap.put("entreprise", facture.getEntreprise() != null
                ? facture.getEntreprise().getNomEntreprise()
                : null);
    
            // Relance et notification
            factureMap.put("dateRelance", facture.getDateRelance());
            factureMap.put("notifie", facture.isNotifie());
    
            // Ajout final
            factureMaps.add(factureMap);
        }
    
        return factureMaps;
    }

    // Methode pour recuperer une facture pro forma par son id
    public FactureProForma getFactureProformaById(Long id, HttpServletRequest request) {
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
    
        FactureProForma facture = factureProformaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture Proforma introuvable avec l'ID : " + id));
    
        if (!facture.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès refusé : Cette facture ne vous appartient pas !");
        }
    
        System.out.println("✅ Facture récupérée par l'utilisateur ID: " + userId);
        return facture;
    }
    
    //Methode pour modifier note d'une facture pro forma que user lui meme a creer
    @Transactional
    public FactureProForma modifierNoteFacture(Long factureId, Long noteId, String nouveauContenu, HttpServletRequest request) {
        // Récupération de la facture
        FactureProForma facture = factureProformaRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée !"));

        // Vérification du token JWT 
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

        // Récupération de la note à modifier
        NoteFactureProForma note = noteFactureProFormaRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note introuvable avec l'ID : " + noteId));

        // Vérification que la note appartient bien à la facture
        if (!note.getFacture().getId().equals(factureId)) {
            throw new RuntimeException("Cette note n'appartient pas à la facture spécifiée !");
        }

        // Vérification que l'utilisateur est le créateur de la note
        if (!note.getAuteur().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette note !");
        }

        // Mise à jour du contenu de la note
        note.setContenu(nouveauContenu);
        note.setDateDerniereModification(LocalDateTime.now());
        note.setModifiee(true);
        note.setAuteur(user);

        noteFactureProFormaRepository.save(note);
        return factureProformaRepository.save(facture);
    }

    //Generate
    private String genererNumeroNotePourFacture(FactureProForma facture) {
        int maxNumero = noteFactureProFormaRepository
            .findByFacture(facture).stream()
            .map(NoteFactureProForma::getNumeroIdentifiant)
            .filter(Objects::nonNull)
            .map(numero -> {
                try {
                    return Integer.parseInt(numero.replace("N ", ""));
                } catch (NumberFormatException e) {
                    return 0; // Si le format ne correspond pas
                }
            })
            .max(Integer::compareTo)
            .orElse(0);

        return "N " + (maxNumero + 1);
    }

  
   // Methode pour supprimer une note d'une facture pro forma que user lui meme a creer
   @Transactional
    public FactureProForma supprimerNoteFacture(Long factureId, Long noteId, HttpServletRequest request) {
        FactureProForma facture = factureProformaRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée !"));

        // Vérification du token JWT
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

        // Détermination du rôle
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;

        // Récupération de la note à supprimer
        NoteFactureProForma note = noteFactureProFormaRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note introuvable avec l'ID : " + noteId));

        // Vérification que la note appartient bien à la facture
        if (!note.getFacture().getId().equals(factureId)) {
            throw new RuntimeException("Cette note n'appartient pas à la facture spécifiée !");
        }

        // Vérification que l'utilisateur est le créateur de la note
        if (!note.getAuteur().getId().equals(user.getId()) && !isAdminOrManager) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette note !");
        }

        String numeroNote = note.getNumeroIdentifiant();

        // Suppression de la note
        noteFactureProFormaRepository.delete(note);

        // Enregistrement de l'historique de suppression
        factProHistoriqueService.enregistrerActionHistorique(
                facture,
                user,
                "Suppression Note",
                "La note " + numeroNote + " a été supprimée."
        );

        // Retourner la facture mise à jour
        return factureProformaRepository.save(facture);
    }

    //Methode get note dune facture by id
    public NoteFactureProForma getNotesByFactureId(Long factureId, Long noteId, HttpServletRequest request) {
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
        // Récupération de la facture
        FactureProForma facture = factureProformaRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'ID : " + factureId));
        // Vérification que l'utilisateur a accès à la facture
        if (!facture.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès refusé : Cette facture ne vous appartient pas !");
        }
        // Récupération de la note
        NoteFactureProForma note = noteFactureProFormaRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note introuvable avec l'ID : " + noteId));
        // Vérification que la note appartient à la facture
        if (!note.getFacture().getId().equals(factureId)) {
            throw new RuntimeException("Cette note n'appartient pas à la facture spécifiée !");
        }
        // Retourner la note
        return note;
    }

    //Methode pour connaitre tout les facture lier a un client
    public List<FactureProForma> getFacturesParClient(Long clientId, Long entrepriseClientId) {
    if (clientId == null && entrepriseClientId == null) {
        throw new RuntimeException("Veuillez spécifier un client ou une entreprise cliente.");
    }

    return factureProformaRepository.findByClientIdOrEntrepriseClientId(clientId, entrepriseClientId);
}

}