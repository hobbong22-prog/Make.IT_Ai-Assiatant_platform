package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.service.CloudWatchMonitoringService;
import com.Human.Ai.D.makit.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller for system monitoring and health checks
 */
@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private CloudWatchMonitoringService monitoringService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            HealthCheckService.HealthCheckResult result = healthCheckService.performHealthCheck();
            
            return ResponseEntity.ok(Map.of(
                "status", result.getOverallHealth(),
                "timestamp", result.getTimestamp(),
                "components", result.getComponents()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "UNHEALTHY",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * Simple liveness probe
     */
    @GetMapping("/health/live")
    public ResponseEntity<?> livenessProbe() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Readiness probe
     */
    @GetMapping("/health/ready")
    public ResponseEntity<?> readinessProbe() {
        try {
            HealthCheckService.HealthCheckResult result = healthCheckService.performHealthCheck();
            
            if (result.getOverallHealth() == HealthCheckService.OverallHealth.UNHEALTHY) {
                return ResponseEntity.status(503).body(Map.of(
                    "status", "NOT_READY",
                    "timestamp", LocalDateTime.now()
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "READY",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "NOT_READY",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * Get dashboard metrics
     */
    @GetMapping("/metrics/dashboard")
    public ResponseEntity<?> getDashboardMetrics() {
        try {
            Map<String, Object> metrics = monitoringService.getDashboardMetrics();
            
            return ResponseEntity.ok(Map.of(
                "metrics", metrics,
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get dashboard metrics",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get specific metric statistics
     */
    @GetMapping("/metrics/{metricName}")
    public ResponseEntity<?> getMetricStatistics(
            @PathVariable String metricName,
            @RequestParam(required = false, defaultValue = "1") int hoursBack) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hoursBack);
            
            CloudWatchMonitoringService.MetricStatistics stats = 
                monitoringService.getMetricStatistics(metricName, startTime, endTime);
            
            return ResponseEntity.ok(Map.of(
                "metricName", stats.getMetricName(),
                "statistics", Map.of(
                    "sum", stats.getSum(),
                    "average", stats.getAverage(),
                    "minimum", stats.getMinimum(),
                    "maximum", stats.getMaximum(),
                    "sampleCount", stats.getSampleCount()
                ),
                "timeRange", Map.of(
                    "startTime", startTime,
                    "endTime", endTime
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get metric statistics",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Record custom metric
     */
    @PostMapping("/metrics")
    public ResponseEntity<?> recordMetric(@RequestBody MetricRequest request) {
        try {
            monitoringService.recordMetric(
                request.getMetricName(),
                request.getValue(),
                request.getDimensions()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Metric recorded successfully",
                "metricName", request.getMetricName(),
                "value", request.getValue()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to record metric",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Create alarm
     */
    @PostMapping("/alarms")
    public ResponseEntity<?> createAlarm(@RequestBody AlarmRequest request) {
        try {
            monitoringService.createAlarm(
                request.getAlarmName(),
                request.getMetricName(),
                request.getThreshold(),
                request.getOperator(),
                request.getSeverity()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Alarm created successfully",
                "alarmName", request.getAlarmName()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to create alarm",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get system information
     */
    @GetMapping("/system/info")
    public ResponseEntity<?> getSystemInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            
            Map<String, Object> systemInfo = Map.of(
                "jvm", Map.of(
                    "version", System.getProperty("java.version"),
                    "vendor", System.getProperty("java.vendor"),
                    "runtime", System.getProperty("java.runtime.name")
                ),
                "memory", Map.of(
                    "total", runtime.totalMemory(),
                    "free", runtime.freeMemory(),
                    "max", runtime.maxMemory(),
                    "used", runtime.totalMemory() - runtime.freeMemory()
                ),
                "processors", runtime.availableProcessors(),
                "os", Map.of(
                    "name", System.getProperty("os.name"),
                    "version", System.getProperty("os.version"),
                    "arch", System.getProperty("os.arch")
                ),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(systemInfo);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get system info",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Metric request DTO
     */
    public static class MetricRequest {
        private String metricName;
        private double value;
        private Map<String, String> dimensions;

        // Getters and setters
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
        public Map<String, String> getDimensions() { return dimensions; }
        public void setDimensions(Map<String, String> dimensions) { this.dimensions = dimensions; }
    }

    /**
     * Alarm request DTO
     */
    public static class AlarmRequest {
        private String alarmName;
        private String metricName;
        private double threshold;
        private CloudWatchMonitoringService.ComparisonOperator operator;
        private CloudWatchMonitoringService.AlarmSeverity severity;

        // Getters and setters
        public String getAlarmName() { return alarmName; }
        public void setAlarmName(String alarmName) { this.alarmName = alarmName; }
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }
        public CloudWatchMonitoringService.ComparisonOperator getOperator() { return operator; }
        public void setOperator(CloudWatchMonitoringService.ComparisonOperator operator) { this.operator = operator; }
        public CloudWatchMonitoringService.AlarmSeverity getSeverity() { return severity; }
        public void setSeverity(CloudWatchMonitoringService.AlarmSeverity severity) { this.severity = severity; }
    }
}