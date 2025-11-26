package com.xpertcash.service;

import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.DTOs.UpdateEntrepriseDTO;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;



@Service
public class EntrepriseService {



    @Autowired
    private EntrepriseRepository entrepriseRepository;

     @Autowired
    private ImageStorageService imageStorageService;


    /**
     * Récupérer une entreprise par son id.
     */
    public Entreprise getEntrepriseById(Long id) {
        return entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));
    }

    /**
     * Activer ou désactiver une entreprise.
     * Si active == false, aucun utilisateur de cette entreprise ne devra pouvoir se connecter.
     */
    public void setEntrepriseActive(Long id, boolean active) {
        Entreprise entreprise = getEntrepriseById(id);
        entreprise.setActive(active);
        entrepriseRepository.save(entreprise);
    }

    /**
     * Désactiver une entreprise.
     */
    public void desactiverEntreprise(Long id) {
        setEntrepriseActive(id, false);
    }

    /**
     * Réactiver une entreprise.
     */
    public void activerEntreprise(Long id) {
        setEntrepriseActive(id, true);
    }


    public List<EntrepriseDTO> getAllEntreprisesWithInfo() {
        // Récupérer toutes les entreprises
        List<Entreprise> entreprises = entrepriseRepository.findAll();

        if (entreprises.isEmpty()) {
            throw new RuntimeException("Aucune entreprise trouvée.");
        }

        // Mapper les entreprises en DTO
        List<EntrepriseDTO> entrepriseDTOs = entreprises.stream()
                .map(entreprise -> new EntrepriseDTO(
                        entreprise.getId(),
                        entreprise.getNomEntreprise(),
                        entreprise.getAdmin() != null ? entreprise.getAdmin().getNomComplet() : "Aucun Admin", 
                        entreprise.getCreatedAt(),
                        entreprise.getAdresse(),
                        entreprise.getLogo(),
                        entreprise.getSiege(),
                        entreprise.getNina(),
                        entreprise.getNif(),
                        entreprise.getBanque(),
                        entreprise.getEmail(),
                        entreprise.getTelephone(),
                        entreprise.getPays(),
                        entreprise.getSecteur(),
                        entreprise.getRccm(),
                        entreprise.getSiteWeb(),
                        entreprise.getSignataire(),
                        entreprise.getSignataireNom(),
                        entreprise.getPrefixe(),
                        entreprise.getSuffixe(),
                        entreprise.getTauxTva(),
                        entreprise.getSignaturNum(),
                        entreprise.getCachetNum()

                ))
                .collect(Collectors.toList());

        return entrepriseDTOs;
    }

   
    @Transactional
    public void updateEntreprise(Long id, UpdateEntrepriseDTO dto, MultipartFile logoFile,
    MultipartFile imageSignatureFile, MultipartFile imageCachetFile) {
    Entreprise entreprise = entrepriseRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));

    if (dto.getNom() != null) {
        entreprise.setNomEntreprise(dto.getNom());
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

    if (dto.getEmail() != null) {
            entreprise.setEmail(dto.getEmail());
    }

    if (dto.getTelephone() != null) {
        entreprise.setTelephone(dto.getTelephone());
    }
    
    if (dto.getPays() != null) {
        entreprise.setPays(dto.getPays());
    }

    if (dto.getSecteur() != null) {
        entreprise.setSecteur(dto.getSecteur());
    }

    if (dto.getRccm() != null) {
        entreprise.setRccm(dto.getRccm());
    }

    if (dto.getSiteWeb() != null) {
        entreprise.setSiteWeb(dto.getSiteWeb());
    }
    if (dto.getSignataire() != null) {
        entreprise.setSignataire(dto.getSignataire());
    }
    if (dto.getSignataireNom() != null) {
        entreprise.setSignataireNom(dto.getSignataireNom());
    }
    if (dto.getPrefixe() != null) {
        entreprise.setPrefixe(dto.getPrefixe());
    }
     if (dto.getSuffixe() != null) {
        entreprise.setSuffixe(dto.getSuffixe());
    }
    if (dto.getTauxTva() != null) {
        entreprise.setTauxTva(dto.getTauxTva());
    }

        if (logoFile != null && !logoFile.isEmpty()) {
        String oldLogoPath = entreprise.getLogo();
        if (oldLogoPath != null && !oldLogoPath.isBlank()) {
            Path oldPath = Paths.get("src/main/resources/static" + oldLogoPath);
            try {
                Files.deleteIfExists(oldPath);
                System.out.println("🗑️ Ancien logo supprimé : " + oldLogoPath);
            } catch (IOException e) {
                System.out.println("⚠️ Impossible de supprimer l'ancien logo : " + e.getMessage());
            }
        }

        // Sauvegarde du nouveau logo
        String newLogoUrl = imageStorageService.saveLogoImage(logoFile);
        entreprise.setLogo(newLogoUrl);
        System.out.println("📸 Nouveau logo enregistré : " + newLogoUrl);
    }


    if (imageSignatureFile != null && !imageSignatureFile.isEmpty()) {
        String oldLogoPath = entreprise.getSignaturNum();
        if (oldLogoPath != null && !oldLogoPath.isBlank()) {
            Path oldPath = Paths.get("src/main/resources/static" + oldLogoPath);
            try {
                Files.deleteIfExists(oldPath);
                System.out.println("🗑️ Ancien signature supprimé : " + oldLogoPath);
            } catch (IOException e) {
                System.out.println("⚠️ Impossible de supprimer l'ancien signature : " + e.getMessage());
            }
        }

        // Sauvegarde du nouveau signature
        String newSignatureUrl = imageStorageService.SavesignatureNum(imageSignatureFile);
        entreprise.setSignaturNum(newSignatureUrl);
        System.out.println("📸 Nouveau signature enregistré : " + newSignatureUrl);
    }


     if (imageCachetFile != null && !imageCachetFile.isEmpty()) {
        String oldLogoPath = entreprise.getCachetNum();
        if (oldLogoPath != null && !oldLogoPath.isBlank()) {
            Path oldPath = Paths.get("src/main/resources/static" + oldLogoPath);
            try {
                Files.deleteIfExists(oldPath);
                System.out.println("🗑️ Ancien signature supprimé : " + oldLogoPath);
            } catch (IOException e) {
                System.out.println("⚠️ Impossible de supprimer l'ancien cachet : " + e.getMessage());
            }
        }

        // Sauvegarde du nouveau signature
        String newCachetUrl = imageStorageService.SaveCachetNum(imageCachetFile);
        entreprise.setCachetNum(newCachetUrl);
        System.out.println("📸 Nouveau cachet enregistré : " + newCachetUrl);
    }


    System.out.println("DTO reçu dans le controller : " + dto);


    entrepriseRepository.save(entreprise);
}

}
