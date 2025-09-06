package com.xpertcash.configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        // Configuration de cache en mémoire simple et efficace
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Définir les noms de cache
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "produits-boutique",
            "produits-entreprise", 
            "stock-historique",
            "stock-entreprise",
            "user-info"
        ));
        
        // Activer le stockage des valeurs null
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
}