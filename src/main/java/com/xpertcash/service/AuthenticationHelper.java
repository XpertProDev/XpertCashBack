package com.xpertcash.service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import io.jsonwebtoken.Claims;
import java.time.ZoneId;
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
     * Le token est déjà validé (signature, expiration, révocation) dans extractTokenFromRequest
     * Optimisé : récupère l'utilisateur une seule fois et vérifie lastActivity en même temps
     */
    public User getAuthenticatedUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String token = authHeader.substring(7);

        // 🔒 Valider la signature et l'expiration du token
        Claims claims = jwtUtil.extractAllClaimsSafe(token);
        if (claims == null) {
            throw new RuntimeException("Token invalide ou expiré. Veuillez vous reconnecter.");
        }

        // 🔒 Récupérer l'utilisateur une seule fois et vérifier lastActivity
        String userUuid = claims.getSubject();
        if (userUuid == null || userUuid.trim().isEmpty()) {
            throw new RuntimeException("UUID utilisateur non trouvé dans le token");
        }

        User user = usersRepository.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));

        // 🔒 Vérifier que le token n'a pas été invalidé par un logout
        if (user.getLastActivity() != null) {
            Object lastActivityClaim = claims.get("lastActivity");
            if (lastActivityClaim != null) {
                long tokenLastActivity = ((Number) lastActivityClaim).longValue();
                long userLastActivity = user.getLastActivity()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                
                // Si le lastActivity de l'utilisateur est plus récent que celui du token,
                // cela signifie que l'utilisateur s'est déconnecté (logout) et le token est invalide
                if (userLastActivity > tokenLastActivity) {
                    throw new RuntimeException("Token révoqué. Veuillez vous reconnecter.");
                }
            }
        }

        return user;
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
     * Valide la signature et l'expiration (mais pas lastActivity pour éviter double requête)
     * La vérification lastActivity est faite dans getAuthenticatedUser pour optimiser
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String token = authHeader.substring(7);

        // 🔒 Valider la signature et l'expiration du token
        Claims claims = jwtUtil.extractAllClaimsSafe(token);
        if (claims == null) {
            throw new RuntimeException("Token invalide ou expiré. Veuillez vous reconnecter.");
        }

        return token;
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
     * Optimisé : vérifie lastActivity pour éviter les tokens révoqués
     */
    public User getAuthenticatedUserWithFallback(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String token = authHeader.substring(7);

        // 🔒 Valider la signature et l'expiration du token
        Claims claims = jwtUtil.extractAllClaimsSafe(token);
        if (claims == null) {
            throw new RuntimeException("Token invalide ou expiré. Veuillez vous reconnecter.");
        }

        // Essayer d'abord avec UUID
        try {
            String userUuid = claims.getSubject();
            if (userUuid != null && isUuidBasedToken(token)) {
                User user = usersRepository.findByUuid(userUuid)
                        .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));
                
                // Vérifier lastActivity
                if (user.getLastActivity() != null) {
                    Object lastActivityClaim = claims.get("lastActivity");
                    if (lastActivityClaim != null) {
                        long tokenLastActivity = ((Number) lastActivityClaim).longValue();
                        long userLastActivity = user.getLastActivity()
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli();
                        if (userLastActivity > tokenLastActivity) {
                            throw new RuntimeException("Token révoqué. Veuillez vous reconnecter.");
                        }
                    }
                }
                return user;
            }
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Token révoqué")) {
                throw e; // Propager l'erreur de révocation
            }
            System.out.println("⚠️ Échec extraction UUID, tentative avec ID legacy...");
        }
        
        // Fallback vers l'ancienne méthode avec ID (pas de vérification lastActivity pour compatibilité)
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
