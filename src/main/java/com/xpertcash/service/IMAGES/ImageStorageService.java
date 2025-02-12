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

    private final String imageLocation = "C:\\xampp\\htdocs\\xpertCashFile\\images"; // Chemin à rendre configurable

    public String saveImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new NotFoundException("Le fichier image est vide ou invalide.");
        }

        try {
            Path imageRootLocation = Paths.get(imageLocation);
            if (!Files.exists(imageRootLocation)) {
                Files.createDirectories(imageRootLocation);
            }

            String imageName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
            Path imagePath = imageRootLocation.resolve(imageName);
            Files.copy(imageFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

            return "http://localhost/xpertCashFile/images/" + imageName;

        } catch (IOException e) {
            throw new NotFoundException("Erreur lors de l'enregistrement de l'image : " + e.getMessage());
        }
    }
}

