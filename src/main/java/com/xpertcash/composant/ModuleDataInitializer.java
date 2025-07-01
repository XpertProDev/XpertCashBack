package com.xpertcash.composant;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.Module.ModuleRepository;
import com.xpertcash.service.Module.ModuleActivationService;




@Component
public class ModuleDataInitializer implements CommandLineRunner {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ModuleActivationService moduleActivationService;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Override
    public void run(String... args) {

        List<AppModule> modules = List.of(
            creerModule("VENTE", "Gestion des Ventes", "Le POS sert à vendre", true, false, null),
            creerModule("CLIENT", "Gestion des Clients", "", true, false, null),
            creerModule("ENTREPRISE_CLIENT", "Gestion des Entreprises Client", "", true, false, null),
            creerModule("USER", "Gestion des Employés", "", true, false, null),
            creerModule("BOUTIQUE", "Gestion des Boutiques", "", true, false, null),
            creerModule("PRODUIT", "Gestion des Produits", "", true, false, null),
            creerModule("STOCK", "Gestion des Stocks", "Gestion des stocks", true, false, null),

            creerModule("GESTION_FACTURATION", "Gestion de Facturation", "Gestion centralisée des factures", false, true, new BigDecimal("10000")),
            // Sous-modules structurels
            creerModule("FACTURE_PROFORMA", "Factures Proforma", "Création de devis/factures proforma", false, false, null),
            creerModule("FACTURE_REELLE", "Factures Réelles", "Émission de factures définitives", false, false, null)
        );

        // Création des modules s'ils n'existent pas
        for (AppModule module : modules) {
            Optional<AppModule> moduleExistantOpt = moduleRepository.findByCode(module.getCode());

            if (moduleExistantOpt.isEmpty()) {
                AppModule savedModule = moduleRepository.save(module);

               if (savedModule.isPayant()) {
                List<Entreprise> entreprises = entrepriseRepository.findAll();
                for (Entreprise entreprise : entreprises) {
                    moduleActivationService.activerEssaiPourEntreprise(entreprise, savedModule);
                }
            }

            }
        }

        // Mise à jour des entreprises existantes
        List<Entreprise> entreprises = entrepriseRepository.findAll();

        Set<AppModule> modulesParDefaut = new HashSet<>(moduleRepository.findByActifParDefautTrue());
        List<AppModule> modulesPayants = moduleRepository.findByPayantTrue();

        for (Entreprise entreprise : entreprises) {

            // Initialiser la liste des modules actifs si nécessaire
            if (entreprise.getModulesActifs() == null) {
                entreprise.setModulesActifs(new HashSet<>());
            }

            // Ajouter les modules par défaut s'ils ne sont pas déjà présents
            entreprise.getModulesActifs().addAll(modulesParDefaut);

            // Activation des essais pour tous les modules payants manquants
            for (AppModule modulePayant : modulesPayants) {
                if (!moduleActivationService.dejaEssaiPourEntreprise(entreprise, modulePayant)) {
                    moduleActivationService.activerEssaiPourEntreprise(entreprise, modulePayant);
                }
            }

            entrepriseRepository.save(entreprise);
        }

   
    }

    private AppModule creerModule(String code, String nom, String description, boolean actifParDefaut, boolean payant, BigDecimal prix) {
        AppModule module = new AppModule();
        module.setCode(code);
        module.setNom(nom);
        module.setDescription(description);
        module.setActifParDefaut(actifParDefaut);
        module.setPayant(payant);
        module.setPrix(prix);
        return module;
    }
}


