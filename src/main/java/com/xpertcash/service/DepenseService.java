package com.xpertcash.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Depense;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.repository.DepenseRepository;
import com.xpertcash.repository.UsersRepository;



@Service
public class DepenseService {

    @Autowired
    private DepenseRepository depenseRepository;

    @Autowired
    private UsersRepository usersRepository;

    // Méthode pour enregistrer une dépense
    public Depense enregistrerDepense(double montant, String description, User user2) {
        // Récupérer l'utilisateur
        User user = usersRepository.findById(user2.getId())
        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // L'entreprise liée à l'utilisateur
        Entreprise entreprise = user.getEntreprise();

        // Créer la dépense
        Depense depense = new Depense();
        depense.setMontant(montant);
        depense.setDescription(description);
        depense.setDate(new Date());
        depense.setEntreprise(entreprise);  // Associer l'entreprise à la dépense

        // Enregistrer la dépense dans la base de données
        return depenseRepository.save(depense);
    }

    // Méthode pour récupérer toutes les dépenses
    public List<Depense> getAllDepenses() {
        return depenseRepository.findAll();
    }

    // Méthode pour récupérer les dépenses par utilisateur
    public List<Depense> getDepensesByUser(User user) {
        return depenseRepository.findByEntreprise(user.getEntreprise());
    }

    // Méthode pour récupérer les dépenses entre deux dates
    public List<Depense> getDepensesBetweenDates(Date startDate, Date endDate) {
        return depenseRepository.findByDateBetween(startDate, endDate);
    }
}
