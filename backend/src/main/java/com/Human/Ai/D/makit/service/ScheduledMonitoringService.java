package com.Human.Ai.D.makit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for scheduled monitoring tasks
 */
@Service
public class ScheduledMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledMonitoringService.class);

    @Autowired
    private CloudWatchMonitoringService monitoringService;

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    @Autowired
    private AsyncTaskManager asyncTaskManager;

    /**
     * Record system metrics every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void recordSystemMetrics() {
        try {
            monitoringService.recordSystemMetrics();
            logger.debug("Recorded system metrics");
        } catch (Exception e) {
            logger.error("Failed to record system metrics", e);
        }
    }

    /**
     * Perform health check every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void performScheduledHealthCheck() {
        try {
            HealthCheckService.HealthCheckResult result = healthCheckService.performHealthCheck();
            
            // Record health status as metric
            double healthScore = switch (result.getOverallHealth()) {
                case HEALTHY -> 1.0;
                case DEGRADED -> 0.5;
                case UNHEALTHY -> 0.0;
            };
            
            monitoringService.recordMetric("SystemHealthScore", healthScore, 
                Map.of("Application", "makit-platform"));
            
            // Log health status
            if (result.getOverallHealth() != HealthCheckService.OverallHealth.HEALTHY) {
                logger.warn("System health check: {} at {}", result.getOverallHealth(), result.getTimestamp());
                
                // Log component issues
                result.getComponents().forEach((component, health) -> {
                    if (health.getStatus() != HealthCheckService.HealthStatus.UP) {
                        logger.warn("Component {} is {}: {}", component, health.getStatus(), health.getMessage());
                    }
                });
            }
            
        } catch (Exception e) {
            logger.error("Scheduled health check failed", e);
            monitoringService.recordMetric("SystemHealthScore", 0.0, 
                Map.of("Application", "makit-platform", "Error", "HealthCheckFailed"));
        }
    }

    /**
     * Clean up old cache entries every hour
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void performCacheCleanup() {
        try {
            cacheInvalidationService.performScheduledCacheCleanup();
            logger.info("Performed scheduled cache cleanup");
        } catch (Exception e) {
            logger.error("Scheduled cache cleanup failed", e);
        }
    }

    /**
     * Clean up old async tasks every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // Every 6 hours
    public void cleanupOldTasks() {
        try {
            asyncTaskManager.cleanupOldTasks(24); // Clean tasks older than 24 hours
            logger.info("Cleaned up old async tasks");
        } catch (Exception e) {
            logger.error("Failed to cleanup old tasks", e);
        }
    }

    /**
     * Record application metrics every 30 seconds
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void recordApplicationMetrics() {
        try {
            // Record JVM metrics
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            Map<String, String> dimensions = Map.of("Application", "makit-platform");
            
            monitoringService.recordMetric("JVMMemoryUsed", usedMemory, dimensions);
            monitoringService.recordMetric("JVMMemoryFree", freeMemory, dimensions);
            monitoringService.recordMetric("JVMMemoryTotal", totalMemory, dimensions);
            
            // Record thread metrics
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            ThreadGroup parentGroup;
            while ((parentGroup = rootGroup.getParent()) != null) {
                rootGroup = parentGroup;
            }
            
            int activeThreads = rootGroup.activeCount();
            monitoringService.recordMetric("ActiveThreads", activeThreads, dimensions);
            
            logger.debug("Recorded application metrics - Memory: {}MB, Threads: {}", 
                        usedMemory / 1024 / 1024, activeThreads);
            
        } catch (Exception e) {
            logger.error("Failed to record application metrics", e);
        }
    }

    /**
     * Check for system alerts every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    public void checkSystemAlerts() {
        try {
            // Check memory usage
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            if (memoryUsagePercent > 85) {
                logger.warn("High memory usage detected: {:.2f}%", memoryUsagePercent);
                monitoringService.recordMetric("HighMemoryUsageAlert", 1, 
                    Map.of("Application", "makit-platform", "Severity", "WARNING"));
            }
            
            // Check thread count
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            ThreadGroup parentGroup;
            while ((parentGroup = rootGroup.getParent()) != null) {
                rootGroup = parentGroup;
            }
            
            int activeThreads = rootGroup.activeCount();
            if (activeThreads > 100) {
                logger.warn("High thread count detected: {}", activeThreads);
                monitoringService.recordMetric("HighThreadCountAlert", 1,
                    Map.of("Application", "makit-platform", "Severity", "WARNING"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to check system alerts", e);
        }
    }

    /**
     * Generate daily summary report
     */
    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    public void generateDailySummary() {
        try {
            logger.info("Generating daily monitoring summary");
            
            // Get dashboard metrics for summary
            Map<String, Object> metrics = monitoringService.getDashboardMetrics();
            
            logger.info("Daily Summary - Total Metrics: {}, Active Alarms: {}", 
                       metrics.get("totalMetrics"), metrics.get("activeAlarms"));
            
            // Record daily summary metric
            monitoringService.recordMetric("DailySummaryGenerated", 1,
                Map.of("Application", "makit-platform", "Type", "Summary"));
            
        } catch (Exception e) {
            logger.error("Failed to generate daily summary", e);
        }
    }

    /**
     * Initialize default alarms on startup
     */
    @Scheduled(initialDelay = 30000, fixedRate = Long.MAX_VALUE) // Run once after 30 seconds
    public void initializeDefaultAlarms() {
        try {
            logger.info("Initializing default monitoring alarms");
            
            // High memory usage alarm
            monitoringService.createAlarm(
                "HighMemoryUsage",
                "MemoryUtilization",
                85.0,
                CloudWatchMonitoringService.ComparisonOperator.GREATER_THAN,
                CloudWatchMonitoringService.AlarmSeverity.HIGH
            );
            
            // High response time alarm
            monitoringService.createAlarm(
                "HighAPIResponseTime",
                "APIResponseTime",
                5000.0,
                CloudWatchMonitoringService.ComparisonOperator.GREATER_THAN,
                CloudWatchMonitoringService.AlarmSeverity.MEDIUM
            );
            
            // Low cache hit rate alarm
            monitoringService.createAlarm(
                "LowCacheHitRate",
                "CacheHitRate",
                0.7,
                CloudWatchMonitoringService.ComparisonOperator.LESS_THAN,
                CloudWatchMonitoringService.AlarmSeverity.MEDIUM
            );
            
            // System health alarm
            monitoringService.createAlarm(
                "SystemUnhealthy",
                "SystemHealthScore",
                0.5,
                CloudWatchMonitoringService.ComparisonOperator.LESS_THAN,
                CloudWatchMonitoringService.AlarmSeverity.CRITICAL
            );
            
            logger.info("Default monitoring alarms initialized");
            
        } catch (Exception e) {
            logger.error("Failed to initialize default alarms", e);
        }
    }
}