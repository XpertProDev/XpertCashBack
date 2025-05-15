package com.xpertcash.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.FactureReelleDTO;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.LigneFactureReelle;
import com.xpertcash.entity.StatutPaiementFacture;
import com.xpertcash.entity.User;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.LigneFactureReelleRepository;
import com.xpertcash.repository.UsersRepository;

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
    private JwtUtil jwtUtil;

    


    public FactureReelle genererFactureReelle(FactureProForma factureProForma) {
        FactureReelle factureReelle = new FactureReelle();
        
        // Copier les informations de la proforma
        factureReelle.setNumeroFacture(genererNumeroFactureReel());
        factureReelle.setDateCreation(LocalDate.now());
        factureReelle.setTotalHT(factureProForma.getTotalHT());
        factureReelle.setRemise(factureProForma.getRemise());
        factureReelle.setDescription(factureProForma.getDescription());

        factureReelle.setTva(factureProForma.isTva());
        factureReelle.setTotalFacture(factureProForma.getTotalFacture());
        factureReelle.setStatutPaiement(StatutPaiementFacture.EN_ATTENTE);

        factureReelle.setUtilisateurCreateur(factureProForma.getUtilisateurModificateur());
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
            ligneReelle.setMontantTotal(ligneProForma.getMontantTotal());
            ligneReelle.setFactureReelle(factureReelleSauvegardee); // Utiliser la facture sauvegardée
            return ligneReelle;
        }).collect(Collectors.toList());
    
        ligneFactureReelleRepository.saveAll(lignesFacture);
    
        return factureReelleSauvegardee;
    }

    
    private String genererNumeroFactureReel() {
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));
    
        List<FactureReelle> facturesDeLAnnee = factureReelleRepository.findFacturesDeLAnnee(year);
        int newIndex = 1;
    
        if (!facturesDeLAnnee.isEmpty()) {
            String lastNumeroFacture = facturesDeLAnnee.get(0).getNumeroFacture();
            // Exemple : "FACTURE N°005-04-2025"
            String[] parts = lastNumeroFacture.split("-");
            String numeroPart = parts[0].replace("FACTURE N°", "").trim();
            newIndex = Integer.parseInt(numeroPart) + 1;
        }
    
        return String.format("FACTURE N°%03d-%s", newIndex, formattedDate);
    }
    

  
    // Méthode pour modifier le statut de paiement d'une facture
  public FactureReelleDTO modifierStatutPaiement(Long factureId, StatutPaiementFacture nouveauStatut, HttpServletRequest request) {
    // Vérifier la présence du token JWT et récupérer l'ID de l'utilisateur connecté
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

    // Récupérer l'utilisateur par son ID
    User utilisateur = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

    // Récupérer la facture réelle à partir de son ID
    FactureReelle factureReelle = factureReelleRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture introuvable !"));

    // Vérifier que l'utilisateur appartient bien à l'entreprise de la facture
    if (!factureReelle.getEntreprise().getUtilisateurs().contains(utilisateur)) {
        throw new RuntimeException("Vous n'avez pas l'autorisation de modifier cette facture !");
    }

    // Vérifier que le statut n'est pas déjà celui demandé
    if (factureReelle.getStatutPaiement() == nouveauStatut) {
        throw new RuntimeException("Le statut est déjà défini sur " + nouveauStatut);
    }

    // Mettre à jour le statut de paiement
    factureReelle.setStatutPaiement(nouveauStatut);

    // Sauvegarder la facture modifiée
    factureReelleRepository.save(factureReelle);

    // Retourner uniquement les informations essentielles via le DTO
    return new FactureReelleDTO(factureReelle);
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

    List<FactureReelle> factures = factureReelleRepository.findByEntreprise(entreprise);

    return factures.stream()
            .map(FactureReelleDTO::new)
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

    List<FactureReelleDTO> factureDTOs = factures.stream()
            .map(FactureReelleDTO::new)
            .collect(Collectors.toList());

    if (factureDTOs.isEmpty()) {
        return ResponseEntity.ok("Aucune facture trouvée.");
    }

    return ResponseEntity.ok(factureDTOs);
}

    // Methode Get facture reel by id
    public FactureReelleDTO getFactureReelleById(Long factureId, HttpServletRequest request) {
        // Extraire le token JWT
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
    
        // Récupérer l'utilisateur et son entreprise
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        Long entrepriseId = user.getEntreprise().getId();
    
        // Récupérer la facture
        FactureReelle facture = factureReelleRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Aucune facture trouvée"));
    
        // Vérifier que la facture appartient bien à l'entreprise de l'utilisateur
        if (!facture.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Accès refusé : cette facture ne vous appartient pas !");
        }
    
        return new FactureReelleDTO(facture);
    }
    
    

        // Methode pour Supprimer facturer deja generer une fois annuler

    public void supprimerFactureReelleLiee(FactureProForma proforma) {
        Optional<FactureReelle> factureReelleOpt = factureReelleRepository.findByFactureProForma(proforma);
        if (factureReelleOpt.isPresent()) {
            factureReelleRepository.delete(factureReelleOpt.get());
            System.out.println("🗑️ Facture réelle supprimée suite à l'annulation.");
        } else {
            System.out.println("ℹ️ Aucune facture réelle associée à cette facture proforma.");
        }
    }
}
