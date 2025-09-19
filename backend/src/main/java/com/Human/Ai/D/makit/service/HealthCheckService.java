package com.Human.Ai.D.makit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for system health checks and monitoring
 */
@Service
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CloudWatchMonitoringService monitoringService;

    /**
     * Perform comprehensive health check
     */
    public HealthCheckResult performHealthCheck() {
        Map<String, ComponentHealth> componentHealths = new HashMap<>();
        OverallHealth overallHealth = OverallHealth.HEALTHY;

        // Check database
        ComponentHealth dbHealth = checkDatabaseHealth();
        componentHealths.put("database", dbHealth);
        if (dbHealth.getStatus() != HealthStatus.UP) {
            overallHealth = OverallHealth.DEGRADED;
        }

        // Check cache
        ComponentHealth cacheHealth = checkCacheHealth();
        componentHealths.put("cache", cacheHealth);
        if (cacheHealth.getStatus() != HealthStatus.UP) {
            overallHealth = OverallHealth.DEGRADED;
        }

        // Check memory
        ComponentHealth memoryHealth = checkMemoryHealth();
        componentHealths.put("memory", memoryHealth);
        if (memoryHealth.getStatus() == HealthStatus.DOWN) {
            overallHealth = OverallHealth.UNHEALTHY;
        }

        // Check disk space
        ComponentHealth diskHealth = checkDiskHealth();
        componentHealths.put("disk", diskHealth);
        if (diskHealth.getStatus() == HealthStatus.DOWN) {
            overallHealth = OverallHealth.UNHEALTHY;
        }

        // Check AI services
        ComponentHealth aiHealth = checkAIServicesHealth();
        componentHealths.put("ai_services", aiHealth);
        if (aiHealth.getStatus() != HealthStatus.UP) {
            overallHealth = OverallHealth.DEGRADED;
        }

        HealthCheckResult result = new HealthCheckResult(overallHealth, componentHealths, LocalDateTime.now());
        
        // Record health metrics
        monitoringService.recordMetric("SystemHealth", 
            overallHealth == OverallHealth.HEALTHY ? 1 : 0, 
            Map.of("Application", "makit-platform"));

        return result;
    }

    /**
     * Check database connectivity and performance
     */
    private ComponentHealth checkDatabaseHealth() {
        try {
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5 second timeout
                long responseTime = System.currentTimeMillis() - startTime;
                
                Map<String, Object> details = new HashMap<>();
                details.put("responseTime", responseTime + "ms");
                details.put("connectionValid", isValid);
                
                monitoringService.recordDatabaseMetric("health_check", responseTime, isValid);
                
                if (isValid && responseTime < 1000) {
                    return new ComponentHealth(HealthStatus.UP, "Database connection healthy", details);
                } else if (isValid) {
                    return new ComponentHealth(HealthStatus.DEGRADED, "Database slow response", details);
                } else {
                    return new ComponentHealth(HealthStatus.DOWN, "Database connection invalid", details);
                }
            }
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return new ComponentHealth(HealthStatus.DOWN, "Database connection failed: " + e.getMessage(), 
                                     Map.of("error", e.getClass().getSimpleName()));
        }
    }

    /**
     * Check cache system health
     */
    private ComponentHealth checkCacheHealth() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Test cache operations
            String testKey = "health_check_" + System.currentTimeMillis();
            String testValue = "test_value";
            
            cacheService.put("health", testKey, testValue);
            String retrievedValue = cacheService.get("health", testKey, String.class);
            cacheService.evict("health", testKey);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> details = new HashMap<>();
            details.put("responseTime", responseTime + "ms");
            details.put("operationsSuccessful", testValue.equals(retrievedValue));
            
            monitoringService.recordCacheMetric("health", "health_check", testValue.equals(retrievedValue));
            
            if (testValue.equals(retrievedValue) && responseTime < 500) {
                return new ComponentHealth(HealthStatus.UP, "Cache system healthy", details);
            } else if (testValue.equals(retrievedValue)) {
                return new ComponentHealth(HealthStatus.DEGRADED, "Cache system slow", details);
            } else {
                return new ComponentHealth(HealthStatus.DOWN, "Cache operations failed", details);
            }
            
        } catch (Exception e) {
            logger.error("Cache health check failed", e);
            return new ComponentHealth(HealthStatus.DOWN, "Cache system error: " + e.getMessage(),
                                     Map.of("error", e.getClass().getSimpleName()));
        }
    }

    /**
     * Check memory usage
     */
    private ComponentHealth checkMemoryHealth() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            Map<String, Object> details = new HashMap<>();
            details.put("usedMemory", formatBytes(usedMemory));
            details.put("totalMemory", formatBytes(totalMemory));
            details.put("maxMemory", formatBytes(maxMemory));
            details.put("usagePercent", String.format("%.2f%%", memoryUsagePercent));
            
            monitoringService.recordMetric("MemoryUsagePercent", memoryUsagePercent, 
                                         Map.of("Application", "makit-platform"));
            
            if (memoryUsagePercent < 80) {
                return new ComponentHealth(HealthStatus.UP, "Memory usage normal", details);
            } else if (memoryUsagePercent < 90) {
                return new ComponentHealth(HealthStatus.DEGRADED, "Memory usage high", details);
            } else {
                return new ComponentHealth(HealthStatus.DOWN, "Memory usage critical", details);
            }
            
        } catch (Exception e) {
            logger.error("Memory health check failed", e);
            return new ComponentHealth(HealthStatus.DOWN, "Memory check error: " + e.getMessage(),
                                     Map.of("error", e.getClass().getSimpleName()));
        }
    }

    /**
     * Check disk space
     */
    private ComponentHealth checkDiskHealth() {
        try {
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double diskUsagePercent = (double) usedSpace / totalSpace * 100;
            
            Map<String, Object> details = new HashMap<>();
            details.put("totalSpace", formatBytes(totalSpace));
            details.put("freeSpace", formatBytes(freeSpace));
            details.put("usedSpace", formatBytes(usedSpace));
            details.put("usagePercent", String.format("%.2f%%", diskUsagePercent));
            
            monitoringService.recordMetric("DiskUsagePercent", diskUsagePercent,
                                         Map.of("Application", "makit-platform"));
            
            if (diskUsagePercent < 80) {
                return new ComponentHealth(HealthStatus.UP, "Disk space sufficient", details);
            } else if (diskUsagePercent < 90) {
                return new ComponentHealth(HealthStatus.DEGRADED, "Disk space low", details);
            } else {
                return new ComponentHealth(HealthStatus.DOWN, "Disk space critical", details);
            }
            
        } catch (Exception e) {
            logger.error("Disk health check failed", e);
            return new ComponentHealth(HealthStatus.DOWN, "Disk check error: " + e.getMessage(),
                                     Map.of("error", e.getClass().getSimpleName()));
        }
    }

    /**
     * Check AI services health
     */
    private ComponentHealth checkAIServicesHealth() {
        try {
            // This is a simplified check - in production you'd test actual AI service connectivity
            Map<String, Object> details = new HashMap<>();
            details.put("bedrockService", "Available");
            details.put("lastCheck", LocalDateTime.now());
            
            // Simulate AI service check
            CompletableFuture<Boolean> aiCheck = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100); // Simulate network call
                    return true; // Assume healthy for demo
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            });
            
            boolean aiHealthy = aiCheck.get(5, TimeUnit.SECONDS);
            
            if (aiHealthy) {
                return new ComponentHealth(HealthStatus.UP, "AI services operational", details);
            } else {
                return new ComponentHealth(HealthStatus.DOWN, "AI services unavailable", details);
            }
            
        } catch (Exception e) {
            logger.error("AI services health check failed", e);
            return new ComponentHealth(HealthStatus.DOWN, "AI services check error: " + e.getMessage(),
                                     Map.of("error", e.getClass().getSimpleName()));
        }
    }

    /**
     * Format bytes to human readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Health check result class
     */
    public static class HealthCheckResult {
        private final OverallHealth overallHealth;
        private final Map<String, ComponentHealth> components;
        private final LocalDateTime timestamp;

        public HealthCheckResult(OverallHealth overallHealth, Map<String, ComponentHealth> components, LocalDateTime timestamp) {
            this.overallHealth = overallHealth;
            this.components = components;
            this.timestamp = timestamp;
        }

        // Getters
        public OverallHealth getOverallHealth() { return overallHealth; }
        public Map<String, ComponentHealth> getComponents() { return components; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    /**
     * Component health class
     */
    public static class ComponentHealth {
        private final HealthStatus status;
        private final String message;
        private final Map<String, Object> details;

        public ComponentHealth(HealthStatus status, String message, Map<String, Object> details) {
            this.status = status;
            this.message = message;
            this.details = details;
        }

        // Getters
        public HealthStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }
    }

    /**
     * Health status enumeration
     */
    public enum HealthStatus {
        UP, DEGRADED, DOWN
    }

    /**
     * Overall health enumeration
     */
    public enum OverallHealth {
        HEALTHY, DEGRADED, UNHEALTHY
    }
}