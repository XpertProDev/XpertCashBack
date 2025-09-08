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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.EntrepriseClientDTO;
import com.xpertcash.DTOs.FactureProFormaDTO;
import com.xpertcash.DTOs.FactureProformaPaginatedResponseDTO;
import com.xpertcash.DTOs.LigneFactureDTO;
import com.xpertcash.DTOs.CLIENT.ClientDTO;
import com.xpertcash.configuration.CentralAccess;

import com.xpertcash.entity.Client;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.LigneFactureProforma;
import com.xpertcash.entity.MethodeEnvoi;
import com.xpertcash.entity.NoteFactureProForma;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.StatutFactureProForma;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactProHistoriqueActionRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.LigneFactureProformaRepository;
import com.xpertcash.repository.NoteFactureProFormaRepository;
import com.xpertcash.repository.PaiementRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.Module.ModuleActivationService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.xpertcash.service.AuthenticationHelper;

@Service
public class FactureProformaService {

    @Autowired
    private AuthenticationHelper authHelper;

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
    private LigneFactureProformaRepository ligneFactureProformaRepository;

    @Autowired 
    private FactProHistoriqueActionRepository factProHistoriqueActionRepository;





    @Autowired
    private ModuleActivationService moduleActivationService;

    @Autowired
    private GlobalNotificationService globalNotificationService;
//    private NotificationService notificationService;
    
    // Methode pour creer une facture pro forma
    public FactureProForma ajouterFacture(FactureProForma facture, Double remisePourcentage, Boolean appliquerTVA, HttpServletRequest request) {
    if (facture == null) {
        throw new RuntimeException("La facture ne peut pas être vide !");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    // 🏢 Vérifier que l'utilisateur est bien associé à une entreprise
    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    // 🔐 Vérification des droits d'accès
    boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseUtilisateur.getId());
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    if (!isAdmin && !hasPermission) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour créer une facture dans cette entreprise !");
    }


    // 🔒 Vérification d'accès au module Gestion Facturation
    moduleActivationService.verifierAccesModulePourEntreprise(entrepriseUtilisateur, "GESTION_FACTURATION");

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
    facture.setDateCreation(LocalDateTime.now());

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

                // Traitement du prix unitaire comme la ligneDescription
                if (ligne.getPrixUnitaire() != null) {
                    // Si un prix unitaire est fourni, l'utiliser directement
                    ligne.setPrixUnitaire(ligne.getPrixUnitaire());
                } else {
                    // Sinon, utiliser le prix du produit
                    ligne.setPrixUnitaire(produit.getPrixVente());
                }

                // Pour les produits de type SERVICE, mettre à jour le prix global du produit
                if ("SERVICE".equals(produit.getTypeProduit()) &&
                        ligne.getPrixUnitaire() != null &&
                        !ligne.getPrixUnitaire().equals(produit.getPrixVente())) {

                    // Mettre à jour le prix du produit global
                    produit.setPrixVente(ligne.getPrixUnitaire());
                    produit.setLastUpdated(LocalDateTime.now());
                    produitRepository.save(produit);
                }

                ligne.setMontantTotal(ligne.getQuantite() * ligne.getPrixUnitaire());

                // Traitement de la description (comme avant)
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
    public FactureProFormaDTO modifierFacture(Long factureId, Double remisePourcentage, Boolean appliquerTVA, FactureProForma modifications, List<Long> idsApprobateurs, HttpServletRequest request) {
        // 🔐 Récupération de la facture
        FactureProForma facture = factureProformaRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée !"));

        // Stocker l'ancien montant total HT avant toute modification
        double ancienTotalHT = facture.getTotalHT();

        User user = authHelper.getAuthenticatedUserWithFallback(request);

         // --- Vérification que la facture appartient à la même entreprise que l'utilisateur ---
        Entreprise entrepriseFacture = facture.getEntreprise();
        Entreprise entrepriseUtilisateur = user.getEntreprise();

        if (entrepriseFacture == null || entrepriseUtilisateur == null || !entrepriseFacture.getId().equals(entrepriseUtilisateur.getId())) {
            throw new RuntimeException("Accès refusé : vous ne pouvez modifier que les factures de votre entreprise.");
        }

        // --- Optionnel : Vérification des droits via CentralAccess et permission ---
        // 🔐 Vérification des droits d'accès
        boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseUtilisateur.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

        if (!isAdmin && !hasPermission) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour créer une facture dans cette entreprise !");
        }


        // 🔒 Blocage total si facture annulée
        if (facture.getStatut() == StatutFactureProForma.ANNULE) {
            throw new RuntimeException("Cette facture est annulée. Elle ne peut plus être modifiée.");
        }

        // Si l'utilisateur tente de revalider une facture déjà validée
        // if (modifications.getStatut() == StatutFactureProForma.VALIDE) {
        //     throw new RuntimeException("Cette facture est déjà VALIDÉE. Vous ne pouvez pas la valider une seconde fois.");
        // }

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

            return new FactureProFormaDTO(facture);
        }

        // 🔁 Application des modifications normales
        facture.setUtilisateurModificateur(user);
        System.out.println("Modification effectuée par l'utilisateur ID: " + user.getId());
        System.out.println("Modifications reçues: " + modifications);

        // 💡 Génération de facture réelle si passage à VALIDE
        if (modifications.getStatut() == StatutFactureProForma.VALIDE && facture.getStatut() != StatutFactureProForma.VALIDE) {
            // On récupère et enregistre le validateur
            facture.setUtilisateurValidateur(user);

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
            // && !(modifications.getNoteModification() != null
                && (modifications.getLignesFacture() == null || modifications.getLignesFacture().isEmpty())
                && modifications.getDescription() == null
                && modifications.getDateRelance() == null
                && modifications.getMethodeEnvoi() == null) {

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
        // === Ajout des approbateurs et notifications ===
        if (modifications.getStatut() == StatutFactureProForma.APPROBATION) {
            if (idsApprobateurs == null || idsApprobateurs.isEmpty()) {
                throw new RuntimeException("Vous devez fournir au moins un utilisateur pour approuver cette facture.");
            }
            List<User> approbateurs = usersRepository.findAllById(idsApprobateurs);
            if (approbateurs.size() != idsApprobateurs.size()) {
                throw new RuntimeException("Un ou plusieurs approbateurs sont introuvables !");
            }

            // Persistance des approbateurs sur la facture
            facture.setApprobateurs(approbateurs);
            System.out.println("👥 Approbateurs ajoutés : " + approbateurs.stream()
                    .map(User::getId).toList());

            // Construction des messages
            String numero = Optional.ofNullable(facture.getNumeroFacture())
                    .filter(s -> !s.isBlank())
                    .orElseGet(() -> "Facture #" + facture.getId());
            String createur = Optional.ofNullable(user.getNomComplet())
                    .filter(s -> !s.isBlank())
                    .orElse("un utilisateur");

            String msgAppro = String.format(
                    "Nouvelle facture '%s' à approuver, créée par %s.",
                    Optional.ofNullable(numero).orElse("N/A"),
                    Optional.ofNullable(createur).orElse("un utilisateur")
            );
            String destinataires = approbateurs.stream()
                    .map(u -> u.getNomComplet() != null ? u.getNomComplet() : "(nom inconnu)")
                    .collect(Collectors.joining(", "));
            String msgSender = String.format(
                    "Vous avez envoyé une demande d'approbation pour la facture '%s' à: %s.",
                    numero,
                    destinataires
            );

            // Notifications
            globalNotificationService.notifyRecipients(approbateurs, msgAppro);
            globalNotificationService.notifySingle(user, msgSender);

            // Historique
            factProHistoriqueService.enregistrerActionHistorique(
                    facture,
                    user,
                    "Demande Approbation",
                    "Demande envoyée à: " + destinataires
            );
        }
        
        // 🔁 Mise à jour de la date de relance
        if (modifications.getDateRelance() != null) {
            if (modifications.getDateRelance().isBefore(facture.getDateCreation())) {
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
                throw new IllegalArgumentException("Veuillez spécifier la méthode d’envoi : PHYSIQUE, EMAIL ou AUTRE.");
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

            String details = "Facture envoyée au client via " + facture.getMethodeEnvoi();
              if (facture.getMethodeEnvoi() == MethodeEnvoi.AUTRE) {
                    details += " : " + modifications.getJustification();
                }

                        /*
                            if (facture.getDateRelance() != null) {
                            details += " | Date de relance prévue : " + facture.getDateRelance();
                        }

                        */
                factProHistoriqueService.enregistrerActionHistorique(
                    facture,
                    user,
                    "Envoi",
                    details
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


        return new FactureProFormaDTO(facture);
    }

    //Supression dune facture proforma en brouillon
     @Transactional
    public void supprimerFactureProforma(Long factureId, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        FactureProForma facture = factureProformaRepository.findById(factureId)
                .orElseThrow(() -> new EntityNotFoundException("Facture introuvable avec l'ID : " + factureId));

        if (facture.getStatut() != StatutFactureProForma.BROUILLON) {
            throw new RuntimeException("Seules les factures en statut BROUILLON peuvent être supprimées.");
        }

        if (!facture.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Cette facture ne vous appartient pas.");
        }

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour supprimer une facture.");
        }

        // 🔥 Supprimer d'abord les lignes de facture
        ligneFactureProformaRepository.deleteByFactureProForma(facture);

        // 🔥 Supprimer les historiques liés à la facture
        factProHistoriqueActionRepository.deleteByFacture(facture);

        // Suprimer les note
        noteFactureProFormaRepository.deleteByFacture(facture);

        // ✅ Ensuite on peut supprimer la facture
        factureProformaRepository.delete(facture);
    }


   

    //Methode pour recuperer les factures pro forma dune entreprise
    @Transactional
    public List<Map<String, Object>> getFacturesParEntrepriseParUtilisateur(Long userIdRequete, HttpServletRequest request) {
        return getFacturesParEntrepriseParUtilisateurPaginated(userIdRequete, 0, Integer.MAX_VALUE, request).getContent();
    }

    // Méthode scalable avec pagination pour récupérer les factures proforma d'une entreprise
    @Transactional
    public FactureProformaPaginatedResponseDTO getFacturesParEntrepriseParUtilisateurPaginated(
            Long userIdRequete, 
            int page, 
            int size, 
            HttpServletRequest request) {
        
        // --- 1. Validation des paramètres de pagination ---
        if (page < 0) page = 0;
        if (size <= 0) size = 20; // Taille par défaut
        if (size > 100) size = 100; // Limite maximale pour éviter la surcharge
        
        // --- 2. Récupération et validation de l'utilisateur ---
        User currentUser = authHelper.getAuthenticatedUserWithFallback(request);
        User targetUser = usersRepository.findById(userIdRequete)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

        Entreprise entrepriseCourante = currentUser.getEntreprise();
        Entreprise entrepriseCible = targetUser.getEntreprise();

        if (entrepriseCourante == null || entrepriseCible == null
            || !entrepriseCourante.getId().equals(entrepriseCible.getId())) {
            throw new RuntimeException("Opération interdite : utilisateurs de différentes entreprises.");
        }

        // --- 3. Vérification des droits d'accès ---
        boolean isAdmin = currentUser.getRole().getName() == RoleType.ADMIN;
        boolean isManager = currentUser.getRole().getName() == RoleType.MANAGER;
        boolean hasPermission = currentUser.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
        boolean isApprover = factureProformaRepository.existsByApprobateursAndEntrepriseId(currentUser, entrepriseCourante.getId());

        // --- 4. Créer le Pageable avec tri optimisé ---
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending().and(Sort.by("id").descending()));

        // --- 5. Récupérer les factures avec pagination selon les droits ---
        Page<FactureProForma> facturesPage;
        
        if (isAdmin || isManager) {
            // Admins et managers voient toutes les factures de l'entreprise
            facturesPage = factureProformaRepository.findFacturesAvecRelationsParEntreprisePaginated(
                    entrepriseCourante.getId(), pageable);
        } else if (hasPermission || isApprover) {
            // Utilisateurs avec permissions voient leurs factures + celles où ils sont approbateurs
            facturesPage = factureProformaRepository.findFacturesAvecRelationsParEntrepriseEtUtilisateurPaginated(
                    entrepriseCourante.getId(), currentUser.getId(), pageable);
        } else {
            // Utilisateurs normaux ne voient que leurs propres factures
            if (!Objects.equals(currentUser.getId(), userIdRequete)) {
                throw new RuntimeException("Vous ne pouvez voir que vos propres factures.");
            }
            facturesPage = factureProformaRepository.findFacturesAvecRelationsParEntrepriseEtUtilisateurPaginated(
                    entrepriseCourante.getId(), currentUser.getId(), pageable);
        }

        // --- 6. Récupérer les statistiques globales (une seule fois) ---
        long totalFactures = factureProformaRepository.countFacturesByEntrepriseId(entrepriseCourante.getId());
        long totalFacturesBrouillon = factureProformaRepository.countFacturesByEntrepriseIdAndStatut(
                entrepriseCourante.getId(), StatutFactureProForma.BROUILLON);
        long totalFacturesEnAttente = factureProformaRepository.countFacturesByEntrepriseIdAndStatut(
                entrepriseCourante.getId(), StatutFactureProForma.APPROBATION);
        long totalFacturesValidees = factureProformaRepository.countFacturesByEntrepriseIdAndStatut(
                entrepriseCourante.getId(), StatutFactureProForma.VALIDE);
        long totalFacturesAnnulees = factureProformaRepository.countFacturesByEntrepriseIdAndStatut(
                entrepriseCourante.getId(), StatutFactureProForma.ANNULE);

        // --- 7. Transformer les factures de la page courante en Map ---
        List<Map<String, Object>> facturesMap = facturesPage.getContent().stream()
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
                    map.put("ligneFactureProforma", facture.getLignesFacture() != null ? facture.getLignesFacture() : Collections.emptyList());
                    map.put("client", facture.getClient() != null ? facture.getClient().getNomComplet() : null);
                    map.put("entrepriseClient", facture.getEntrepriseClient() != null ? facture.getEntrepriseClient().getNom() : null);
                    map.put("entreprise", facture.getEntreprise() != null ? facture.getEntreprise().getNomEntreprise() : null);
                    map.put("dateRelance", facture.getDateRelance());
                    map.put("notifie", facture.isNotifie());
                    return map;
                })
                .collect(Collectors.toList());

        // --- 8. Créer la page de DTOs ---
        Page<Map<String, Object>> dtoPage = new PageImpl<>(
                facturesMap,
                pageable,
                facturesPage.getTotalElements()
        );

        // --- 9. Retourner la réponse paginée ---
        return FactureProformaPaginatedResponseDTO.fromPage(dtoPage, totalFactures, totalFacturesBrouillon, 
                totalFacturesEnAttente, totalFacturesValidees, totalFacturesAnnulees);
    }



//Trie:
   
    // Methode pour recuperer une facture pro forma par son id
    // Méthode privée pour récupérer l'entité FactureProForma avec contrôle d'accès
    public FactureProForma getFactureProformaEntityById(Long id, HttpServletRequest request) {
        User utilisateur = authHelper.getAuthenticatedUserWithFallback(request);

        FactureProForma facture = factureProformaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Facture Proforma introuvable avec l'ID : " + id));

        Entreprise entrepriseUtilisateur = utilisateur.getEntreprise();
        Entreprise entrepriseFacture = facture.getEntreprise();

        if (entrepriseUtilisateur == null || entrepriseFacture == null ||
            !entrepriseUtilisateur.getId().equals(entrepriseFacture.getId())) {
            throw new RuntimeException("Accès refusé : cette facture ne vous appartient pas.");
        }

        boolean isAdmin = utilisateur.getRole().getName() == RoleType.ADMIN;
        boolean hasPermission = utilisateur.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
        boolean isCreateurFacture = facture.getUtilisateurCreateur() != null &&
                                    facture.getUtilisateurCreateur().getId().equals(utilisateur.getId());

        if (!(isAdmin || hasPermission || isCreateurFacture)) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour consulter cette facture.");
        }

        return facture;
    }

// Méthode publique pour récupérer le DTO (utilisée par l'API)
public FactureProFormaDTO getFactureProformaById(Long id, HttpServletRequest request) {
    FactureProForma facture = getFactureProformaEntityById(id, request);
    return new FactureProFormaDTO(facture);
}

    //Methode pour modifier note d'une facture pro forma que user lui meme a creer
        @Transactional
        public FactureProFormaDTO modifierNoteFacture(Long factureId, Long noteId, String nouveauContenu, HttpServletRequest request) {
            // Récupération de la facture
            FactureProForma facture = factureProformaRepository.findById(factureId)
                    .orElseThrow(() -> new RuntimeException("Facture non trouvée !"));

            User user = authHelper.getAuthenticatedUserWithFallback(request);

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
            return new FactureProFormaDTO(facture);
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
    public FactureProFormaDTO supprimerNoteFacture(Long factureId, Long noteId, HttpServletRequest request) {
        FactureProForma facture = factureProformaRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée !"));

        User user = authHelper.getAuthenticatedUserWithFallback(request);

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
        // return factureProformaRepository.save(facture);
        return new FactureProFormaDTO(facture);

    }

    //Methode get note dune facture by id
    public NoteFactureProForma getNotesByFactureId(Long factureId, Long noteId, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
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

 
    //Trier
public List<FactureProFormaDTO> getFacturesParPeriode(Long userIdRequete, HttpServletRequest request,
                                                      String typePeriode, LocalDate dateDebut, LocalDate dateFin) {
    User currentUser = authHelper.getAuthenticatedUserWithFallback(request);
    User targetUser = usersRepository.findById(userIdRequete)
            .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

    Entreprise entrepriseCourante = currentUser.getEntreprise();
    Entreprise entrepriseCible = targetUser.getEntreprise();

    if (entrepriseCourante == null || entrepriseCible == null
        || !entrepriseCourante.getId().equals(entrepriseCible.getId())) {
        throw new RuntimeException("Opération interdite : utilisateurs de différentes entreprises.");
    }

    boolean isAdmin = currentUser.getRole().getName() == RoleType.ADMIN;
    boolean isManager = currentUser.getRole().getName() == RoleType.MANAGER;
    boolean hasPermission = currentUser.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    // 🔹 Calcul période
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
            if (dateDebut == null || dateFin == null) throw new RuntimeException("Dates de début et de fin requises.");
            dateStart = dateDebut.atStartOfDay();
            dateEnd = dateFin.plusDays(1).atStartOfDay();
            break;
        default:
            throw new RuntimeException("Type de période invalide.");
    }

    // 🔹 Récupérer toutes les factures de l’entreprise dans la période avec les relations nécessaires
    List<FactureProForma> factures = factureProformaRepository
            .findFacturesAvecRelationsParEntrepriseEtPeriode(entrepriseCourante.getId(), dateStart, dateEnd);

    // 🔹 Filtrage selon les rôles et permissions
            if (!(isAdmin || isManager)) {
            if (hasPermission) {
                factures = factures.stream()
                        .filter(f -> f.getUtilisateurCreateur().getId().equals(currentUser.getId())
                               || f.getApprobateurs().contains(currentUser))
                        .collect(Collectors.toList());
            } else {
                factures = factures.stream()
                        .filter(f -> f.getUtilisateurCreateur().getId().equals(currentUser.getId()))
                        .collect(Collectors.toList());
            }
        }

    // 🔹 Transformation en DTO et tri
    return factures.stream()
            .sorted(Comparator.comparing(FactureProForma::getDateCreation).reversed())
            .map(f -> {
                FactureProFormaDTO dto = new FactureProFormaDTO();
                dto.setId(f.getId());
                dto.setNumeroFacture(f.getNumeroFacture());
                dto.setDateCreation(f.getDateCreation());
                dto.setDescription(f.getDescription());
                dto.setTotalHT(f.getTotalHT());
                dto.setRemise(f.getRemise());
                dto.setTva(f.isTva());
                dto.setTotalFacture(f.getTotalFacture());
                dto.setStatut(f.getStatut());
                dto.setLigneFactureProforma(f.getLignesFacture().stream()
                        .map(LigneFactureDTO::new)
                        .collect(Collectors.toList()));
                dto.setClient(f.getClient() != null ? new ClientDTO(f.getClient()) : null);
                dto.setEntrepriseClient(f.getEntrepriseClient() != null ? new EntrepriseClientDTO(f.getEntrepriseClient()) : null);
                dto.setDateRelance(f.getDateRelance());
                dto.setNotifie(f.isNotifie());
                return dto;
            })
            .collect(Collectors.toList());
}

}