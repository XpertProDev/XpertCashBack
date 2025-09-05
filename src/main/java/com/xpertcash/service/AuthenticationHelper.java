package com.xpertcash.service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service helper pour gérer l'authentification avec UUID
 * Ce service facilite la transition et centralise la logique d'extraction d'utilisateur
 */
@Service
public class AuthenticationHelper {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsersRepository usersRepository;

    /**
     * Extrait l'utilisateur authentifié depuis la requête HTTP
     * Utilise l'UUID du token JWT pour récupérer l'utilisateur
     */
    public User getAuthenticatedUser(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        String userUuid = jwtUtil.extractUserUuid(token);
        
        if (userUuid == null) {
            throw new RuntimeException("UUID utilisateur non trouvé dans le token");
        }

        return usersRepository.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));
    }

    /**
     * Extrait l'UUID de l'utilisateur depuis la requête HTTP
     */
    public String getAuthenticatedUserUuid(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        String userUuid = jwtUtil.extractUserUuid(token);
        
        if (userUuid == null) {
            throw new RuntimeException("UUID utilisateur non trouvé dans le token");
        }
        
        return userUuid;
    }

    /**
     * Méthode LEGACY - Extrait l'ID de l'utilisateur depuis la requête HTTP
     * @deprecated Utiliser getAuthenticatedUserUuid() à la place
     */
    @Deprecated
    public Long getAuthenticatedUserId(HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        return user.getId();
    }

    /**
     * Extrait le token JWT depuis l'header Authorization
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        return authHeader.substring(7); // Enlever "Bearer "
    }

    /**
     * Vérifie si un token contient un UUID valide
     */
    public boolean isUuidBasedToken(String token) {
        try {
            String subject = jwtUtil.extractUserUuid(token);
            return subject != null && subject.length() == 36 && subject.contains("-");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Méthode de transition - essaie UUID d'abord, puis ID si échec
     * À utiliser temporairement pendant la migration
     */
    public User getAuthenticatedUserWithFallback(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        
        // Essayer d'abord avec UUID
        try {
            String userUuid = jwtUtil.extractUserUuid(token);
            if (userUuid != null && isUuidBasedToken(token)) {
                return usersRepository.findByUuid(userUuid)
                        .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));
            }
        } catch (Exception e) {
            System.out.println("⚠️ Échec extraction UUID, tentative avec ID legacy...");
        }
        
        // Fallback vers l'ancienne méthode avec ID
        try {
            Long userId = jwtUtil.extractUserId(token);
            if (userId != null) {
                return usersRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec ID: " + userId));
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'extraire l'utilisateur du token", e);
        }
        
        throw new RuntimeException("Token invalide - ni UUID ni ID trouvé");
    }
}
