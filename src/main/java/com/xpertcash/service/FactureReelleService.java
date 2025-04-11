package com.xpertcash.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.LigneFactureReelle;
import com.xpertcash.entity.StatutPaiementFacture;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.LigneFactureReelleRepository;

@Service
public class FactureReelleService {

     @Autowired
    private FactureReelleRepository factureReelleRepository;

    @Autowired
    private LigneFactureReelleRepository ligneFactureReelleRepository;


    public FactureReelle genererFactureReelle(FactureProForma factureProForma) {
        FactureReelle factureReelle = new FactureReelle();
        
        // Copier les informations de la proforma
        factureReelle.setNumeroFacture(genererNumeroFactureReel());
        factureReelle.setDateCreation(LocalDate.now());
        factureReelle.setTotalHT(factureProForma.getTotalHT());
        factureReelle.setRemise(factureProForma.getRemise());
        factureReelle.setDescription(factureProForma.getDescription());

        factureReelle.setTva(factureProForma.isTva());
        factureReelle.setTotalFacture(factureProForma.getTotalFacture());
        factureReelle.setStatutPaiement(StatutPaiementFacture.EN_ATTENTE);

        factureReelle.setUtilisateurCreateur(factureProForma.getUtilisateurModificateur());
        factureReelle.setClient(factureProForma.getClient());
        factureReelle.setEntrepriseClient(factureProForma.getEntrepriseClient());
        factureReelle.setEntreprise(factureProForma.getEntreprise());
        factureReelle.setFactureProForma(factureProForma);
    
        // Sauvegarder la facture réelle AVANT d'ajouter les lignes (important pour les relations en base)
        FactureReelle factureReelleSauvegardee = factureReelleRepository.save(factureReelle);
    
        // Copier les lignes de facture
        List<LigneFactureReelle> lignesFacture = factureProForma.getLignesFacture().stream().map(ligneProForma -> {
            LigneFactureReelle ligneReelle = new LigneFactureReelle();
            ligneReelle.setProduit(ligneProForma.getProduit());
            ligneReelle.setQuantite(ligneProForma.getQuantite());
            ligneReelle.setPrixUnitaire(ligneProForma.getPrixUnitaire());
            ligneReelle.setMontantTotal(ligneProForma.getMontantTotal());
            ligneReelle.setFactureReelle(factureReelleSauvegardee); // Utiliser la facture sauvegardée
            return ligneReelle;
        }).collect(Collectors.toList());
    
        ligneFactureReelleRepository.saveAll(lignesFacture);
    
        return factureReelleSauvegardee;
    }

    
    private String genererNumeroFactureReel() {
         // la date actuelle
             LocalDate currentDate = LocalDate.now();
            String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));

            Optional<FactureReelle> lastFacture = factureReelleRepository.findTopByDateCreationOrderByNumeroFactureDesc(currentDate);
            int newIndex = 1;
            if (lastFacture.isPresent()) {
                String lastNumeroFacture = lastFacture.get().getNumeroFacture();
                // Assumer que le format est "FACTURE PROFORMA N°XXX-dd-MM-yyyy"
                String[] parts = lastNumeroFacture.split("-");
                newIndex = Integer.parseInt(parts[0].replace("FACTURE N°", "")) + 1;
            }

            return String.format("FACTURE N°%03d-%s", newIndex, formattedDate);

    }

  

}
