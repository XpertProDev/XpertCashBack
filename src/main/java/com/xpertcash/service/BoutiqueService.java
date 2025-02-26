package com.xpertcash.service;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.EntrepriseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BoutiqueService {

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    public Boutique createBoutiqueForUser(User user, String nomBoutique) {
        if (boutiqueRepository.findByNomBoutiqueAndEntreprise(nomBoutique, user.getEntreprise()).isPresent()) {
            throw new RuntimeException("Une boutique avec ce nom existe déjà pour cette entreprise.");
        }

        // Création de la boutique liée à l'utilisateur et à l'entreprise
        Boutique boutique = new Boutique();
        boutique.setNomBoutique(nomBoutique);
        boutique.setEntreprise(user.getEntreprise());
        boutique.setUser(user);
        boutique.setCreatedAt(LocalDateTime.now());

        return boutiqueRepository.save(boutique);
    }

    public Boutique updateBoutiqueName(Long boutiqueId, String newNomBoutique) {
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("La boutique n'a pas été trouvée."));

        boutique.setNomBoutique(newNomBoutique);
        return boutiqueRepository.save(boutique);
    }

    public List<Boutique> getAllBoutiquesForEntreprise(Entreprise entreprise) {
        return boutiqueRepository.findByEntreprise(entreprise);
    }
}

