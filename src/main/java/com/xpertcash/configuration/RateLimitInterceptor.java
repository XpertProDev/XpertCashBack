package com.xpertcash.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpertcash.service.RateLimiterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercepteur pour appliquer le Rate Limiting sur les endpoints
 * 
 * Cet intercepteur vérifie les annotations @RateLimit sur les méthodes
 * et applique les restrictions de débit avant l'exécution des contrôleurs.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Vérifier si c'est une méthode de contrôleur
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // Récupérer l'annotation @RateLimit sur la méthode
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        
        // Si pas d'annotation sur la méthode, vérifier sur la classe
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }
        
        // Si aucune annotation, pas de rate limiting
        if (rateLimit == null) {
            return true;
        }

        // Générer la clé de requête
        String requestKey = rateLimiterService.generateRequestKey(rateLimit.key(), request);
        
        // Vérifier si la requête est autorisée
        boolean isAllowed = rateLimiterService.isRequestAllowed(rateLimit, requestKey);
        
        if (!isAllowed) {
            // Rate limit dépassé, retourner une erreur
            handleRateLimitExceeded(response, rateLimit, requestKey);
            return false;
        }
        
        // Ajouter les headers d'information sur le rate limit
        addRateLimitHeaders(response, rateLimit, requestKey);
        
        return true;
    }

    /**
     * Gère le cas où le rate limit est dépassé
     */
    private void handleRateLimitExceeded(HttpServletResponse response, RateLimit rateLimit, String requestKey) throws IOException {
        
        logger.warn("Rate limit dépassé - Clé: {}, Limite: {}/{}", 
                   requestKey, rateLimit.requests(), rateLimit.window());
        
        // Calculer le temps de réinitialisation
        long resetTime = rateLimiterService.getResetTime(rateLimit, requestKey);
        long retryAfter = (resetTime - System.currentTimeMillis()) / 1000;
        
        // Préparer la réponse d'erreur
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Rate limit exceeded");
        errorResponse.put("message", rateLimit.message());
        errorResponse.put("retryAfter", Math.max(1, retryAfter));
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorResponse.put("limit", rateLimit.requests());
        errorResponse.put("window", rateLimit.window());
        
        // Configurer la réponse HTTP
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.requests()));
        response.setHeader("X-RateLimit-Window", rateLimit.window());
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
        
        // Écrire la réponse JSON
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Ajoute les headers d'information sur le rate limit
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimit rateLimit, String requestKey) {
        try {
            int remaining = rateLimiterService.getRemainingRequests(rateLimit, requestKey);
            long resetTime = rateLimiterService.getResetTime(rateLimit, requestKey);
            
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.requests()));
            response.setHeader("X-RateLimit-Window", rateLimit.window());
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout des headers de rate limit", e);
        }
    }
}
