package com.xpertcash.service.VENTE;

import java.math.BigDecimal;
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
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.FactureVente;
import com.xpertcash.entity.User;
import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.repository.FactureVenteRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class FactureVenteService {

    @Autowired
    private FactureVenteRepository factureVenteRepository;
    @Autowired
    private  UsersRepository usersRepository;
    @Autowired
    private  JwtUtil jwtUtil;

private FactureVenteResponseDTO toResponse(FactureVente facture) {
    Vente vente = facture.getVente();

    List<ProduitFactureResponse> produits = vente.getProduits().stream()
            .map(ligne -> {
                Integer quantiteAffichee;
                if (ligne.isEstRemboursee() && ligne.getQuantiteRemboursee() != null && ligne.getQuantiteRemboursee() > 0) {
                    quantiteAffichee = ligne.getQuantiteRemboursee();
                } else {
                    quantiteAffichee = ligne.getQuantite();
                }
                
                return new ProduitFactureResponse(
                        ligne.getProduit().getId(),
                        ligne.getProduit().getNom(),
                        quantiteAffichee,
                        ligne.getPrixUnitaire(),
                        ligne.getRemise(),
                        ligne.getMontantLigne()
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

    FactureVenteResponseDTO dto = new FactureVenteResponseDTO();
    dto.setId(facture.getId());
    dto.setNumeroFacture(facture.getNumeroFacture());
    dto.setDateEmission(facture.getDateEmission());
    dto.setMontantTotal(facture.getMontantTotal());
    dto.setClientNom(vente.getClientNom());
    dto.setClientNumero(vente.getClientNumero());
    dto.setBoutiqueNom(vente.getBoutique().getNomBoutique());
    dto.setProduits(produits);
    dto.setStatutRemboursement(statutRemboursement);
    dto.setCaisseId(vente.getCaisse() != null ? vente.getCaisse().getId() : null);
    dto.setVendeur(vente.getVendeur() != null ? vente.getVendeur().getNomComplet() : null);
    dto.setMontantDette(montantDette);
    dto.setMontantPaye(montantPaye);
    
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
        HttpServletRequest request) {
    
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou invalide");
    }
    String userUuid = jwtUtil.extractUserUuid(token.substring(7));
    User user = usersRepository.findByUuid(userUuid)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    Long entrepriseId = user.getEntreprise().getId();

    List<FactureVente> allFactures = factureVenteRepository.findAllByEntrepriseId(entrepriseId);
    
    List<FactureVenteResponseDTO> allItems = allFactures.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    
    allItems.sort((item1, item2) -> {
        if (sortDir.equalsIgnoreCase("desc")) {
            return item2.getDateEmission().compareTo(item1.getDateEmission());
        } else {
            return item1.getDateEmission().compareTo(item2.getDateEmission());
        }
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
    
    return response;
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

}