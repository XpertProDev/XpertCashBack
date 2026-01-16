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

        Claims claims = jwtUtil.extractAllClaimsSafe(token);
        if (claims == null) {
            throw new RuntimeException("Token invalide ou expiré. Veuillez vous reconnecter.");
        }

        String userUuid = claims.getSubject();
        if (userUuid == null || userUuid.trim().isEmpty()) {
            throw new RuntimeException("UUID utilisateur non trouvé dans le token");
        }

        Object sessionIdClaim = claims.get("sessionId");
        if (sessionIdClaim != null) {
            Long sessionId = ((Number) sessionIdClaim).longValue();
            var session = userSessionRepository.findById(sessionId);
            
            if (session.isEmpty() || !session.get().isActive() || session.get().isExpired()) {
                throw new RuntimeException("Session invalide ou expirée. Veuillez vous reconnecter.");
            }
            

            if (session.get().getSessionToken() != null && !token.equals(session.get().getSessionToken())) {
                throw new RuntimeException("Token ne correspond pas à la session. Veuillez vous reconnecter.");
            }
            
            
            LocalDateTime now = LocalDateTime.now();
            if (session.get().getLastActivity() == null || 
                session.get().getLastActivity().isBefore(now.minusMinutes(1))) {
                userSessionRepository.updateLastActivity(sessionId, now);
            }
        } else {
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

    @Deprecated
    public Long getAuthenticatedUserId(HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        return user.getId();
    }


    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String token = authHeader.substring(7);

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


    public User getAuthenticatedUserWithFallback(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String token = authHeader.substring(7);

        Claims claims = jwtUtil.extractAllClaimsSafe(token);
        if (claims == null) {
            throw new RuntimeException("Token invalide ou expiré. Veuillez vous reconnecter.");
        }

        try {
            String userUuid = claims.getSubject();
            if (userUuid != null && isUuidBasedToken(token)) {
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
                
                return usersRepository.findByUuid(userUuid)
                        .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));
            }
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Token révoqué") || e.getMessage().contains("Session invalide") || e.getMessage().contains("Token ne correspond pas")) {
                throw e;
            }
            System.out.println(" Échec extraction UUID, tentative avec ID legacy...");
        }
        
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
