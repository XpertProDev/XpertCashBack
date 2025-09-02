package com.xpertcash.service.VENTE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.VENTE.FactureVenteResponseDTO;
import com.xpertcash.DTOs.VENTE.ProduitFactureResponse;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Caisse;
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
            .map(ligne -> new ProduitFactureResponse(
                    ligne.getProduit().getId(),
                    ligne.getProduit().getNom(),
                    ligne.getQuantite(),
                    ligne.getPrixUnitaire(),
                    ligne.getRemise(),
                    ligne.getMontantLigne()
            ))
            .toList();

    return new FactureVenteResponseDTO(
            facture.getId(),
            facture.getNumeroFacture(),
            facture.getDateEmission(),
            facture.getMontantTotal(),
            vente.getClientNom(),
            vente.getClientNumero(),
            vente.getBoutique().getNomBoutique(),
            produits
    );
}


public List<FactureVenteResponseDTO> getAllFacturesForConnectedUser(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou invalide");
    }
    String userUuid = jwtUtil.extractUserUuid(token.substring(7));
    User user = usersRepository.findByUuid(userUuid)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    Long entrepriseId = user.getEntreprise().getId();

    return factureVenteRepository.findAllByEntrepriseId(entrepriseId)
            .stream()
            .map(this::toResponse)
            .toList();
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




}
