package com.xpertcash.composant;
import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.repository.Module.ModuleRepository;




@Component
public class ModuleDataInitializer implements CommandLineRunner {

    @Autowired
    private ModuleRepository moduleRepository;

    @Override
    public void run(String... args) {

        // Vente
        if (!moduleRepository.existsByCode("VENTE")) {
            AppModule vente = new AppModule();
            vente.setCode("VENTE");
            vente.setNom("Gestion des Ventes");
            vente.setActifParDefaut(true);
            vente.setPayant(false);
            moduleRepository.save(vente);
        }

        // Gestion des clients
        if (!moduleRepository.existsByCode("CLIENT")) {
            AppModule client = new AppModule();
            client.setCode("CLIENT");
            client.setNom("Gestion des Clients");
            client.setActifParDefaut(true);
            client.setPayant(false);
            moduleRepository.save(client);
        }

        // Entreprise client
        if (!moduleRepository.existsByCode("ENTREPRISE_CLIENT")) {
            AppModule entrepriseClient = new AppModule();
            entrepriseClient.setCode("ENTREPRISE_CLIENT");
            entrepriseClient.setNom("Gestion des Entreprises Client");
            entrepriseClient.setActifParDefaut(true);
            entrepriseClient.setPayant(false);
            moduleRepository.save(entrepriseClient);
        }

        // Gestion des utilisateurs
        if (!moduleRepository.existsByCode("USER")) {
            AppModule user = new AppModule();
            user.setCode("USER");
            user.setNom("Gestion des Employés");
            user.setActifParDefaut(true);
            user.setPayant(false);
            moduleRepository.save(user);
        }

        // Gestion des boutiques
        if (!moduleRepository.existsByCode("BOUTIQUE")) {
            AppModule boutique = new AppModule();
            boutique.setCode("BOUTIQUE");
            boutique.setNom("Gestion des Boutiques");
            boutique.setActifParDefaut(true);
            boutique.setPayant(false);
            moduleRepository.save(boutique);
        }

        // Gestion des produits
        if (!moduleRepository.existsByCode("PRODUIT")) {
            AppModule produit = new AppModule();
            produit.setCode("PRODUIT");
            produit.setNom("Gestion des Produits");
            produit.setActifParDefaut(true);
            produit.setPayant(false);
            moduleRepository.save(produit);
        }

        // Gestion des factures
        if (!moduleRepository.existsByCode("FACTURE")) {
            AppModule facture = new AppModule();
            facture.setCode("FACTURE");
            facture.setNom("Gestion des Factures");
            facture.setActifParDefaut(true);
            facture.setPayant(false);
            moduleRepository.save(facture);
        }

        // Factures Proforma
        if (!moduleRepository.existsByCode("FACTURE_PROFORMA")) {
            AppModule proforma = new AppModule();
            proforma.setCode("FACTURE_PROFORMA");
            proforma.setNom("Gestion des Factures Proforma");
            proforma.setActifParDefaut(true);
            proforma.setPayant(false);
            moduleRepository.save(proforma);
        }

        // Factures Réelles
        if (!moduleRepository.existsByCode("FACTURE_REELLE")) {
            AppModule reel = new AppModule();
            reel.setCode("FACTURE_REELLE");
            reel.setNom("Gestion des Factures Réelles");
            reel.setActifParDefaut(false);
            reel.setPayant(true);
            reel.setPrix(new BigDecimal("30000"));
            moduleRepository.save(reel);
        }
    }
}

