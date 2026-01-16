package com.xpertcash.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.User;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.FactureProformaRepository;

import jakarta.transaction.Transactional;

@Service
public class NotificationService {

     @Autowired
    private MailService mailService;

     @Autowired
    private FactureProformaRepository factureProformaRepository;

     @Autowired
    private EntrepriseRepository entrepriseRepository;

    // tâche planifiée : Vérifie tous les jours à 08h00 quelles factures doivent être relancées
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void verifierFacturesAEnvoyer() {
        LocalDateTime maintenant = LocalDateTime.now().withSecond(0).withNano(0);
    
        System.out.println(" Vérification des factures à relancer à " + maintenant);
    
        List<Entreprise> entreprises = entrepriseRepository.findAll();
        int totalFacturesAEnvoyer = 0;
        
        for (Entreprise entreprise : entreprises) {
            List<FactureProForma> facturesAEnvoyer = factureProformaRepository.findFacturesAEnvoyerByEntrepriseId(
                    entreprise.getId(), maintenant);
            totalFacturesAEnvoyer += facturesAEnvoyer.size();
            
            for (FactureProForma facture : facturesAEnvoyer) {
            System.out.println("Facture à relancer : " + facture.getNumeroFacture() +
                               ", Date Relance : " + facture.getDateRelance() +
                               ", Dernier Rappel Envoyé : " + facture.getDernierRappelEnvoye() +
                               ", Notifiée : " + facture.isNotifie());
    
                               try {
                                User utilisateurRelanceur = facture.getUtilisateurRelanceur();
                                if (utilisateurRelanceur == null || utilisateurRelanceur.getEmail() == null) {
                                    System.err.println(" Impossible d'envoyer l'email : Aucun utilisateur relanceur défini pour la facture " + facture.getNumeroFacture());
                                    continue;
                                }
                            
                                String emailUtilisateur = utilisateurRelanceur.getEmail();
                                String nomUtilisateur = utilisateurRelanceur.getNomComplet();
                            
                                Date relanceDate = Date.from(facture.getDateRelance().atZone(ZoneId.systemDefault()).toInstant());
                            
                                String clientName = facture.getClient() != null 
                                                    ? facture.getClient().getNomComplet() 
                                                    : (facture.getEntrepriseClient() != null 
                                                        ? facture.getEntrepriseClient().getNom() 
                                                        : "Client inconnu");
                            
                                                        boolean estEntreprise = facture.getEntrepriseClient() != null;
                                                        mailService.sendRelanceeEmail(emailUtilisateur, nomUtilisateur, facture.getNumeroFacture(), clientName, relanceDate, estEntreprise);
                                                        
                                facture.setNotifie(true);
                                facture.setDernierRappelEnvoye(maintenant);
                                factureProformaRepository.save(facture);
                            
                                System.out.println(" Notification envoyée pour la facture " + facture.getNumeroFacture());
                            } catch (Exception e) {
                                System.err.println(" Erreur lors de l'envoi de la notification pour la facture " + facture.getNumeroFacture() + " : " + e.getMessage());
                            }
                        }
        }
        
        System.out.println(" Nombre total de factures à relancer : " + totalFacturesAEnvoyer);
    }
   

}
