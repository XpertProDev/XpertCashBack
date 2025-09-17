package com.xpertcash.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Web pour enregistrer les intercepteurs
 * 
 * Cette configuration permet d'enregistrer l'intercepteur de rate limiting
 * pour qu'il soit appliqué sur toutes les requêtes HTTP.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Enregistrer l'intercepteur de rate limiting
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**") // Appliquer sur toutes les routes API
                .excludePathPatterns(
                    "/api/auth/register", // Exclure l'inscription (géré séparément)
                    "/api/auth/activate", // Exclure l'activation
                    "/api/auth/forgot-password", // Exclure la récupération de mot de passe
                    "/api/auth/reset-password", // Exclure la réinitialisation
                    "/swagger-ui/**", // Exclure Swagger UI
                    "/api-docs/**", // Exclure la documentation API
                    "/csrf" // Exclure l'endpoint CSRF
                );
    }
}