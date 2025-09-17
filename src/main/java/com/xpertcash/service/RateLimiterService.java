package com.xpertcash.service;

import com.xpertcash.configuration.RateLimit;
import com.xpertcash.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service de Rate Limiting utilisant Redis
 * 
 * Ce service implémente l'algorithme de rate limiting "Sliding Window"
 * pour contrôler le nombre de requêtes par utilisateur/IP dans une fenêtre de temps.
 */
@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);
    
    @Autowired
    private RedisTemplate<String, String> rateLimitRedisTemplate;
    
    @Autowired
    private AuthenticationHelper authHelper;

    /**
     * Vérifie si une requête est autorisée selon les limites définies
     * 
     * @param rateLimit annotation de configuration
     * @param requestKey clé unique pour identifier la requête (IP, user, etc.)
     * @return true si la requête est autorisée, false sinon
     */
    public boolean isRequestAllowed(RateLimit rateLimit, String requestKey) {
        if (!rateLimit.enabled()) {
            return true;
        }

        try {
            String redisKey = buildRedisKey(rateLimit, requestKey);
            int requests = rateLimit.requests();
            Duration window = parseWindow(rateLimit.window());
            
            // Utilisation de l'algorithme Sliding Window avec Redis
            return checkSlidingWindow(redisKey, requests, window);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification du rate limit pour la clé: {}", requestKey, e);
            // En cas d'erreur, on autorise la requête pour éviter de bloquer le service
            return true;
        }
    }

    /**
     * Construit la clé Redis pour le rate limiting
     */
    private String buildRedisKey(RateLimit rateLimit, String requestKey) {
        String prefix = rateLimit.prefix();
        String window = rateLimit.window();
        String keyType = rateLimit.key();
        
        // Format: prefix:keyType:requestKey:window
        return String.format("%s:%s:%s:%s", prefix, keyType, requestKey, window);
    }

    /**
     * Parse la fenêtre de temps (ex: "1h", "30m", "5s")
     */
    private Duration parseWindow(String window) {
        if (window.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(window.substring(0, window.length() - 1)));
        } else if (window.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(window.substring(0, window.length() - 1)));
        } else if (window.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(window.substring(0, window.length() - 1)));
        } else if (window.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(window.substring(0, window.length() - 1)));
        } else {
            throw new IllegalArgumentException("Format de fenêtre non supporté: " + window);
        }
    }

    /**
     * Implémentation de l'algorithme Sliding Window
     * Utilise des timestamps pour maintenir une fenêtre glissante
     */
    private boolean checkSlidingWindow(String redisKey, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();
        
        // Nettoyer les anciens timestamps
        rateLimitRedisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
        
        // Compter les requêtes dans la fenêtre
            Long currentCount = rateLimitRedisTemplate.opsForZSet().count(redisKey, windowStart, now);
            long count = currentCount != null ? currentCount : 0L;
            
            if (count < maxRequests) {
                // Ajouter la requête actuelle
                rateLimitRedisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
                
                // Définir l'expiration de la clé
                rateLimitRedisTemplate.expire(redisKey, window);
                
                logger.debug("Rate limit OK - Clé: {}, Requêtes: {}/{}", redisKey, count + 1, maxRequests);
                return true;
            } else {
                logger.warn("Rate limit dépassé - Clé: {}, Requêtes: {}/{}", redisKey, count, maxRequests);
                return false;
            }
    }

    /**
     * Obtient le nombre de requêtes restantes pour une clé donnée
     */
    public int getRemainingRequests(RateLimit rateLimit, String requestKey) {
        try {
            String redisKey = buildRedisKey(rateLimit, requestKey);
            Duration window = parseWindow(rateLimit.window());
            long now = System.currentTimeMillis();
            long windowStart = now - window.toMillis();
            
            Long currentCount = rateLimitRedisTemplate.opsForZSet().count(redisKey, windowStart, now);
            long count = currentCount != null ? currentCount : 0L;
            int remaining = rateLimit.requests() - (int) count;
            
            return Math.max(0, remaining);
        } catch (Exception e) {
            logger.error("Erreur lors du calcul des requêtes restantes", e);
            return 0;
        }
    }

    /**
     * Obtient le temps de réinitialisation de la fenêtre
     */
    public long getResetTime(RateLimit rateLimit, String requestKey) {
        try {
            String redisKey = buildRedisKey(rateLimit, requestKey);
            Duration window = parseWindow(rateLimit.window());
            
            // Obtenir le plus ancien timestamp
            var oldestRequest = rateLimitRedisTemplate.opsForZSet().rangeWithScores(redisKey, 0, 0);
            
            if (oldestRequest != null && !oldestRequest.isEmpty()) {
                Double oldestTimestamp = oldestRequest.iterator().next().getScore();
                return oldestTimestamp != null ? (long) (oldestTimestamp + window.toMillis()) : System.currentTimeMillis() + window.toMillis();
            }
            
            return System.currentTimeMillis() + window.toMillis();
        } catch (Exception e) {
            logger.error("Erreur lors du calcul du temps de réinitialisation", e);
            return System.currentTimeMillis() + parseWindow(rateLimit.window()).toMillis();
        }
    }

    /**
     * Génère une clé de requête basée sur le type spécifié
     */
    public String generateRequestKey(String keyType, jakarta.servlet.http.HttpServletRequest request) {
        switch (keyType.toLowerCase()) {
            case "ip":
                return getClientIpAddress(request);
            case "user":
                try {
                    User user = authHelper.getAuthenticatedUserWithFallback(request);
                    return user != null ? user.getId().toString() : getClientIpAddress(request);
                } catch (Exception e) {
                    logger.debug("Utilisateur non authentifié, utilisation de l'IP");
                    return getClientIpAddress(request);
                }
            case "global":
                return "global";
            default:
                return getClientIpAddress(request);
        }
    }

    /**
     * Obtient l'adresse IP du client
     */
    private String getClientIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Réinitialise le compteur pour une clé donnée (utile pour les tests)
     */
    public void resetRateLimit(String requestKey) {
        try {
            // Supprimer toutes les clés qui commencent par le préfixe
            var keys = rateLimitRedisTemplate.keys("rate_limit:*:" + requestKey + ":*");
            if (keys != null && !keys.isEmpty()) {
                rateLimitRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la réinitialisation du rate limit", e);
        }
    }
}
