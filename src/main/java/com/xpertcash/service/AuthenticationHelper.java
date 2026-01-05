package com.xpertcash.service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.UserSessionRepository;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.time.ZoneId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service helper pour gérer l'authentification avec UUID
 * Ce service facilite la transition et centralise la logique d'extraction d'utilisateur
 * Supporte maintenant la gestion des sessions multiples par appareil
 */
@Service
public class AuthenticationHelper {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    /**
     * Extrait l'utilisateur authentifié depuis la requête HTTP
     * Utilise l'UUID du token JWT pour récupérer l'utilisateur
     * Le token est déjà validé (signature, expiration, révocation) dans extractTokenFromRequest
     * Optimisé : vérifie d'abord la session si sessionId présent, sinon fallback sur lastActivity
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

        // 🔒 Récupérer l'UUID utilisateur
        String userUuid = claims.getSubject();
        if (userUuid == null || userUuid.trim().isEmpty()) {
            throw new RuntimeException("UUID utilisateur non trouvé dans le token");
        }

        // 🔒 Vérifier la session si sessionId présent (nouveau système)
        Object sessionIdClaim = claims.get("sessionId");
        if (sessionIdClaim != null) {
            Long sessionId = ((Number) sessionIdClaim).longValue();
            var session = userSessionRepository.findById(sessionId);
            
            if (session.isEmpty() || !session.get().isActive() || session.get().isExpired()) {
                throw new RuntimeException("Session invalide ou expirée. Veuillez vous reconnecter.");
            }
            
            // Vérifier que le token correspond à la session (si sessionToken n'est pas null)
            // Permet de gérer les cas où le token n'a pas encore été mis à jour
            if (session.get().getSessionToken() != null && !token.equals(session.get().getSessionToken())) {
                throw new RuntimeException("Token ne correspond pas à la session. Veuillez vous reconnecter.");
            }
            
            // Mettre à jour la dernière activité de la session (optimisé : query directe)
            // On met à jour seulement si la dernière activité date de plus de 1 minute (évite trop de requêtes)
            LocalDateTime now = LocalDateTime.now();
            if (session.get().getLastActivity() == null || 
                session.get().getLastActivity().isBefore(now.minusMinutes(1))) {
                userSessionRepository.updateLastActivity(sessionId, now);
            }
        } else {
            // 🔒 Fallback : vérifier lastActivity pour les anciens tokens (compatibilité)
        User user = usersRepository.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));

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

        // Récupérer l'utilisateur
        User user = usersRepository.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));

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
     * Optimisé : vérifie d'abord la session, puis lastActivity pour compatibilité
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

        // Essayer d'abord avec UUID et session
        try {
            String userUuid = claims.getSubject();
            if (userUuid != null && isUuidBasedToken(token)) {
                // Vérifier la session si sessionId présent
                Object sessionIdClaim = claims.get("sessionId");
                if (sessionIdClaim != null) {
                    Long sessionId = ((Number) sessionIdClaim).longValue();
                    var session = userSessionRepository.findById(sessionId);
                    
                    if (session.isEmpty() || !session.get().isActive() || session.get().isExpired()) {
                        throw new RuntimeException("Session invalide ou expirée. Veuillez vous reconnecter.");
                    }
                    
                    if (!token.equals(session.get().getSessionToken())) {
                        throw new RuntimeException("Token ne correspond pas à la session. Veuillez vous reconnecter.");
                    }
                    
                    session.get().updateLastActivity();
                    userSessionRepository.save(session.get());
                } else {
                    // Fallback : vérifier lastActivity pour les anciens tokens
                User user = usersRepository.findByUuid(userUuid)
                        .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));
                
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
                
                // Récupérer l'utilisateur après validation de session
                return usersRepository.findByUuid(userUuid)
                        .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));
            }
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Token révoqué") || e.getMessage().contains("Session invalide") || e.getMessage().contains("Token ne correspond pas")) {
                throw e; // Propager l'erreur de révocation
            }
            System.out.println("⚠️ Échec extraction UUID, tentative avec ID legacy...");
        }
        
        // Fallback vers l'ancienne méthode avec ID (pas de vérification pour compatibilité)
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
