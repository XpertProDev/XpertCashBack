package com.xpertcash.service;

import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.DTOs.UpdateEntrepriseDTO;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;



@Service
public class EntrepriseService {



    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ImageStorageService imageStorageService;


    // Méthode pour  Récupérer une entreprise par son id.
     
    public Entreprise getEntrepriseById(Long id) {
        return entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));
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
                System.out.println(" Ancien logo supprimé : " + oldLogoPath);
            } catch (IOException e) {
                System.out.println(" Impossible de supprimer l'ancien logo : " + e.getMessage());
            }
        }

        String newLogoUrl = imageStorageService.saveLogoImage(logoFile);
        entreprise.setLogo(newLogoUrl);
        System.out.println(" Nouveau logo enregistré : " + newLogoUrl);
    }


    if (imageSignatureFile != null && !imageSignatureFile.isEmpty()) {
        String oldLogoPath = entreprise.getSignaturNum();
        if (oldLogoPath != null && !oldLogoPath.isBlank()) {
            Path oldPath = Paths.get("src/main/resources/static" + oldLogoPath);
            try {
                Files.deleteIfExists(oldPath);
                System.out.println(" Ancien signature supprimé : " + oldLogoPath);
            } catch (IOException e) {
                System.out.println(" Impossible de supprimer l'ancien signature : " + e.getMessage());
            }
        }

        String newSignatureUrl = imageStorageService.SavesignatureNum(imageSignatureFile);
        entreprise.setSignaturNum(newSignatureUrl);
        System.out.println(" Nouveau signature enregistré : " + newSignatureUrl);
    }


     if (imageCachetFile != null && !imageCachetFile.isEmpty()) {
        String oldLogoPath = entreprise.getCachetNum();
        if (oldLogoPath != null && !oldLogoPath.isBlank()) {
            Path oldPath = Paths.get("src/main/resources/static" + oldLogoPath);
            try {
                Files.deleteIfExists(oldPath);
                System.out.println(" Ancien signature supprimé : " + oldLogoPath);
            } catch (IOException e) {
                System.out.println(" Impossible de supprimer l'ancien cachet : " + e.getMessage());
            }
        }

        String newCachetUrl = imageStorageService.SaveCachetNum(imageCachetFile);
        entreprise.setCachetNum(newCachetUrl);
        System.out.println(" Nouveau cachet enregistré : " + newCachetUrl);
    }


    System.out.println("DTO reçu dans le controller : " + dto);

    entrepriseRepository.save(entreprise);

    // Synchroniser téléphone et pays sur l'admin de l'entreprise pour que getEntrepriseOfConnectedUser() et le profil restent cohérents
    User admin = entreprise.getAdmin();
    if (admin != null) {
        boolean adminUpdated = false;
        if (dto.getTelephone() != null) {
            admin.setPhone(dto.getTelephone());
            adminUpdated = true;
        }
        if (dto.getPays() != null) {
            admin.setPays(dto.getPays());
            adminUpdated = true;
        }
        if (adminUpdated) {
            usersRepository.save(admin);
        }
    }
}

}
