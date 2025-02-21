package com.xpertcash.service.IMAGES;

import com.xpertcash.exceptions.CustomException;
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

            // Retourner l'URL de l'image
            return "/uploads/" + imageName;

        } catch (IOException e) {
            throw new NotFoundException("Erreur lors de l'enregistrement de l'image : " + e.getMessage());
        }
    }

}

