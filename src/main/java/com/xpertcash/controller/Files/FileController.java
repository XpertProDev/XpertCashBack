package com.xpertcash.controller.Files;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.service.AuthenticationHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/auth/files")
public class FileController {

    @Autowired
    private AuthenticationHelper authHelper;

    private static final String STATIC_BASE_PATH = "src/main/resources/static";

 
    @GetMapping("/images/{folder}/{filename:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String folder,
            @PathVariable String filename,
            HttpServletRequest request) {
        
        try {
            authHelper.getAuthenticatedUserWithFallback(request);
            
            Path filePath = Paths.get(STATIC_BASE_PATH, folder, filename).normalize();
            
            Path staticPath = Paths.get(STATIC_BASE_PATH).normalize().toAbsolutePath();
            if (!filePath.toAbsolutePath().startsWith(staticPath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = detectContentType(filename, filePath);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("inline", filename);
            headers.setCacheControl("public, max-age=3600");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint pour obtenir l'URL du fichier (retourne du JSON)
     * Format: /api/auth/files/images/{folder}/{filename}?url=true
     */
    @GetMapping(value = "/images/{folder}/{filename:.+}", params = "url=true")
    public ResponseEntity<Map<String, Object>> getImageUrl(
            @PathVariable String folder,
            @PathVariable String filename,
            HttpServletRequest request) {
        
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            Entreprise entreprise = user.getEntreprise();
            
            Path filePath = Paths.get(STATIC_BASE_PATH, folder, filename).normalize();
            
            Path staticPath = Paths.get(STATIC_BASE_PATH).normalize().toAbsolutePath();
            if (!filePath.toAbsolutePath().startsWith(staticPath)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Accès non autorisé");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists() || !resource.isReadable()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Fichier introuvable");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            String baseUrl = getBaseUrl(request);
            String fileUrl = baseUrl + "/api/auth/files/images/" + folder + "/" + filename;
            
            String contentType = detectContentType(filename, filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("filename", filename);
            response.put("folder", folder);
            response.put("contentType", contentType);
            response.put("exists", true);
            
            if (entreprise != null) {
                Map<String, Object> entrepriseInfo = new HashMap<>();
                entrepriseInfo.put("id", entreprise.getId());
                entrepriseInfo.put("nomEntreprise", entreprise.getNomEntreprise());
                response.put("entreprise", entrepriseInfo);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(response);
                    
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Non authentifié");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Erreur interne");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Construit l'URL de base à partir de la requête HTTP
     */
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        
        if (contextPath != null && !contextPath.isEmpty()) {
            url.append(contextPath);
        }
        
        return url.toString();
    }

     // Détecte le type MIME d'un fichier à partir de son extension ou du contenu réel
   
    private String detectContentType(String filename, Path filePath) {
        try {
            int lastDotIndex = filename.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
                String extension = filename.substring(lastDotIndex + 1).toLowerCase();
                switch (extension) {
                    case "jpg":
                    case "jpeg":
                        return MediaType.IMAGE_JPEG_VALUE;
                    case "png":
                        return MediaType.IMAGE_PNG_VALUE;
                    case "gif":
                        return MediaType.IMAGE_GIF_VALUE;
                    case "webp":
                        return "image/webp";
                    case "svg":
                        return "image/svg+xml";
                    case "pdf":
                        return MediaType.APPLICATION_PDF_VALUE;
                    case "blob":
                        return detectContentTypeFromFile(filePath);
                    default:
                        String detected = Files.probeContentType(filePath);
                        if (detected != null) {
                            return detected;
                        }
                        return detectContentTypeFromFile(filePath);
                }
            } else {
       
                return detectContentTypeFromFile(filePath);
            }
        } catch (Exception e) {
            if (filePath.toString().contains("signatureUpload") || 
                filePath.toString().contains("cachetUpload") ||
                filePath.toString().contains("qrUpload")) {
                return MediaType.IMAGE_PNG_VALUE;
            }
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

     // Détecte le type MIME en lisant les magic bytes du fichier
    private String detectContentTypeFromFile(Path filePath) {
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length < 4) {
                return MediaType.IMAGE_PNG_VALUE;
            }

      
            if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return MediaType.IMAGE_PNG_VALUE;
            }
            
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
                return MediaType.IMAGE_JPEG_VALUE;
            }
            
            if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38) {
                return MediaType.IMAGE_GIF_VALUE;
            }
            
            if (bytes.length >= 12 && 
                bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 &&
                bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) {
                return "image/webp";
            }
            
            if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
                return MediaType.APPLICATION_PDF_VALUE;
            }
            
            String detected = Files.probeContentType(filePath);
            if (detected != null) {
                return detected;
            }
            
            String pathStr = filePath.toString();
            if (pathStr.contains("signatureUpload") || 
                pathStr.contains("cachetUpload") ||
                pathStr.contains("qrUpload") ||
                pathStr.contains("userUpload") ||
                pathStr.contains("clientUpload") ||
                pathStr.contains("uploads")) {
                return MediaType.IMAGE_PNG_VALUE;
            }
            
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (Exception e) {
            String pathStr = filePath.toString();
            if (pathStr.contains("signatureUpload") || 
                pathStr.contains("cachetUpload") ||
                pathStr.contains("qrUpload")) {
                return MediaType.IMAGE_PNG_VALUE;
            }
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
