package com.xpertcash.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Client;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.LigneFactureProforma;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.StatutFactureProForma;
import com.xpertcash.entity.User;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class FactureProformaService {

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
    private UsersRepository usersRepository;

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
    facture.setNumeroFacture(generateNumeroFacture());

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

    double montantTotalHT = 0;
    if (facture.getLignesFacture() != null) {
        for (LigneFactureProforma ligne : facture.getLignesFacture()) {
            Produit produit = produitRepository.findById(ligne.getProduit().getId())
                    .orElseThrow(() -> new RuntimeException("Produit avec ID " + ligne.getProduit().getId() + " introuvable !"));

            ligne.setFactureProForma(facture);
            ligne.setProduit(produit);
            ligne.setPrixUnitaire(produit.getPrixVente());

            ligne.setMontantTotal(ligne.getQuantite() * ligne.getPrixUnitaire());

            // Ajout au montant total HT
            montantTotalHT += ligne.getMontantTotal();
        }
    }

    // Calcul de la remise
    double remiseMontant = (remisePourcentage > 0) ? montantTotalHT * (remisePourcentage / 100) : 0;

    // Appliquer la TVA uniquement si elle est activée
    boolean tvaActive = (appliquerTVA != null && appliquerTVA) || facture.isTva();
    double montantTVA = tvaActive ? (montantTotalHT - remiseMontant) * 0.18 : 0;

    // Calcul du montant total à payer
    double montantTotalAPayer = (montantTotalHT - remiseMontant) + montantTVA;

    // Assigner les montants calculés à la facture
    facture.setTotalHT(montantTotalHT);
    facture.setRemise(remiseMontant);  // Remise en montant
    facture.setTva(tvaActive);
    facture.setTotalFacture(montantTotalAPayer);

    return factureProformaRepository.save(facture);
}

    
    // Méthode pour générer un numéro de facture unique
        private String generateNumeroFacture() {
            // la date actuelle
            LocalDate currentDate = LocalDate.now();
            String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));

            Optional<FactureProForma> lastFacture = factureProformaRepository.findTopByDateCreationOrderByNumeroFactureDesc(currentDate);

            int newIndex = 1;
            if (lastFacture.isPresent()) {
                String lastNumeroFacture = lastFacture.get().getNumeroFacture();
                // Assumer que le format est "FACTURE PROFORMA N°XXX-dd-MM-yyyy"
                String[] parts = lastNumeroFacture.split("-");
                newIndex = Integer.parseInt(parts[0].replace("FACTURE PROFORMA N°", "")) + 1;
            }

            return String.format("FACTURE PROFORMA N°%03d-%s", newIndex, formattedDate);
        }


    // Méthode pour modifier une facture pro forma
            @Transactional
            public FactureProForma modifierFacture(Long factureId, Double remisePourcentage, Boolean appliquerTVA, FactureProForma modifications, HttpServletRequest request) {
                FactureProForma facture = factureProformaRepository.findById(factureId)
                        .orElseThrow(() -> new RuntimeException("Facture non trouvée !"));
            
                // Extraire l'utilisateur connecté à partir du token JWT
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

            // Ajouter l'ID de l'utilisateur dans la facture pour savoir qui a effectué la modification
            facture.setUtilisateurModificateur(user);

            System.out.println("Modification effectuée par l'utilisateur ID: " + userId);
            
                // Ajouter un log pour voir ce que vous recevez comme modifications
                System.out.println("Modifications reçues: " + modifications);
            
            
            
                // Vérifier si la facture est VALIDÉE
                if (facture.getStatut() == StatutFactureProForma.VALIDE) {
                
            
                    // Si une tentative de modification autre que le statut de paiement est effectuée, on lève une exception
                    if (modifications.getLignesFacture() != null || 
                        remisePourcentage != null || 
                        appliquerTVA != null || 
                        modifications.getStatut() != null) {
                        
                        throw new RuntimeException("Impossible de modifier une facture VALIDÉE, sauf son statut de paiement !");
                    }
            
                    //return facture; // Retourner la facture sans aucune modification si c'est une facture VALIDÉE
                }

                // Si la facture passe en "VALIDE", générer une facture réelle
                if (modifications.getStatut() == StatutFactureProForma.VALIDE && facture.getStatut() != StatutFactureProForma.VALIDE) {
                    FactureReelle factureReelle = factureReelleService.genererFactureReelle(facture);
                    System.out.println("✅ Facture Réelle générée avec succès : " + factureReelle.getNumeroFacture());
                }


                    // Vérifier si la date de relance est modifiée
                    if (modifications.getDateRelance() != null && !modifications.getDateRelance().equals(facture.getDateRelance())) {
                        LocalDateTime dateCreationAsDateTime = facture.getDateCreation().atStartOfDay();
                        // Vérifier que la date de relance n'est pas antérieure à la date de création
                        if (modifications.getDateRelance().isBefore(dateCreationAsDateTime)) {
                            throw new RuntimeException("La date de relance ne peut pas être antérieure à la date de création de la facture !");
                        }

                        System.out.println("🚀 La date de relance a été modifiée. Réinitialisation des champs...");
                        facture.setDernierRappelEnvoye(null);
                        facture.setNotifie(false);
                    }

                    // Mettre à jour la date de relance
                    if (modifications.getDateRelance() != null) {
                        facture.setDateRelance(modifications.getDateRelance());
                    }


                // Si le statut est modifié et passe à "BROUILLON", "APPROUVE" ou "VALIDE", réinitialiser les champs dateRelance et dernierRappelEnvoye
                    if (modifications.getStatut() != null && 
                        (modifications.getStatut() == StatutFactureProForma.BROUILLON ||
                        modifications.getStatut() == StatutFactureProForma.APPROUVE ||
                        modifications.getStatut() == StatutFactureProForma.VALIDE)) {
                        facture.setDateRelance(null);
                        facture.setDernierRappelEnvoye(null);
                    }

                    // Si le statut est modifié et passe à "BROUILLON", "APPROUVE" ou "ENVOYE", réinitialiser le statutPaiement
                    if (modifications.getStatut() != null && 
                    (modifications.getStatut() == StatutFactureProForma.BROUILLON ||
                    modifications.getStatut() == StatutFactureProForma.APPROUVE ||
                    modifications.getStatut() == StatutFactureProForma.ENVOYE)) {
                    
                }


                // Vérifier si on passe en "ENVOYÉ" et définir la date de relance
                if (modifications.getStatut() != null && modifications.getStatut() == StatutFactureProForma.ENVOYE) {
                    if (facture.getDateRelance() == null) {
                        //facture.setDateRelance(LocalDateTime.now().plusHours(72)); // Par défaut 72h
                        // Exemple pour une relance dans une minute
                        facture.setDateRelance(LocalDateTime.now().plusMinutes(1));
                    }

                    // Enregistrer l'utilisateur qui a mis la facture en ENVOYÉ
                    facture.setUtilisateurRelanceur(facture.getUtilisateurModificateur());
                }

            
            // Si la facture n'est pas VALIDÉE, on applique les autres modifications
        if (modifications.getLignesFacture() != null) {
            facture.getLignesFacture().clear();
            for (LigneFactureProforma ligne : modifications.getLignesFacture()) {
                // Récupérer le produit à partir de son ID
                Produit produit = produitRepository.findById(ligne.getProduit().getId())
                        .orElseThrow(() -> new RuntimeException("Produit introuvable !"));

                System.out.println("Produit ID: " + produit.getId() + " - Prix de vente: " + produit.getPrixVente());

                if (produit.getPrixVente() == null) {
                    throw new RuntimeException("Le prix de vente du produit avec l'ID " + produit.getId() + " est nul.");
                }

                // Mettre à jour le prix unitaire de la ligne avec le prix du produit
                ligne.setPrixUnitaire(produit.getPrixVente());

                // Calcul du montant total pour cette ligne (quantité * prix unitaire)
                double montantTotal = ligne.getQuantite() * ligne.getPrixUnitaire();
                System.out.println("Montant total pour la ligne: " + montantTotal);

                // Mettre à jour la ligne de facture avec le montant calculé
                ligne.setFactureProForma(facture);
                ligne.setProduit(produit);
                ligne.setMontantTotal(montantTotal);

                // Ajouter la ligne de facture modifiée à la facture
                facture.getLignesFacture().add(ligne);
            }
        }
            
                // Vérifier et appliquer la remise
                remisePourcentage = (remisePourcentage == null) ? 0.0 : remisePourcentage;
                if (remisePourcentage < 0 || remisePourcentage > 100) {
                    throw new RuntimeException("Le pourcentage de remise doit être compris entre 0 et 100 !");
                }
            
                // Calcul du montant total HT
                double montantTotalHT = facture.getLignesFacture().stream()
                        .mapToDouble(LigneFactureProforma::getMontantTotal)
                        .sum();
            
                double remiseMontant = montantTotalHT * (remisePourcentage / 100);
                boolean tvaActive = (appliquerTVA != null && appliquerTVA);
                double montantTVA = tvaActive ? (montantTotalHT - remiseMontant) * 0.18 : 0;
                double montantTotalAPayer = (montantTotalHT - remiseMontant) + montantTVA;
            
                // Mise à jour des valeurs calculées
                facture.setTotalHT(montantTotalHT);
                facture.setRemise(remiseMontant);
                facture.setTva(tvaActive);
                facture.setTotalFacture(montantTotalAPayer);
            
                // Mise à jour du statut si fourni (sauf si la facture est VALIDÉE ou déjà ENCAISSÉ)
                if (modifications.getStatut() != null && facture.getStatut() != StatutFactureProForma.VALIDE) {
                    facture.setStatut(modifications.getStatut());
                }
            
                return factureProformaRepository.save(facture);
            }

    

    //Methode pour recuperer les factures pro forma dune entreprise
    public List<Map<String, Object>> getFacturesParEntreprise(Long userId) {
    // Récupérer les factures de l'entreprise de l'utilisateur
    List<FactureProForma> factures = factureProformaRepository.findByEntrepriseId(userId);

    List<Map<String, Object>> factureMaps = new ArrayList<>();

    for (FactureProForma facture : factures) {
        Map<String, Object> factureMap = new HashMap<>();
        
        // Ajouter les informations de la facture
        factureMap.put("id", facture.getId());
        factureMap.put("numeroFacture", facture.getNumeroFacture());
        factureMap.put("dateCreation", facture.getDateCreation());
        factureMap.put("description", facture.getDescription());
        factureMap.put("totalHT", facture.getTotalHT());
        factureMap.put("remise", facture.getRemise());
        factureMap.put("tva", facture.isTva());
        factureMap.put("totalFacture", facture.getTotalFacture());
        factureMap.put("statut", facture.getStatut());

        // Vérifier si le client est associé à la facture
        if (facture.getClient() != null) {
            factureMap.put("client", facture.getClient().getNomComplet());
        } else {
            factureMap.put("client", null); 
        }

        // Vérifier si l'entreprise client est associée à la facture
        if (facture.getEntrepriseClient() != null) {
            factureMap.put("entrepriseClient", facture.getEntrepriseClient().getNom());
        } else {
            factureMap.put("entrepriseClient", null);
        }

        // Ajouter les informations de l'entreprise (s'il y en a une)
        factureMap.put("entreprise", facture.getEntreprise() != null ? facture.getEntreprise().getNomEntreprise() : null);
        
        // Ajouter d'autres informations pertinentes comme la date de relance et le statut de notification
        factureMap.put("dateRelance", facture.getDateRelance());
        factureMap.put("notifie", facture.isNotifie());

        factureMaps.add(factureMap);
    }

    return factureMaps;
}

    
}
 