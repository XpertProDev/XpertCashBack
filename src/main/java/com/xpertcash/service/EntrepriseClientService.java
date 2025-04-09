package com.xpertcash.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Client;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class EntrepriseClientService {

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

    @Autowired
    private ClientRepository clientRepository;

    public EntrepriseClient saveEntreprise(EntrepriseClient entrepriseClient) {
        // Vérification que le nom est renseigné
        if (entrepriseClient.getNom() == null || entrepriseClient.getNom().trim().isEmpty()) {
            throw new RuntimeException("Le nom de l'entreprise est obligatoire !");
        }
    
        String email = entrepriseClient.getEmail();
        String telephone = entrepriseClient.getTelephone();
    
        Optional<EntrepriseClient> existingByEmail = Optional.empty();
        Optional<EntrepriseClient> existingByTelephone = Optional.empty();
    
        // Vérifier si l'email est renseigné et existe déjà
        if (email != null && !email.isEmpty()) {
            existingByEmail = entrepriseClientRepository.findByEmail(email);
        }
    
        // Vérifier si le téléphone est renseigné et existe déjà
        if (telephone != null && !telephone.isEmpty()) {
            existingByTelephone = entrepriseClientRepository.findByTelephone(telephone);
        }
    
        // Construire un message d'erreur précis
        if (existingByEmail.isPresent() && existingByTelephone.isPresent()) {
            throw new RuntimeException("Une entreprise avec cet email et ce téléphone existe déjà !");
        } else if (existingByEmail.isPresent()) {
            throw new RuntimeException("Une entreprise avec cet email existe déjà !");
        } else if (existingByTelephone.isPresent()) {
            throw new RuntimeException("Une entreprise avec ce téléphone existe déjà !");
        }
    
        // Enregistrement de la nouvelle entreprise
        entrepriseClient.setCreatedAt(LocalDateTime.now());
        return entrepriseClientRepository.save(entrepriseClient);
    }
    

    public Optional<EntrepriseClient> getEntrepriseById(Long id) {
        return entrepriseClientRepository.findById(id);
    }

    public List<EntrepriseClient> getAllEntreprises() {
        return entrepriseClientRepository.findAll();
    }

    public void deleteEntreprise(Long id) { 
        entrepriseClientRepository.deleteById(id);
    }



     //Methode pour modifier une Entreprise client
    public EntrepriseClient updateEntrepriseClient(EntrepriseClient entrepriseClient) {
        if (entrepriseClient.getId() == null) {
            throw new IllegalArgumentException("L'ID d'entreprise est obligatoire !");
        }
    
        //  si l'entreprise client existe
        Optional<EntrepriseClient> existingEntrepriseClient = entrepriseClientRepository.findById(entrepriseClient.getId());
        if (existingEntrepriseClient.isEmpty()) {
            throw new EntityNotFoundException("L'entreprise avec cet ID n'existe pas !");
        }
    
        EntrepriseClient updateEntrepriseClient = existingEntrepriseClient.get();
    
        // Utilisation de la réflexion pour mettre à jour seulement les champs non null
        for (Field field : EntrepriseClient.class.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object newValue = field.get(entrepriseClient);
                if (newValue != null) {
                    field.set(updateEntrepriseClient, newValue);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    
        // Enregistrer les modifications
        return entrepriseClientRepository.save(updateEntrepriseClient);
    }



}
