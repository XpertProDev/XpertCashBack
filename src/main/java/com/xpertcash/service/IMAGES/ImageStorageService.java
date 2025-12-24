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

    //Gestion image pour les clients
    public String saveClientImage(MultipartFile imageClientFile) {
        if (imageClientFile == null || imageClientFile.isEmpty()) {
            throw new NotFoundException("Le fichier image est vide ou invalide.");
        }
        try {
            Path imageRootLocation = Paths.get("src/main/resources/static/clientUpload");
            // Vérifie si le dossier existe, sinon le crée
            if (!Files.exists(imageRootLocation)) {
                Files.createDirectories(imageRootLocation);
            }

            String imageName = UUID.randomUUID().toString() + "_" + imageClientFile.getOriginalFilename();
            Path imagePath = imageRootLocation.resolve(imageName);
            Files.copy(imageClientFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/clientUpload/" + imageName;
            System.out.println("✅ Image client sauvegardée : " + imageUrl);
            return "/clientUpload/" + imageName;
        } catch (IOException e) {
            System.out.println("❌ ERREUR lors de l'enregistrement de l'image client : " + e.getMessage());
            throw new NotFoundException("Erreur lors de l'enregistrement de l'image client : " + e.getMessage());
        }
    }
    
    //Gestion image pour fournisseurs
    public String saveFournisseurImage(MultipartFile imageFournisseurFile) {
        if (imageFournisseurFile == null || imageFournisseurFile.isEmpty())
            throw new NotFoundException("Le fichier image est vide ou invalide.");
            try {
            Path imageRootLocation = Paths.get("src/main/resources/static/fournisseurUpload");
            // Vérifie si le dossier existe, sinon le crée
            if (!Files.exists(imageRootLocation)) {
                Files.createDirectories(imageRootLocation);
            }
            String imageName = UUID.randomUUID().toString() + "_" + imageFournisseurFile.getOriginalFilename();
            Path imagePath = imageRootLocation.resolve(imageName);
            Files.copy(imageFournisseurFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
            String imageUrl = "/fournisseurUpload/" + imageName;
            System.out.println("✅ Image fournisseur sauvegardée : " + imageUrl);
            return "/fournisseurUpload/" + imageName;
        } catch (IOException e) {
            System.out.println("❌ ERREUR lors de l'enregistrement de l'image fournisseur : " + e.getMessage());
            throw new NotFoundException("Erreur lors de l'enregistrement de l'image fournisseur : " + e.getMessage());
        }

    }


    //Sinature numerique

    public String SavesignatureNum(MultipartFile imageSignatureFile) {
        if (imageSignatureFile == null || imageSignatureFile.isEmpty()) {
            throw new NotFoundException("Le fichier image est vide ou invalide.");
        }

        try {
            // Emplacement de l'image dans le dossier static/signatureUpload
            Path imageRootLocation = Paths.get("src/main/resources/static/signatureUpload");
            if (!Files.exists(imageRootLocation)) {
                Files.createDirectories(imageRootLocation);
            }
            String imageName = UUID.randomUUID().toString() + "_" + imageSignatureFile.getOriginalFilename();
            Path imagePath = imageRootLocation.resolve(imageName);
            Files.copy(imageSignatureFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
            String imageUrl = "/signatureUpload/" + imageName;
            System.out.println("✅ Signature sauvegardée : " + imageUrl);
            return "/signatureUpload/" + imageName;
        } catch (IOException e) {
            System.out.println("❌ ERREUR lors de l'enregistrement de signature : " + e.getMessage());
            throw new NotFoundException("Erreur lors de l'enregistrement de signature : " + e.getMessage());
        }
    }


    //Cachet numerique
     public String SaveCachetNum(MultipartFile imageCachetFile) {
        if (imageCachetFile == null || imageCachetFile.isEmpty()) {
            throw new NotFoundException("Le fichier image est vide ou invalide.");
        }

        try {
            // Emplacement de l'image dans le dossier static/cachetUpload
            Path imageRootLocation = Paths.get("src/main/resources/static/cachetUpload");
            if (!Files.exists(imageRootLocation)) {
                Files.createDirectories(imageRootLocation);
            }
            String imageName = UUID.randomUUID().toString() + "_" + imageCachetFile.getOriginalFilename();
            Path imagePath = imageRootLocation.resolve(imageName);
            Files.copy(imageCachetFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
            String imageUrl = "/cachetUpload/" + imageName;
            System.out.println("✅ Cachet sauvegardée : " + imageUrl);
            return "/cachetUpload/" + imageName;
        } catch (IOException e) {
            System.out.println("❌ ERREUR lors de l'enregistrement de signature : " + e.getMessage());
            throw new NotFoundException("Erreur lors de l'enregistrement de cachet : " + e.getMessage());
        }
    }


    //code qr
    public String saveQrCodeImage(byte[] qrCodeBytes, String fileNameHint) {
    try {
        // Emplacement du QR code dans /static/qrUpload
        Path qrRootLocation = Paths.get("src/main/resources/static/qrUpload");

        // Vérifie si le dossier existe, sinon le crée
        if (!Files.exists(qrRootLocation)) {
            Files.createDirectories(qrRootLocation);
        }

        // Générer un nom de fichier unique
        String imageName = UUID.randomUUID().toString() + "_" + fileNameHint + ".png";
        Path imagePath = qrRootLocation.resolve(imageName);

        // Sauvegarder le fichier
        Files.write(imagePath, qrCodeBytes);

        String imageUrl = "/qrUpload/" + imageName;
        System.out.println("✅ QR Code sauvegardé : " + imageUrl);

        // Retourner l’URL du fichier
        return imageUrl;

    } catch (IOException e) {
        System.out.println("❌ ERREUR lors de l'enregistrement du QR Code : " + e.getMessage());
        throw new RuntimeException("Erreur lors de l'enregistrement du QR Code : " + e.getMessage());
    }
}

    public void deleteQrCodeImage(String qrCodeUrl) {
    try {
        String fileName = Paths.get(qrCodeUrl).getFileName().toString();

        Path qrRootLocation = Paths.get("src/main/resources/static/qrUpload");
        Path imagePath = qrRootLocation.resolve(fileName);

        // Vérifier si le fichier existe avant de supprimer
        if (Files.exists(imagePath)) {
            Files.delete(imagePath);
            System.out.println("✅ QR Code supprimé : " + fileName);
        } else {
            System.out.println("⚠️ Fichier QR Code introuvable : " + fileName);
        }

    } catch (IOException e) {
        System.err.println("❌ Erreur lors de la suppression du QR Code : " + e.getMessage());
        throw new RuntimeException("Impossible de supprimer le QR Code : " + e.getMessage(), e);
    }
}

    // Gestion pièce jointe pour les dépenses générales
    public String saveDepensePieceJointe(MultipartFile pieceJointeFile) {
        if (pieceJointeFile == null || pieceJointeFile.isEmpty()) {
            throw new NotFoundException("Le fichier pièce jointe est vide ou invalide.");
        }
        try {
            Path depenseRootLocation = Paths.get("src/main/resources/static/depenseUpload");
            // Vérifie si le dossier existe, sinon le crée
            if (!Files.exists(depenseRootLocation)) {
                Files.createDirectories(depenseRootLocation);
            }

            String fileName = UUID.randomUUID().toString() + "_" + pieceJointeFile.getOriginalFilename();
            Path filePath = depenseRootLocation.resolve(fileName);
            Files.copy(pieceJointeFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/depenseUpload/" + fileName;
            System.out.println("✅ Pièce jointe dépense sauvegardée : " + fileUrl);
            return fileUrl;
        } catch (IOException e) {
            System.out.println("❌ ERREUR lors de l'enregistrement de la pièce jointe : " + e.getMessage());
            throw new NotFoundException("Erreur lors de l'enregistrement de la pièce jointe : " + e.getMessage());
        }
    }

    // Gestion pièce jointe pour les transferts de fonds
    public String saveTransfertPieceJointe(MultipartFile pieceJointeFile) {
        if (pieceJointeFile == null || pieceJointeFile.isEmpty()) {
            throw new NotFoundException("Le fichier pièce jointe est vide ou invalide.");
        }
        try {
            Path transfertRootLocation = Paths.get("src/main/resources/static/transfertUpload");
            // Vérifie si le dossier existe, sinon le crée
            if (!Files.exists(transfertRootLocation)) {
                Files.createDirectories(transfertRootLocation);
            }

            String fileName = UUID.randomUUID().toString() + "_" + pieceJointeFile.getOriginalFilename();
            Path filePath = transfertRootLocation.resolve(fileName);
            Files.copy(pieceJointeFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/transfertUpload/" + fileName;
            System.out.println("✅ Pièce jointe transfert sauvegardée : " + fileUrl);
            return fileUrl;
        } catch (IOException e) {
            System.out.println("❌ ERREUR lors de l'enregistrement de la pièce jointe transfert : " + e.getMessage());
            throw new NotFoundException("Erreur lors de l'enregistrement de la pièce jointe transfert : " + e.getMessage());
        }
    }

}

