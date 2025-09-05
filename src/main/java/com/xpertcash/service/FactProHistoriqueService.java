package com.xpertcash.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.service.AuthenticationHelper;
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
    private AuthenticationHelper authHelper;



public void enregistrerActionHistorique(FactureProForma facture, User user, String action, String details) {
    FactProHistoriqueAction historique = new FactProHistoriqueAction();
    historique.setFacture(facture);
    historique.setUtilisateur(user);
    historique.setAction(action);
    historique.setDateAction(LocalDateTime.now());
    historique.setMontantFacture(BigDecimal.valueOf(facture.getTotalHT()));
    historique.setDetails(details);

     factProHistoriqueActionRepository.save(historique);
}


  public Map<String, Object> getHistoriqueFacture(Long factureId, HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    FactureProForma facture = factureProformaRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture Proforma introuvable avec l'ID : " + factureId));

    if (!facture.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès refusé : Cette facture ne vous appartient pas !");
    }

    Map<String, Object> historique = new HashMap<>();
    historique.put("id", facture.getId());
    historique.put("numeroFacture", facture.getNumeroFacture());
    historique.put("dateCreation", facture.getDateCreation());
    historique.put("description", facture.getDescription());
    historique.put("statut", facture.getStatut());
    historique.put("dateRelance", facture.getDateRelance());


    

    historique.put("creePar", facture.getUtilisateurCreateur() != null ?
        facture.getUtilisateurCreateur().getNomComplet() : "Inconnu");

    historique.put("dernierModificateur", facture.getUtilisateurModificateur() != null ?
        facture.getUtilisateurModificateur().getNomComplet() : "Aucune modification");

    historique.put("approuvePar", facture.getUtilisateurApprobateur() != null ?
        facture.getUtilisateurApprobateur().getNomComplet() : "Non approuvé");

      // Récupérer TOUTES les actions historiques sans filtre
      List<FactProHistoriqueAction> actionList = factProHistoriqueActionRepository
              .findByFactureIdOrderByDateActionDesc(factureId); // Tri décroissant

      List<Map<String, Object>> actionsResume = new ArrayList<>();

    Set<String> actionsImportantes = Set.of("Création", "Modification", "Approbation", "Approuver", "Envoi", "Validation");

      for (FactProHistoriqueAction action : actionList) {
          Map<String, Object> entry = new HashMap<>();
          entry.put("action", action.getAction());
          entry.put("date", action.getDateAction());
          entry.put("utilisateur", action.getUtilisateur().getNomComplet());
          entry.put("photo", action.getUtilisateur().getPhoto());
          entry.put("montant", action.getMontantFacture());
          entry.put("details", action.getDetails());
          actionsResume.add(entry);
      }

    historique.put("historiqueActions", actionsResume);

    return historique;
}




}
