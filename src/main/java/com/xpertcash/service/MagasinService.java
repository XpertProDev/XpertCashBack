package com.xpertcash.service;

import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.entity.*;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.MagasinRepository;
import com.xpertcash.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MagasinService {
    @Autowired
    private MagasinRepository magasinRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private EntrepriseRepository entrepriseRepository;

    // Méthode pour ajouter un magasin (Admin seulement)
    public Magasin ajouterMagasin(Long userId, String nomMagasin) {
        // Vérification de l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));

        // Vérifier si l'utilisateur a une entreprise associée
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur ne fait partie d'aucune entreprise.");
        }

        // Vérification de l'administrateur et des permissions
        authorizationService.checkPermission(user, PermissionType.GERER_MAGASINS);

        // Vérifier si le magasin existe déjà dans l'entreprise
        Optional<Magasin> existingMagasin = magasinRepository.findByNomMagasinAndEntreprise(nomMagasin, entreprise);
        if (existingMagasin.isPresent()) {
            throw new RuntimeException("Un magasin avec ce nom existe déjà dans votre entreprise.");
        }

        // Création du magasin et association avec l'entreprise
        Magasin magasin = new Magasin(nomMagasin, entreprise);

        // Sauvegarder le magasin dans la base de données
        return magasinRepository.save(magasin);
    }


    //  Récupérer tous les magasins d'une entreprise
    public List<Magasin> getMagasinsByEntreprise(Long userId) {
        // Vérification de l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));

        if (user.getEntreprise() == null) {
            throw new RuntimeException("L'utilisateur ne fait partie d'aucune entreprise.");
        }

        return magasinRepository.findByEntreprise(user.getEntreprise());
    }

    // Supprimer un magasin (Admin seulement)
    public void supprimerMagasin(Long userId, Long magasinId) {
        // Vérification de l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));

        if (user.getEntreprise() == null) {
            throw new RuntimeException("L'utilisateur ne fait partie d'aucune entreprise.");
        }

        // Vérification de l'administrateur et des permissions
        authorizationService.checkPermission(user, PermissionType.GERER_MAGASINS);

        // Vérifier si le magasin existe et appartient bien à l'entreprise
        Magasin magasin = magasinRepository.findById(magasinId)
                .orElseThrow(() -> new RuntimeException("Magasin non trouvé."));

        if (!magasin.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Ce magasin ne vous appartient pas.");
        }

        magasinRepository.delete(magasin);
    }

    //Modififer Magasin
    public Magasin modifierMagasin(Long userId, Long magasinId, String nouveauNomMagasin) {
        // Vérification de l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));

        // Vérification de l'existence du magasin
        Magasin magasin = magasinRepository.findById(magasinId)
                .orElseThrow(() -> new NotFoundException("Magasin non trouvé"));

        // Vérifier que l'utilisateur appartient à la même entreprise que le magasin
        if (!magasin.getEntreprise().equals(user.getEntreprise())) {
            throw new RuntimeException("L'utilisateur ne fait pas partie de l'entreprise du magasin.");
        }

        // Vérification des droits de l'utilisateur (administrateur)
        if (!user.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("L'utilisateur n'a pas les droits nécessaires.");
        }

        // Mettre à jour le nom du magasin
        magasin.setNomMagasin(nouveauNomMagasin);

        // Sauvegarder les modifications dans la base de données
        return magasinRepository.save(magasin);
    }

    //Recupere Magasin par ID
    public Magasin getMagasinById(Long magasinId) {
        return magasinRepository.findById(magasinId)
                .orElseThrow(() -> new NotFoundException("Magasin non trouvé"));
    }

    //Recupere Magasin par son Nom
    public Magasin getMagasinByNom(String nomMagasin) {
        return magasinRepository.findByNomMagasin(nomMagasin)
                .orElseThrow(() -> new NotFoundException("Magasin avec ce nom non trouvé"));
    }


}
