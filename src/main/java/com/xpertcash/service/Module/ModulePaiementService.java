package com.xpertcash.service.Module;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.entity.Module.PaiementModule;
import com.xpertcash.repository.Module.PaiementModuleRepository;

@Service

public class ModulePaiementService {
    @Autowired
    private PaiementModuleRepository paiementModuleRepository;


 public boolean effectuerPaiement(String numeroCarte, String cvc, String dateExpiration,
                                 BigDecimal montant, Entreprise entreprise, AppModule module,
                                 String nom, String prenom, String adresse, String ville) {
    // Ici, on simule le paiement
    System.out.println("Paiement simulé de " + montant + " par " + prenom + " " + nom);

     // Historisation
    PaiementModule paiement = new PaiementModule();
    paiement.setMontant(montant);
    paiement.setNomProprietaire(nom);
    paiement.setPrenomProprietaire(prenom);
    paiement.setAdresse(adresse);
    paiement.setVille(ville);
    paiement.setDatePaiement(LocalDateTime.now());
    paiement.setEntreprise(entreprise);
    paiement.setModule(module);
    paiement.setReferenceTransaction(UUID.randomUUID().toString()); // Simulation d'une ref unique

    paiementModuleRepository.save(paiement);
    
    return true;
}

public void enregistrerFacturePaiement(Entreprise entreprise, AppModule module, BigDecimal montant,
                                       String nom, String prenom, String adresse, String ville) {
    // Ici, tu peux enregistrer en BDD ou générer un PDF, selon ton système
    System.out.println("Facture générée pour : " + nom + " " + prenom + ", " + adresse + ", " + ville);
}

}

