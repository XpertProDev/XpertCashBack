package com.xpertcash.service;

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
