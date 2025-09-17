package com.xpertcash.controller;

import com.xpertcash.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur pour les Health Checks de XpertCashBack
 * 
 * Ce contrôleur expose les endpoints de monitoring et de santé
 * de l'application pour les outils DevOps et de monitoring.
 */
@RestController
@RequestMapping("/api/auth")
public class HealthController {

    @Autowired
    private HealthCheckService healthCheckService;

    /**
     * Health Check complet - État de tous les composants
     * 
     * GET /api/health
     * 
     * @return État détaillé de tous les composants de l'application
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = healthCheckService.getOverallHealth();
        
        // Retourner 503 (Service Unavailable) si l'application est DOWN
        String status = (String) health.get("status");
        HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(httpStatus).body(health);
    }

    /**
     * Liveness Probe - Vérification de vivacité
     * 
     * GET /api/health/liveness
     * 
     * Utilisé par Kubernetes et les orchestrateurs pour vérifier
     * si l'application est vivante (pas en crash).
     * 
     * @return État de vivacité de l'application
     */
    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, Object>> getLiveness() {
        Map<String, Object> liveness = healthCheckService.getLiveness();
        return ResponseEntity.ok(liveness);
    }

    /**
     * Readiness Probe - Vérification de disponibilité
     * 
     * GET /api/health/readiness
     * 
     * Utilisé par Kubernetes et les load balancers pour vérifier
     * si l'application est prête à recevoir du trafic.
     * 
     * @return État de disponibilité de l'application
     */
    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, Object>> getReadiness() {
        Map<String, Object> readiness = healthCheckService.getReadiness();
        
        // Retourner 503 si l'application n'est pas prête
        String status = (String) readiness.get("status");
        HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(httpStatus).body(readiness);
    }

    /**
     * Health Check de la base de données
     * 
     * GET /api/health/database
     * 
     * @return État de la connexion à la base de données
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        Map<String, Object> dbHealth = healthCheckService.checkDatabase();
        
        String status = (String) dbHealth.get("status");
        HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(httpStatus).body(dbHealth);
    }

    /**
     * Health Check de Redis
     * 
     * GET /api/health/redis
     * 
     * @return État de la connexion à Redis
     */
    @GetMapping("/health/redis")
    public ResponseEntity<Map<String, Object>> getRedisHealth() {
        Map<String, Object> redisHealth = healthCheckService.checkRedis();
        
        String status = (String) redisHealth.get("status");
        HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(httpStatus).body(redisHealth);
    }

    /**
     * Health Check de l'espace disque
     * 
     * GET /api/health/disk
     * 
     * @return État de l'espace disque disponible
     */
    @GetMapping("/health/disk")
    public ResponseEntity<Map<String, Object>> getDiskHealth() {
        Map<String, Object> diskHealth = healthCheckService.checkDiskSpace();
        
        String status = (String) diskHealth.get("status");
        HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(httpStatus).body(diskHealth);
    }

    /**
     * Health Check de la mémoire
     * 
     * GET /api/health/memory
     * 
     * @return État de l'utilisation de la mémoire
     */
    @GetMapping("/health/memory")
    public ResponseEntity<Map<String, Object>> getMemoryHealth() {
        Map<String, Object> memoryHealth = healthCheckService.checkMemory();
        
        String status = (String) memoryHealth.get("status");
        HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(httpStatus).body(memoryHealth);
    }

    /**
     * Health Check du système de fichiers
     * 
     * GET /api/health/filesystem
     * 
     * @return État du système de fichiers et des dossiers critiques
     */
    @GetMapping("/health/filesystem")
    public ResponseEntity<Map<String, Object>> getFileSystemHealth() {
        Map<String, Object> fileSystemHealth = healthCheckService.checkFileSystem();
        
        String status = (String) fileSystemHealth.get("status");
        HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(httpStatus).body(fileSystemHealth);
    }

    /**
     * Informations sur l'application
     * 
     * GET /api/health/info
     * 
     * @return Informations générales sur l'application
     */
    @GetMapping("/health/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = Map.of(
            "application", "XpertCashBack",
            "version", "1.0.0",
            "description", "SaaS ERP avec Rate Limiting et Health Checks",
            "features", new String[]{
                "Gestion des ventes",
                "Gestion des stocks", 
                "Gestion des clients",
                "Facturation",
                "Rate Limiting",
                "Health Monitoring"
            },
            "endpoints", Map.of(
                "health", "/api/health",
                "liveness", "/api/health/liveness", 
                "readiness", "/api/health/readiness",
                "database", "/api/health/database",
                "redis", "/api/health/redis",
                "disk", "/api/health/disk",
                "memory", "/api/health/memory",
                "filesystem", "/api/health/filesystem"
            )
        );
        
        return ResponseEntity.ok(info);
    }
}
