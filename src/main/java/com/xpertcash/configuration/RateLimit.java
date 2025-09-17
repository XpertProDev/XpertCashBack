package com.xpertcash.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour configurer le Rate Limiting sur les endpoints
 * 
 * Cette annotation permet de définir des limites de débit pour protéger
 * les endpoints contre les abus et les attaques.
 * 
 * @author XpertCash Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Nombre maximum de requêtes autorisées
     * @return nombre de requêtes
     */
    int requests() default 100;
    
    /**
     * Fenêtre de temps pour le comptage des requêtes
     * Formats supportés : "1s", "1m", "1h", "1d"
     * @return fenêtre de temps
     */
    String window() default "1h";
    
    /**
     * Type de clé pour identifier l'utilisateur
     * - "ip" : par adresse IP
     * - "user" : par utilisateur connecté
     * - "global" : global pour tous les utilisateurs
     * @return type de clé
     */
    String key() default "ip";
    
    /**
     * Message d'erreur personnalisé quand la limite est dépassée
     * @return message d'erreur
     */
    String message() default "Trop de requêtes. Veuillez réessayer plus tard.";
    
    /**
     * Indique si le rate limiting doit être appliqué
     * Permet de désactiver temporairement le rate limiting
     * @return true si activé, false sinon
     */
    boolean enabled() default true;
    
    /**
     * Préfixe personnalisé pour la clé Redis
     * Permet de grouper les limites par fonctionnalité
     * @return préfixe de la clé
     */
    String prefix() default "rate_limit";
}
