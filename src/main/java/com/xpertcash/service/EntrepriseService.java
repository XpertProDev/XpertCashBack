package com.xpertcash.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.repository.EntrepriseRepository;

@Service
public class EntrepriseService {

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    public List<EntrepriseDTO> getAllEntreprisesWithInfo() {
        // Récupérer toutes les entreprises
        List<Entreprise> entreprises = entrepriseRepository.findAll();

        if (entreprises.isEmpty()) {
            throw new RuntimeException("Aucune entreprise trouvée.");
        }

        // Mapper les entreprises en DTO
        List<EntrepriseDTO> entrepriseDTOs = entreprises.stream()
                .map(entreprise -> new EntrepriseDTO(
                        entreprise.getNomEntreprise(),
                        entreprise.getAdmin() != null ? entreprise.getAdmin().getNomComplet() : "Aucun Admin", 
                        entreprise.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return entrepriseDTOs;
    }
}
