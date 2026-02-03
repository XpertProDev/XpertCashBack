package com.xpertcash.service.VENTE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.VENTE.FactureVenteResponseDTO;
import com.xpertcash.DTOs.VENTE.FactureVentePaginatedDTO;
import com.xpertcash.DTOs.VENTE.ProduitFactureResponse;
import com.xpertcash.DTOs.VENTE.ReceiptEmailRequest;
import com.xpertcash.DTOs.VENTE.VenteLigneResponse;
import com.xpertcash.DTOs.VENTE.StatistiquesVenteGlobalesDTO;
import com.xpertcash.DTOs.VENTE.TopProduitVenduDTO;
import com.xpertcash.DTOs.VENTE.VendeurFactureDTO;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.FactureVente;
import com.xpertcash.entity.User;
import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.FactureVenteRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.VENTE.VenteProduitRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class FactureVenteService {

    @Autowired
    private FactureVenteRepository factureVenteRepository;
    @Autowired
    private  UsersRepository usersRepository;
    @Autowired
    private  JwtUtil jwtUtil;
    @Autowired
    private VenteProduitRepository venteProduitRepository;

private FactureVenteResponseDTO toResponse(FactureVente facture) {
    Vente vente = facture.getVente();
    
    // Récupérer la remise globale
    Double remiseGlobale = vente.getRemiseGlobale() != null ? vente.getRemiseGlobale() : 0.0;
    boolean hasRemiseGlobale = remiseGlobale > 0;

    List<ProduitFactureResponse> produits = vente.getProduits().stream()
            .map(ligne -> {
                Integer quantiteAffichee;
                if (ligne.isEstRemboursee() && ligne.getQuantiteRemboursee() != null && ligne.getQuantiteRemboursee() > 0) {
                    quantiteAffichee = ligne.getQuantiteRemboursee();
                } else {
                    quantiteAffichee = ligne.getQuantite();
                }
                
                // Calculer le prix unitaire original si une remise globale a été appliquée
                Double prixUnitaireAffiche = ligne.getPrixUnitaire();
                Double montantLigneAffiche = ligne.getMontantLigne();
                
                if (hasRemiseGlobale) {
                    // Calcul inverse : prix_original = prix_actuel / (1 - remise_globale/100)
                    // Le montant actuel est déjà après remise, donc on recalcule le montant original
                    double montantLigneOriginal = montantLigneAffiche / (1 - remiseGlobale / 100.0);
                    prixUnitaireAffiche = montantLigneOriginal / quantiteAffichee;
                    montantLigneAffiche = montantLigneOriginal;
                    
                    // Arrondir à 2 décimales pour éviter les erreurs de précision
                    prixUnitaireAffiche = Math.round(prixUnitaireAffiche * 100.0) / 100.0;
                    montantLigneAffiche = Math.round(montantLigneAffiche * 100.0) / 100.0;
                }
                
                // Arrondir toutes les valeurs monétaires à 2 décimales
                Double remiseArrondie = Math.round(ligne.getRemise() * 100.0) / 100.0;
                
                return new ProduitFactureResponse(
                        ligne.getProduit().getId(),
                        ligne.getProduit().getNom(),
                        quantiteAffichee,
                        prixUnitaireAffiche,
                        remiseArrondie,
                        montantLigneAffiche
                );
            })
            .collect(Collectors.toList());

    String statutRemboursement = calculerStatutRemboursement(vente);

    Double montantDette = 0.0;
    Double montantPaye = 0.0;
    if (vente.getModePaiement() != null && vente.getModePaiement().name().equals("CREDIT")) {
        Double montantTotal = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
        Double montantRembourse = vente.getMontantTotalRembourse() != null ? vente.getMontantTotalRembourse() : 0.0;
        montantDette = Math.max(0.0, montantTotal - montantRembourse);
        montantPaye = montantRembourse;
    } else {
        montantPaye = vente.getMontantPaye() != null ? vente.getMontantPaye() : 
                     (vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0);
    }

    // Arrondir toutes les valeurs monétaires à 2 décimales
    Double montantTotalArrondi = facture.getMontantTotal() != null ? 
        Math.round(facture.getMontantTotal() * 100.0) / 100.0 : 0.0;
    Double montantDetteArrondi = Math.round(montantDette * 100.0) / 100.0;
    Double montantPayeArrondi = Math.round(montantPaye * 100.0) / 100.0;
    Double remiseGlobaleArrondie = Math.round(remiseGlobale * 100.0) / 100.0;
    
    FactureVenteResponseDTO dto = new FactureVenteResponseDTO();
    dto.setId(facture.getId());
    dto.setNumeroFacture(facture.getNumeroFacture());
    dto.setDateEmission(facture.getDateEmission());
    dto.setMontantTotal(montantTotalArrondi);
    dto.setClientNom(vente.getClientNom());
    dto.setClientNumero(vente.getClientNumero());
    dto.setBoutiqueNom(vente.getBoutique().getNomBoutique());
    dto.setProduits(produits);
    dto.setStatutRemboursement(statutRemboursement);
    dto.setCaisseId(vente.getCaisse() != null ? vente.getCaisse().getId() : null);
    // Statut de la caisse (peut être utile pour le front pour savoir si la caisse est encore ouverte ou déjà fermée)
    dto.setStatutCaisse(vente.getCaisse() != null && vente.getCaisse().getStatut() != null
            ? vente.getCaisse().getStatut().name()
            : null);
    dto.setVendeur(vente.getVendeur() != null ? vente.getVendeur().getNomComplet() : null);
    dto.setMontantDette(montantDetteArrondi);
    dto.setMontantPaye(montantPayeArrondi);
    dto.setRemiseGlobale(remiseGlobaleArrondie); // Ajouter la remise globale
    
    return dto;
}



/**
 * Récupère toutes les factures de vente avec pagination pour l'entreprise de l'utilisateur connecté
 */
public FactureVentePaginatedDTO getAllFacturesWithPagination(
        int page, 
        int size, 
        String sortBy, 
        String sortDir,
        String periode,
        LocalDate dateDebut,
        LocalDate dateFin,
        HttpServletRequest request) {
    
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou invalide");
    }
    String userUuid = jwtUtil.extractUserUuid(token.substring(7));
    User user = usersRepository.findByUuid(userUuid)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    Long entrepriseId = user.getEntreprise().getId();

    // Récupérer tous les vendeurs de l'entreprise (même ceux qui n'ont pas encore vendu)
    List<User> allUsersEntreprise = usersRepository.findByEntrepriseId(entrepriseId);
    List<VendeurFactureDTO> vendeurs = allUsersEntreprise.stream()
            .filter(u -> u.getRole() != null && u.getRole().getName() == RoleType.VENDEUR)
            .map(u -> new VendeurFactureDTO(u.getId(), u.getNomComplet()))
            .collect(Collectors.toList());

    // Calculer les dates selon la période
    PeriodeDates periodeDates = calculerDatesPeriode(periode, dateDebut, dateFin);
    
    // Récupérer les factures selon la période
    List<FactureVente> allFactures;
    if (periodeDates.filtrerParPeriode) {
        allFactures = factureVenteRepository.findAllByEntrepriseIdAndDateEmissionBetween(
                entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
    } else {
        allFactures = factureVenteRepository.findAllByEntrepriseId(entrepriseId);
    }
    
    List<FactureVenteResponseDTO> allItems = allFactures.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    
    // Trier selon le critère spécifié
    allItems.sort((item1, item2) -> {
        int comparison = 0;
        
        switch (sortBy.toLowerCase()) {
            case "dateemission":
            case "date":
                if (sortDir.equalsIgnoreCase("desc")) {
                    comparison = item2.getDateEmission().compareTo(item1.getDateEmission());
                } else {
                    comparison = item1.getDateEmission().compareTo(item2.getDateEmission());
                }
                break;
            case "vendeur":
                String vendeur1 = item1.getVendeur() != null ? item1.getVendeur() : "";
                String vendeur2 = item2.getVendeur() != null ? item2.getVendeur() : "";
                comparison = vendeur1.compareToIgnoreCase(vendeur2);
                if (sortDir.equalsIgnoreCase("desc")) {
                    comparison = -comparison;
                }
                break;
            case "boutique":
            case "boutiquenom":
                String boutique1 = item1.getBoutiqueNom() != null ? item1.getBoutiqueNom() : "";
                String boutique2 = item2.getBoutiqueNom() != null ? item2.getBoutiqueNom() : "";
                comparison = boutique1.compareToIgnoreCase(boutique2);
                if (sortDir.equalsIgnoreCase("desc")) {
                    comparison = -comparison;
                }
                break;
            default:
                // Par défaut, trier par dateEmission
                if (sortDir.equalsIgnoreCase("desc")) {
                    comparison = item2.getDateEmission().compareTo(item1.getDateEmission());
                } else {
                    comparison = item1.getDateEmission().compareTo(item2.getDateEmission());
                }
                break;
        }
        
        return comparison;
    });
    
    double totalMontantFactures = allItems.stream()
            .mapToDouble(item -> item.getMontantTotal() != null ? item.getMontantTotal() : 0.0)
            .sum();
    
    int nombreFacturesRemboursees = (int) allItems.stream()
            .filter(item -> "ENTIEREMENT_REMBOURSEE".equals(item.getStatutRemboursement()))
            .count();
    
    int nombreFacturesPartiellementRemboursees = (int) allItems.stream()
            .filter(item -> "PARTIELLEMENT_REMBOURSEE".equals(item.getStatutRemboursement()))
            .count();
    
    int nombreFacturesNormales = (int) allItems.stream()
            .filter(item -> "NORMAL".equals(item.getStatutRemboursement()))
            .count();
    
    int totalElements = allItems.size();
    int totalPages = (int) Math.ceil((double) totalElements / size);
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, totalElements);
    
    List<FactureVenteResponseDTO> pagedItems = 
        startIndex < totalElements ? allItems.subList(startIndex, endIndex) : new ArrayList<>();
    
    FactureVentePaginatedDTO response = new FactureVentePaginatedDTO();
    response.setPage(page);
    response.setSize(size);
    response.setTotalElements(totalElements);
    response.setTotalPages(totalPages);
    response.setFirst(page == 0);
    response.setLast(page >= totalPages - 1);
    response.setHasNext(page < totalPages - 1);
    response.setHasPrevious(page > 0);
    response.setItems(pagedItems);
    response.setTotalMontantFactures(totalMontantFactures);
    response.setNombreFactures(totalElements);
    response.setNombreFacturesRemboursees(nombreFacturesRemboursees);
    response.setNombreFacturesPartiellementRemboursees(nombreFacturesPartiellementRemboursees);
    response.setNombreFacturesNormales(nombreFacturesNormales);
    response.setVendeurs(vendeurs);
    
    return response;
}

/**
 * Calcule les dates de début et fin selon le type de période.
 */
private PeriodeDates calculerDatesPeriode(String periode, LocalDate dateDebut, LocalDate dateFin) {
    // Si dateDebut et dateFin sont fournis, utiliser automatiquement la période personnalisée
    if (dateDebut != null && dateFin != null) {
        return new PeriodeDates(dateDebut.atStartOfDay(), dateFin.plusDays(1).atStartOfDay(), true);
    }
    
    if (periode == null || periode.trim().isEmpty()) {
        periode = "aujourdhui";
    }
    
    LocalDateTime dateStart;
    LocalDateTime dateEnd;
    boolean filtrerParPeriode = true;

    switch (periode.toLowerCase()) {
        case "aujourdhui":
            dateStart = LocalDate.now().atStartOfDay();
            dateEnd = dateStart.plusDays(1);
            break;
        case "hier":
            dateStart = LocalDate.now().minusDays(1).atStartOfDay();
            dateEnd = dateStart.plusDays(1);
            break;
        case "semaine":
            LocalDate aujourdhui = LocalDate.now();
            dateStart = aujourdhui.minusDays(aujourdhui.getDayOfWeek().getValue() - 1).atStartOfDay();
            dateEnd = dateStart.plusWeeks(1);
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
                throw new RuntimeException("Les dates de début et de fin sont requises pour une période personnalisée.");
            }
            dateStart = dateDebut.atStartOfDay();
            dateEnd = dateFin.plusDays(1).atStartOfDay();
            break;
        default:
            // Par défaut, pas de filtre (toutes les données)
            filtrerParPeriode = false;
            dateStart = null;
            dateEnd = null;
    }

    return new PeriodeDates(dateStart, dateEnd, filtrerParPeriode);
}

/**
 * Classe pour stocker les dates de début et fin d'une période.
 */
private static class PeriodeDates {
    final LocalDateTime dateDebut;
    final LocalDateTime dateFin;
    final boolean filtrerParPeriode;

    PeriodeDates(LocalDateTime dateDebut, LocalDateTime dateFin, boolean filtrerParPeriode) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.filtrerParPeriode = filtrerParPeriode;
    }
}

public String genererNumeroFactureCompact(Vente vente) {
    //  Ticket ID modulo 1000 → 3 chiffres
    long ticketId = vente.getId() % 1000;

    //  POS / caisse modulo 100 → 2 chiffres
    long posId = vente.getCaisse() != null ? vente.getCaisse().getId() % 100 : 0;

    //  Date : jour + heure + minute → 6 chiffres
    String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddHHmm"));

    //  Hash alphanumérique 1 caractère pour réduire collision
    char hashChar = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".charAt(
            new Random().nextInt(36)
    );

    return String.format("%03d%02d%s%c", ticketId, posId, datePart, hashChar);
}


private String calculerStatutRemboursement(Vente vente) {
    boolean hasRemboursement = false;
    if (vente.getProduits() != null) {
        hasRemboursement = vente.getProduits().stream()
            .anyMatch(ligne -> (ligne.getQuantiteRemboursee() != null && ligne.getQuantiteRemboursee() > 0) 
                            || ligne.isEstRemboursee());
    }
    
    if (hasRemboursement) {
        if (vente.getMontantTotalRembourse() != null && 
            vente.getMontantTotalRembourse().equals(vente.getMontantTotal())) {
        return "ENTIEREMENT_REMBOURSEE";
        }
        return "PARTIELLEMENT_REMBOURSEE";
    }
    
    return "NORMAL";
}

/**
 * Récupère les données d'une facture pour l'envoi par email
 */
public ReceiptEmailRequest getFactureDataForEmail(String venteId, String email, HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou invalide");
    }
    String userUuid = jwtUtil.extractUserUuid(token.substring(7));
    User user = usersRepository.findByUuid(userUuid)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
    
    if (entrepriseId == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }
    
    Optional<FactureVente> factureOpt = factureVenteRepository.findByVenteIdAndEntrepriseId(
            Long.parseLong(venteId), entrepriseId);
    
    if (factureOpt.isEmpty()) {
        throw new RuntimeException("Facture non trouvée pour l'ID de vente : " + venteId);
    }
    
    FactureVente facture = factureOpt.get();
    Vente vente = facture.getVente();
    
    Map<Long, Double> remisesProduits = new HashMap<>();
    List<VenteLigneResponse> lignes = vente.getProduits().stream()
        .map(ligne -> {
            VenteLigneResponse ligneResponse = new VenteLigneResponse();
            ligneResponse.setProduitId(ligne.getProduit().getId());
            ligneResponse.setNomProduit(ligne.getProduit().getNom());
            ligneResponse.setQuantite(ligne.getQuantite());
            ligneResponse.setPrixUnitaire(ligne.getPrixUnitaire());
            ligneResponse.setMontantLigne(ligne.getMontantLigne());
            double remise = ligne.getRemise();
            ligneResponse.setRemise(remise);
            
            if (remise > 0) {
                remisesProduits.put(ligne.getProduit().getId(), remise);
            }
            
            return ligneResponse;
        })
        .collect(Collectors.toList());
    
    // Créer le ReceiptEmailRequest
    ReceiptEmailRequest receiptRequest = new ReceiptEmailRequest();
    receiptRequest.setEmail(email);
    receiptRequest.setVenteId(venteId);
    receiptRequest.setNumeroFacture(facture.getNumeroFacture());
    receiptRequest.setDateVente(vente.getDateVente());
    receiptRequest.setMontantTotal(BigDecimal.valueOf(vente.getMontantTotal()));
    receiptRequest.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().toString() : "Non spécifié");
    receiptRequest.setMontantPaye(BigDecimal.valueOf(vente.getMontantPaye() != null ? vente.getMontantPaye() : vente.getMontantTotal()));
    
    BigDecimal changeDue = receiptRequest.getMontantPaye().subtract(receiptRequest.getMontantTotal());
    receiptRequest.setChangeDue(changeDue.max(BigDecimal.ZERO));
    
    receiptRequest.setNomVendeur(vente.getVendeur() != null ? vente.getVendeur().getNomComplet() : "Non spécifié");
    receiptRequest.setNomBoutique(vente.getBoutique().getNomBoutique());
    receiptRequest.setLignes(lignes);
    
    receiptRequest.setRemiseGlobale(vente.getRemiseGlobale() != null ? vente.getRemiseGlobale() : 0.0);
    receiptRequest.setRemisesProduits(remisesProduits.isEmpty() ? null : remisesProduits);
    
    return receiptRequest;
}

/**
 * Récupère les statistiques globales de vente pour l'entreprise de l'utilisateur connecté
 * Inclut les 3 meilleurs produits vendus
 */
public StatistiquesVenteGlobalesDTO getStatistiquesGlobales(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou invalide");
    }
    String userUuid = jwtUtil.extractUserUuid(token.substring(7));
    // Utiliser la méthode optimisée avec JOIN FETCH pour éviter N+1
    User user = usersRepository.findByUuidWithEntrepriseAndRole(userUuid)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
    if (entrepriseId == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    List<Object[]> top3ProduitsData = venteProduitRepository.findTop3ProduitsVendusByEntrepriseId(entrepriseId);

    List<TopProduitVenduDTO> top3Produits = top3ProduitsData.stream()
            .map(row -> {
                TopProduitVenduDTO dto = new TopProduitVenduDTO();
                dto.setProduitId(((Number) row[0]).longValue());
                dto.setNomProduit((String) row[1]);
                dto.setTotalQuantite(((Number) row[2]).longValue());
                dto.setTotalMontant(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0);
                return dto;
            })
            .collect(Collectors.toList());

    StatistiquesVenteGlobalesDTO statistiques = new StatistiquesVenteGlobalesDTO();
    statistiques.setTop3ProduitsVendus(top3Produits);

    return statistiques;
}

}