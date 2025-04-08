package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;

@Service
public class EntrepriseClientService {

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

    @Autowired
    private ClientRepository clientRepository;

    public EntrepriseClient saveEntreprise(EntrepriseClient entrepriseClient) {
        // Vérifier si une entreprise avec le même email ou téléphone existe déjà
        Optional<EntrepriseClient> existingEntreprise = entrepriseClientRepository
            .findByEmailOrTelephone(entrepriseClient.getEmail(), entrepriseClient.getTelephone());

        if (existingEntreprise.isPresent()) {
            throw new RuntimeException("Une entreprise avec les même informations existe déjà !");
        }

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




}
