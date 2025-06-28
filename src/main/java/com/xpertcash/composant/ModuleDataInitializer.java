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

        // Définition des modules
        List<AppModule> modules = List.of(
            creerModule("VENTE", "Gestion des Ventes","le POS sert a vendre", true, false, null),
            creerModule("CLIENT", "Gestion des Clients","", true, false, null),
            creerModule("ENTREPRISE_CLIENT", "Gestion des Entreprises Client","", true, false, null),
            creerModule("USER", "Gestion des Employés","", true, false, null),
            creerModule("BOUTIQUE", "Gestion des Boutiques","", true, false, null),
            creerModule("PRODUIT", "Gestion des Produits","", true, false, null),
            creerModule("FACTURE", "Gestion des Factures","", true, false, null),
            creerModule("FACTURE_PROFORMA", "Gestion des Factures Proforma","", true, false, null),
            creerModule("FACTURE_REELLE", "Gestion des Factures Réelles","", false, true, new BigDecimal("30000")),
            creerModule("STOCK", "Gestion des Stock","", false, true, new BigDecimal("10000"))

        );

        for (AppModule module : modules) {
            Optional<AppModule> moduleExistantOpt = moduleRepository.findByCode(module.getCode());

            if (moduleExistantOpt.isEmpty()) {
                AppModule savedModule = moduleRepository.save(module);

                // Si c'est un module payant, on active automatiquement l'essai pour les entreprises existantes
                if (savedModule.isPayant()) {
                    moduleActivationService.activerEssaiPourToutesLesEntreprises(savedModule);
                }
            }
        }
    }

    private AppModule creerModule(String code, String nom,String description, boolean actifParDefaut, boolean payant, BigDecimal prix) {
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

