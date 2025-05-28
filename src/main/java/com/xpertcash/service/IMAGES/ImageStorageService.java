package com.xpertcash.service.IMAGES;

import com.xpertcash.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final String imageLocation = "src/main/resources/static/uploads";

    public String saveImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new NotFoundException("Le fichier image est vide ou invalide.");
        }

        try {
            // Emplacement de l'image dans le dossier static/uploads
            Path imageRootLocation = Paths.get("src/main/resources/static/uploads");

            // Vérifie si le dossier existe, sinon le crée
            if (!Files.exists(imageRootLocation)) {
                Files.createDirectories(imageRootLocation);
            }

            String imageName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
            Path imagePath = imageRootLocation.resolve(imageName);
            Files.copy(imageFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/uploads/" + imageName;
            System.out.println("✅ Image sauvegardée : " + imageUrl);
            // Retourner l'URL de l'image
            return "/uploads/" + imageName;


        } catch (IOException e) {
            System.out.println("❌ ERREUR lors de l'enregistrement de l'image : " + e.getMessage());
            throw new NotFoundException("Erreur lors de l'enregistrement de l'image : " + e.getMessage());
        }
    }


    public String saveLogoImage(MultipartFile imageLogoFile) {
        if (imageLogoFile == null || imageLogoFile.isEmpty()) {
            throw new NotFoundException("Le fichier image est vide ou invalide.");
        }

        try {
            // Emplacement de l'image dans le dossier static/logoUpload
            Path imageRootLocation = Paths.get("src/main/resources/static/logoUpload");

            // Vérifie si le dossier existe, sinon le crée
            if (!Files.exists(imageRootLocation)) {
                Files.createDirectories(imageRootLocation);
            }

            String imageName = UUID.randomUUID().toString() + "_" + imageLogoFile.getOriginalFilename();
            Path imagePath = imageRootLocation.resolve(imageName);
            Files.copy(imageLogoFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/logoUpload/" + imageName;
            System.out.println("✅ Logo sauvegardée : " + imageUrl);
            // Retourner l'URL de l'image
            return "/logoUpload/" + imageName;


        } catch (IOException e) {
            System.out.println("❌ ERREUR lors de l'enregistrement de logo : " + e.getMessage());
            throw new NotFoundException("Erreur lors de l'enregistrement de logo : " + e.getMessage());
        }
    }

    // Gestion image pour les utilisateurs
    public String saveUserImage(MultipartFile imageUserFile) {
        if (imageUserFile == null || imageUserFile.isEmpty()) {
            throw new NotFoundException("Le fichier image est vide ou invalide.");
        }
        try {
            Path imageRootLocation = Paths.get("src/main/resources/static/userUpload");
            // Vérifie si le dossier existe, sinon le crée
            if (!Files.exists(imageRootLocation)) {
                Files.createDirectories(imageRootLocation);
            }

            String imageName = UUID.randomUUID().toString() + "_" + imageUserFile.getOriginalFilename();
            Path imagePath = imageRootLocation.resolve(imageName);
            Files.copy(imageUserFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/userUpload/" + imageName;
            System.out.println("✅ Image utilisateur sauvegardée : " + imageUrl);
            return "/userUpload/" + imageName;
        } catch (IOException e) {
            System.out.println("❌ ERREUR lors de l'enregistrement de l'image utilisateur : " + e.getMessage());
            throw new NotFoundException("Erreur lors de l'enregistrement de l'image utilisateur : " + e.getMessage());
        }
    }
}

