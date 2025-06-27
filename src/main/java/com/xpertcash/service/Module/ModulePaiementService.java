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

    /**
     * Simulation du paiement, sans impact BDD
     */
    public boolean effectuerPaiement(String numeroCarte, String cvc, String dateExpiration,
                                     BigDecimal montant, Entreprise entreprise, AppModule module,
                                     String nom, String prenom,String email, String adresse, String ville) {

        System.out.println("Paiement simulé de " + montant + " par " + prenom + " " + nom);

        // Ici, tu pourrais ajouter des vérifications factices ou logs supplémentaires si nécessaire
        return true;
    }

    /**
     * Historise le paiement et retourne la référence de transaction
     */
    public String enregistrerFacturePaiement(Entreprise entreprise, AppModule module, BigDecimal montant,
                                             String nom, String prenom,String email, String adresse, String ville) {

        PaiementModule paiement = new PaiementModule();
        paiement.setMontant(montant);
        paiement.setNomProprietaire(nom);
        paiement.setPrenomProprietaire(prenom);
        paiement.setEmailProprietaireCarte(email); // Si tu veux stocker l'email, sinon tu peux l'enlever
        paiement.setAdresse(adresse);
        paiement.setVille(ville);
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setEntreprise(entreprise);
        paiement.setModule(module);

        String referenceTransaction = UUID.randomUUID().toString();
        paiement.setReferenceTransaction(referenceTransaction);

        paiementModuleRepository.save(paiement);

        System.out.println("Facture générée pour : " + nom + " " + prenom + ", " + adresse + ", " + ville);
        System.out.println("Référence de transaction : " + referenceTransaction);

        return referenceTransaction;
    }
}
