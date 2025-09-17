package com.xpertcash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de Health Check pour XpertCashBack
 * 
 * Ce service vérifie l'état de santé de tous les composants critiques
 * de l'application : base de données, Redis, système de fichiers, etc.
 */
@Service
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private RedisTemplate<String, String> rateLimitRedisTemplate;

    /**
     * Vérifie l'état de santé complet de l'application
     */
    public Map<String, Object> getOverallHealth() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> components = new HashMap<>();
        
        boolean allHealthy = true;
        
        // Vérifier la base de données
        Map<String, Object> dbHealth = checkDatabase();
        components.put("database", dbHealth);
        if (!"UP".equals(dbHealth.get("status"))) {
            allHealthy = false;
        }
        
        // Vérifier Redis
        Map<String, Object> redisHealth = checkRedis();
        components.put("redis", redisHealth);
        if (!"UP".equals(redisHealth.get("status"))) {
            allHealthy = false;
        }
        
        // Vérifier l'espace disque
        Map<String, Object> diskHealth = checkDiskSpace();
        components.put("diskSpace", diskHealth);
        if (!"UP".equals(diskHealth.get("status"))) {
            allHealthy = false;
        }
        
        // Vérifier la mémoire
        Map<String, Object> memoryHealth = checkMemory();
        components.put("memory", memoryHealth);
        if (!"UP".equals(memoryHealth.get("status"))) {
            allHealthy = false;
        }
        
        // Vérifier le système de fichiers
        Map<String, Object> fileSystemHealth = checkFileSystem();
        components.put("fileSystem", fileSystemHealth);
        if (!"UP".equals(fileSystemHealth.get("status"))) {
            allHealthy = false;
        }
        
        // Informations générales
        health.put("status", allHealthy ? "UP" : "DOWN");
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        health.put("application", "XpertCashBack");
        health.put("version", "1.0.0");
        health.put("components", components);
        
        return health;
    }

    /**
     * Vérifie la connectivité à la base de données
     */
    public Map<String, Object> checkDatabase() {
        Map<String, Object> dbHealth = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection()) {
                // Test de connexion simple
                try (PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
                    stmt.executeQuery();
                }
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                dbHealth.put("status", "UP");
                dbHealth.put("details", Map.of(
                    "database", "MySQL",
                    "responseTime", responseTime + "ms",
                    "validationQuery", "SELECT 1"
                ));
                
                logger.debug("Database health check: UP ({}ms)", responseTime);
                
            }
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("details", Map.of(
                "error", e.getMessage(),
                "type", e.getClass().getSimpleName()
            ));
            
            logger.error("Database health check failed", e);
        }
        
        return dbHealth;
    }

    /**
     * Vérifie la connectivité à Redis
     */
    public Map<String, Object> checkRedis() {
        Map<String, Object> redisHealth = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Test de ping Redis
            String pong = rateLimitRedisTemplate.getConnectionFactory()
                .getConnection().ping();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if ("PONG".equals(pong)) {
                redisHealth.put("status", "UP");
                redisHealth.put("details", Map.of(
                    "response", pong,
                    "responseTime", responseTime + "ms",
                    "purpose", "Rate Limiting"
                ));
                
                logger.debug("Redis health check: UP ({}ms)", responseTime);
            } else {
                redisHealth.put("status", "DOWN");
                redisHealth.put("details", Map.of(
                    "error", "Unexpected response: " + pong
                ));
            }
            
        } catch (Exception e) {
            redisHealth.put("status", "DOWN");
            redisHealth.put("details", Map.of(
                "error", e.getMessage(),
                "type", e.getClass().getSimpleName(),
                "note", "Rate limiting will use in-memory cache"
            ));
            
            logger.warn("Redis health check failed - using fallback", e);
        }
        
        return redisHealth;
    }

    /**
     * Vérifie l'espace disque disponible
     */
    public Map<String, Object> checkDiskSpace() {
        Map<String, Object> diskHealth = new HashMap<>();
        
        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usableSpace = root.getUsableSpace();
            
            // Seuil critique : moins de 1GB libre
            long criticalThreshold = 1024 * 1024 * 1024; // 1GB
            // Seuil d'avertissement : moins de 5GB libre
            long warningThreshold = 5 * 1024 * 1024 * 1024; // 5GB
            
            String status;
            if (freeSpace < criticalThreshold) {
                status = "DOWN";
            } else if (freeSpace < warningThreshold) {
                status = "UP";
            } else {
                status = "UP";
            }
            
            diskHealth.put("status", status);
            diskHealth.put("details", Map.of(
                "total", formatBytes(totalSpace),
                "free", formatBytes(freeSpace),
                "usable", formatBytes(usableSpace),
                "freePercentage", String.format("%.1f%%", (double) freeSpace / totalSpace * 100),
                "threshold", formatBytes(criticalThreshold)
            ));
            
            logger.debug("Disk space health check: {} ({} free)", status, formatBytes(freeSpace));
            
        } catch (Exception e) {
            diskHealth.put("status", "DOWN");
            diskHealth.put("details", Map.of(
                "error", e.getMessage()
            ));
            
            logger.error("Disk space health check failed", e);
        }
        
        return diskHealth;
    }

    /**
     * Vérifie l'utilisation de la mémoire
     */
    public Map<String, Object> checkMemory() {
        Map<String, Object> memoryHealth = new HashMap<>();
        
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            
            // Seuil critique : plus de 90% de la heap utilisée
            double heapUsagePercentage = (double) heapUsed / heapMax * 100;
            
            String status = heapUsagePercentage > 90 ? "DOWN" : 
                           heapUsagePercentage > 80 ? "UP" : "UP";
            
            memoryHealth.put("status", status);
            memoryHealth.put("details", Map.of(
                "heapUsed", formatBytes(heapUsed),
                "heapMax", formatBytes(heapMax),
                "heapUsagePercentage", String.format("%.1f%%", heapUsagePercentage),
                "nonHeapUsed", formatBytes(nonHeapUsed),
                "nonHeapMax", formatBytes(nonHeapMax),
                "uptime", formatUptime(runtimeBean.getUptime())
            ));
            
            logger.debug("Memory health check: {} ({} heap usage)", status, String.format("%.1f%%", heapUsagePercentage));
            
        } catch (Exception e) {
            memoryHealth.put("status", "DOWN");
            memoryHealth.put("details", Map.of(
                "error", e.getMessage()
            ));
            
            logger.error("Memory health check failed", e);
        }
        
        return memoryHealth;
    }

    /**
     * Vérifie le système de fichiers (dossiers critiques)
     */
    public Map<String, Object> checkFileSystem() {
        Map<String, Object> fileSystemHealth = new HashMap<>();
        
        try {
            Map<String, Object> directories = new HashMap<>();
            boolean allDirectoriesOk = true;
            
            // Vérifier les dossiers critiques
            String[] criticalDirs = {
                "src/main/resources/static/uploads",
                "src/main/resources/static/userUpload",
                "src/main/resources/static/defaultProfile",
                "src/main/resources/static/defaultLogo"
            };
            
            for (String dirPath : criticalDirs) {
                File dir = new File(dirPath);
                boolean exists = dir.exists();
                boolean writable = dir.canWrite();
                boolean readable = dir.canRead();
                
                directories.put(dirPath, Map.of(
                    "exists", exists,
                    "readable", readable,
                    "writable", writable
                ));
                
                if (!exists || !writable || !readable) {
                    allDirectoriesOk = false;
                }
            }
            
            String status = allDirectoriesOk ? "UP" : "DOWN";
            
            fileSystemHealth.put("status", status);
            fileSystemHealth.put("details", Map.of(
                "directories", directories,
                "allDirectoriesOk", allDirectoriesOk
            ));
            
            logger.debug("File system health check: {}", status);
            
        } catch (Exception e) {
            fileSystemHealth.put("status", "DOWN");
            fileSystemHealth.put("details", Map.of(
                "error", e.getMessage()
            ));
            
            logger.error("File system health check failed", e);
        }
        
        return fileSystemHealth;
    }

    /**
     * Vérification simple pour les probes de vivacité
     */
    public Map<String, Object> getLiveness() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "UP");
        liveness.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return liveness;
    }

    /**
     * Vérification de disponibilité pour les probes de readiness
     */
    public Map<String, Object> getReadiness() {
        Map<String, Object> readiness = new HashMap<>();
        
        // Vérifier seulement les composants critiques pour la disponibilité
        Map<String, Object> dbHealth = checkDatabase();
        Map<String, Object> redisHealth = checkRedis();
        
        boolean isReady = "UP".equals(dbHealth.get("status"));
        
        readiness.put("status", isReady ? "UP" : "DOWN");
        readiness.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        readiness.put("components", Map.of(
            "database", dbHealth,
            "redis", redisHealth
        ));
        
        return readiness;
    }

    /**
     * Formate les bytes en format lisible
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Formate le temps de fonctionnement
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        if (hours > 0) return String.format("%dh %dm", hours, minutes % 60);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds % 60);
        return String.format("%ds", seconds);
    }
}
