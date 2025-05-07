package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.FactProHistoriqueAction;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.User;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactProHistoriqueActionRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class FactProHistoriqueService {

     @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    private FactProHistoriqueActionRepository factProHistoriqueActionRepository;


     @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtUtil jwtUtil;






    public Map<String, Object> getHistoriqueFacture(Long factureId, HttpServletRequest request) {
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



            FactureProForma facture = factureProformaRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture Proforma introuvable avec l'ID : " + factureId));

    if (!facture.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès refusé : Cette facture ne vous appartient pas !");
    }

    // Vérification que l'utilisateur appartient à l'entreprise (si besoin ici aussi)
    // ...


    Map<String, Object> historique = new HashMap<>();

    historique.put("id", facture.getId());
    historique.put("numeroFacture", facture.getNumeroFacture());
    historique.put("dateCreation", facture.getDateCreation());
    historique.put("description", facture.getDescription());
    historique.put("statut", facture.getStatut());

    historique.put("creePar", facture.getUtilisateurModificateur() != null ?
            facture.getUtilisateurModificateur().getNomComplet() : "Inconnu");

    historique.put("approuvePar", facture.getUtilisateurApprobateur() != null ?
            facture.getUtilisateurApprobateur().getNomComplet() : "Non approuvé");



    historique.put("modifications", facture.getLignesFacture().stream().map(ligne -> {
        Map<String, Object> modification = new HashMap<>();
        modification.put("produit", ligne.getProduit().getNom());
        modification.put("quantite", ligne.getQuantite());
        modification.put("prixUnitaire", ligne.getPrixUnitaire());
        modification.put("montantTotal", ligne.getMontantTotal());
        return modification;
    }).collect(Collectors.toList()));

    // Ajouter l’historique des actions
    List<Map<String, Object>> actions = factProHistoriqueActionRepository.findByFactureIdOrderByDateActionAsc(factureId)
        .stream()
        .map(action -> {
            Map<String, Object> map = new HashMap<>();
            map.put("action", action.getAction());
            map.put("date", action.getDateAction());
            map.put("utilisateur", action.getUtilisateur().getNomComplet());
            map.put("details", action.getDetails());
            return map;
        }).collect(Collectors.toList());

    historique.put("historiqueActions", actions);

    return historique;
}


public void enregistrerActionHistorique(FactureProForma facture, User user, String action, String details) {
    FactProHistoriqueAction historique = new FactProHistoriqueAction();
    historique.setFacture(facture);
    historique.setUtilisateur(user);
    historique.setAction(action);
    historique.setDateAction(LocalDateTime.now());
    historique.setDetails(details);

    factProHistoriqueActionRepository.save(historique);
}



}
