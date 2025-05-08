package com.xpertcash.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.DTOs.UpdateEntrepriseDTO;
import com.xpertcash.configuration.JwtConfig;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.RoleRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class EntrepriseService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
     @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private JwtUtil jwtUtil; 

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
                        entreprise.getCreatedAt(),
                        entreprise.getAdresse(),
                        entreprise.getLogo(),
                        entreprise.getSiege(),
                        entreprise.getNina(),
                        entreprise.getNif(),
                        entreprise.getBanque()
                ))
                .collect(Collectors.toList());

        return entrepriseDTOs;
    }

   
    @Transactional
    public void updateEntreprise(Long id, UpdateEntrepriseDTO dto, MultipartFile logoFile) {
    Entreprise entreprise = entrepriseRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));

    if (dto.getNomEntreprise() != null) {
        entreprise.setNomEntreprise(dto.getNomEntreprise());
    }

    if (dto.getAdresse() != null) {
        entreprise.setAdresse(dto.getAdresse());
    }

    if (dto.getSiege() != null) {
        entreprise.setSiege(dto.getSiege());
    }

    if (dto.getNina() != null) {
        entreprise.setNina(dto.getNina());
    }

    if (dto.getNif() != null) {
        entreprise.setNif(dto.getNif());
    }

    if (dto.getBanque() != null) {
        entreprise.setBanque(dto.getBanque());
    }


   if (logoFile != null && !logoFile.isEmpty()) {
    String logo = imageStorageService.saveLogoImage(logoFile);
    entreprise.setLogo(logo);
    System.out.println("📸 URL de logo enregistrée : " + logo);
}


    System.out.println("donner de dto : " + dto);

    entrepriseRepository.save(entreprise);
}

}
