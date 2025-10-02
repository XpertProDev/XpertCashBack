package com.xpertcash.service.VENTE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.VENTE.FactureVenteResponseDTO;
import com.xpertcash.DTOs.VENTE.FactureVentePaginatedDTO;
import com.xpertcash.DTOs.VENTE.ProduitFactureResponse;
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
                // Pour les remboursements, afficher la quantité remboursée si elle existe
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

    // Calculer le statut de remboursement
    String statutRemboursement = calculerStatutRemboursement(vente);

    return new FactureVenteResponseDTO(
            facture.getId(),
            facture.getNumeroFacture(),
            facture.getDateEmission(),
            facture.getMontantTotal(),
            vente.getClientNom(),
            vente.getClientNumero(),
            vente.getBoutique().getNomBoutique(),
            produits,
            statutRemboursement,
            vente.getCaisse() != null ? vente.getCaisse().getId() : null,
            vente.getVendeur() != null ? vente.getVendeur().getNomComplet() : null
    );
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

    // Récupérer toutes les factures de l'entreprise
    List<FactureVente> allFactures = factureVenteRepository.findAllByEntrepriseId(entrepriseId);
    
    // Convertir en DTOs
    List<FactureVenteResponseDTO> allItems = allFactures.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    
    // Trier par date (par défaut)
    allItems.sort((item1, item2) -> {
        if (sortDir.equalsIgnoreCase("desc")) {
            return item2.getDateEmission().compareTo(item1.getDateEmission());
        } else {
            return item1.getDateEmission().compareTo(item2.getDateEmission());
        }
    });
    
    // Calculer les statistiques
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
    
    // Appliquer la pagination manuelle
    int totalElements = allItems.size();
    int totalPages = (int) Math.ceil((double) totalElements / size);
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, totalElements);
    
    List<FactureVenteResponseDTO> pagedItems = 
        startIndex < totalElements ? allItems.subList(startIndex, endIndex) : new ArrayList<>();
    
    // Construire la réponse
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
    // 1️⃣ Ticket ID modulo 1000 → 3 chiffres
    long ticketId = vente.getId() % 1000;

    // 2️⃣ POS / caisse modulo 100 → 2 chiffres
    long posId = vente.getCaisse() != null ? vente.getCaisse().getId() % 100 : 0;

    // 3️⃣ Date : jour + heure + minute → 6 chiffres
    String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddHHmm"));

    // 4️⃣ Hash alphanumérique 1 caractère pour réduire collision
    char hashChar = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".charAt(
            new Random().nextInt(36)
    );

    // 🔢 Combinaison finale = 3 + 2 + 6 + 1 = 12 caractères
    return String.format("%03d%02d%s%c", ticketId, posId, datePart, hashChar);
}

/**
 * Calcule le statut de remboursement d'une vente
 */
private String calculerStatutRemboursement(Vente vente) {
    if (vente.getMontantTotalRembourse() == null || vente.getMontantTotalRembourse() == 0.0) {
        return "NORMAL";
    }
    
    if (vente.getMontantTotalRembourse().equals(vente.getMontantTotal())) {
        return "ENTIEREMENT_REMBOURSEE";
    }
    
    return "PARTIELLEMENT_REMBOURSEE";
}

}