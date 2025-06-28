package com.xpertcash.composant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.repository.Module.ModuleRepository;
import com.xpertcash.service.Module.ModuleActivationService;




@Component
public class ModuleDataInitializer implements CommandLineRunner {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ModuleActivationService moduleActivationService;

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

        for (AppModule module : modules) {
            Optional<AppModule> moduleExistantOpt = moduleRepository.findByCode(module.getCode());

            if (moduleExistantOpt.isEmpty()) {
                AppModule savedModule = moduleRepository.save(module);

                // Activation d'essai uniquement sur les modules payants (ici uniquement GESTION_FACTURATION)
                if (savedModule.isPayant()) {
                    moduleActivationService.activerEssaiPourToutesLesEntreprises(savedModule);
                }
            }
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


//exemple de new ligne dans la liste des modules comme payant
//creerModule("CRM", "Module CRM Premium", false, true, new BigDecimal("45000"))

