package com.Human.Ai.D.makit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for CloudWatch monitoring integration
 */
@Service
public class CloudWatchMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(CloudWatchMonitoringService.class);

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${spring.application.name:makit-platform}")
    private String applicationName;

    // In-memory metrics storage (in production, this would send to CloudWatch)
    private final Map<String, MetricData> metrics = new ConcurrentHashMap<>();
    private final Map<String, AlarmConfiguration> alarms = new ConcurrentHashMap<>();

    /**
     * Record a custom metric
     */
    public void recordMetric(String metricName, double value, Map<String, String> dimensions) {
        try {
            MetricData metric = new MetricData(metricName, value, dimensions, LocalDateTime.now());
            metrics.put(metricName + "_" + System.currentTimeMillis(), metric);
            
            logger.debug("Recorded metric: {} = {} with dimensions: {}", metricName, value, dimensions);
            
            // Check alarms
            checkAlarms(metricName, value);
            
        } catch (Exception e) {
            logger.error("Failed to record metric: {}", metricName, e);
        }
    }

    /**
     * Record AI task metrics
     */
    public void recordAITaskMetric(String taskType, String status, long durationMs) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("TaskType", taskType);
        dimensions.put("Status", status);
        dimensions.put("Application", applicationName);
        
        recordMetric("AITaskDuration", durationMs, dimensions);
        recordMetric("AITaskCount", 1, dimensions);
    }

    /**
     * Record API request metrics
     */
    public void recordAPIMetric(String endpoint, int statusCode, long responseTimeMs) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("Endpoint", endpoint);
        dimensions.put("StatusCode", String.valueOf(statusCode));
        dimensions.put("Application", applicationName);
        
        recordMetric("APIResponseTime", responseTimeMs, dimensions);
        recordMetric("APIRequestCount", 1, dimensions);
    }

    /**
     * Record cache metrics
     */
    public void recordCacheMetric(String cacheName, String operation, boolean hit) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("CacheName", cacheName);
        dimensions.put("Operation", operation);
        dimensions.put("Application", applicationName);
        
        recordMetric("CacheHitRate", hit ? 1 : 0, dimensions);
        recordMetric("CacheOperationCount", 1, dimensions);
    }

    /**
     * Record database metrics
     */
    public void recordDatabaseMetric(String operation, long executionTimeMs, boolean success) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("Operation", operation);
        dimensions.put("Status", success ? "Success" : "Error");
        dimensions.put("Application", applicationName);
        
        recordMetric("DatabaseExecutionTime", executionTimeMs, dimensions);
        recordMetric("DatabaseOperationCount", 1, dimensions);
    }

    /**
     * Record system resource metrics
     */
    public void recordSystemMetrics() {
        try {
            Runtime runtime = Runtime.getRuntime();
            
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            Map<String, String> dimensions = Map.of("Application", applicationName);
            
            recordMetric("MemoryUsed", usedMemory, dimensions);
            recordMetric("MemoryFree", freeMemory, dimensions);
            recordMetric("MemoryTotal", totalMemory, dimensions);
            recordMetric("MemoryMax", maxMemory, dimensions);
            recordMetric("MemoryUtilization", (double) usedMemory / maxMemory * 100, dimensions);
            
            // CPU metrics would require additional libraries like OSHI
            
        } catch (Exception e) {
            logger.error("Failed to record system metrics", e);
        }
    }

    /**
     * Create an alarm configuration
     */
    public void createAlarm(String alarmName, String metricName, double threshold, 
                           ComparisonOperator operator, AlarmSeverity severity) {
        AlarmConfiguration alarm = new AlarmConfiguration(
            alarmName, metricName, threshold, operator, severity
        );
        alarms.put(alarmName, alarm);
        
        logger.info("Created alarm: {} for metric: {} with threshold: {}", 
                   alarmName, metricName, threshold);
    }

    /**
     * Check alarms for a metric
     */
    private void checkAlarms(String metricName, double value) {
        alarms.values().stream()
            .filter(alarm -> alarm.getMetricName().equals(metricName))
            .forEach(alarm -> {
                boolean triggered = false;
                
                switch (alarm.getOperator()) {
                    case GREATER_THAN:
                        triggered = value > alarm.getThreshold();
                        break;
                    case LESS_THAN:
                        triggered = value < alarm.getThreshold();
                        break;
                    case GREATER_THAN_OR_EQUAL:
                        triggered = value >= alarm.getThreshold();
                        break;
                    case LESS_THAN_OR_EQUAL:
                        triggered = value <= alarm.getThreshold();
                        break;
                }
                
                if (triggered) {
                    triggerAlarm(alarm, value);
                }
            });
    }

    /**
     * Trigger an alarm
     */
    private void triggerAlarm(AlarmConfiguration alarm, double value) {
        logger.warn("ALARM TRIGGERED: {} - Metric: {} Value: {} Threshold: {}", 
                   alarm.getAlarmName(), alarm.getMetricName(), value, alarm.getThreshold());
        
        // In production, this would send notifications via SNS, email, etc.
        AlarmEvent event = new AlarmEvent(
            alarm.getAlarmName(),
            alarm.getMetricName(),
            value,
            alarm.getThreshold(),
            alarm.getSeverity(),
            LocalDateTime.now()
        );
        
        // Store alarm event for retrieval
        metrics.put("alarm_" + alarm.getAlarmName() + "_" + System.currentTimeMillis(), 
                   new MetricData("AlarmTriggered", 1, 
                   Map.of("AlarmName", alarm.getAlarmName(), "Severity", alarm.getSeverity().name()), 
                   LocalDateTime.now()));
    }

    /**
     * Get metric statistics
     */
    public MetricStatistics getMetricStatistics(String metricName, LocalDateTime startTime, LocalDateTime endTime) {
        var relevantMetrics = metrics.values().stream()
            .filter(m -> m.getMetricName().equals(metricName))
            .filter(m -> m.getTimestamp().isAfter(startTime) && m.getTimestamp().isBefore(endTime))
            .mapToDouble(MetricData::getValue)
            .toArray();
        
        if (relevantMetrics.length == 0) {
            return new MetricStatistics(metricName, 0, 0, 0, 0, 0);
        }
        
        double sum = java.util.Arrays.stream(relevantMetrics).sum();
        double avg = sum / relevantMetrics.length;
        double min = java.util.Arrays.stream(relevantMetrics).min().orElse(0);
        double max = java.util.Arrays.stream(relevantMetrics).max().orElse(0);
        
        return new MetricStatistics(metricName, sum, avg, min, max, relevantMetrics.length);
    }

    /**
     * Get all metrics for dashboard
     */
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> dashboard = new HashMap<>();
        
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime now = LocalDateTime.now();
        
        // AI Task metrics
        dashboard.put("aiTaskStats", getMetricStatistics("AITaskDuration", oneHourAgo, now));
        dashboard.put("apiStats", getMetricStatistics("APIResponseTime", oneHourAgo, now));
        dashboard.put("cacheStats", getMetricStatistics("CacheHitRate", oneHourAgo, now));
        dashboard.put("dbStats", getMetricStatistics("DatabaseExecutionTime", oneHourAgo, now));
        
        // System metrics
        recordSystemMetrics(); // Update current system metrics
        dashboard.put("memoryStats", getMetricStatistics("MemoryUtilization", oneHourAgo, now));
        
        // Recent alarms
        var recentAlarms = metrics.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("alarm_"))
            .filter(entry -> entry.getValue().getTimestamp().isAfter(oneHourAgo))
            .map(Map.Entry::getValue)
            .toList();
        
        dashboard.put("recentAlarms", recentAlarms);
        dashboard.put("totalMetrics", metrics.size());
        dashboard.put("activeAlarms", alarms.size());
        
        return dashboard;
    }

    /**
     * Metric data class
     */
    public static class MetricData {
        private final String metricName;
        private final double value;
        private final Map<String, String> dimensions;
        private final LocalDateTime timestamp;

        public MetricData(String metricName, double value, Map<String, String> dimensions, LocalDateTime timestamp) {
            this.metricName = metricName;
            this.value = value;
            this.dimensions = dimensions;
            this.timestamp = timestamp;
        }

        // Getters
        public String getMetricName() { return metricName; }
        public double getValue() { return value; }
        public Map<String, String> getDimensions() { return dimensions; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    /**
     * Alarm configuration class
     */
    public static class AlarmConfiguration {
        private final String alarmName;
        private final String metricName;
        private final double threshold;
        private final ComparisonOperator operator;
        private final AlarmSeverity severity;

        public AlarmConfiguration(String alarmName, String metricName, double threshold, 
                                ComparisonOperator operator, AlarmSeverity severity) {
            this.alarmName = alarmName;
            this.metricName = metricName;
            this.threshold = threshold;
            this.operator = operator;
            this.severity = severity;
        }

        // Getters
        public String getAlarmName() { return alarmName; }
        public String getMetricName() { return metricName; }
        public double getThreshold() { return threshold; }
        public ComparisonOperator getOperator() { return operator; }
        public AlarmSeverity getSeverity() { return severity; }
    }

    /**
     * Alarm event class
     */
    public static class AlarmEvent {
        private final String alarmName;
        private final String metricName;
        private final double value;
        private final double threshold;
        private final AlarmSeverity severity;
        private final LocalDateTime timestamp;

        public AlarmEvent(String alarmName, String metricName, double value, double threshold, 
                         AlarmSeverity severity, LocalDateTime timestamp) {
            this.alarmName = alarmName;
            this.metricName = metricName;
            this.value = value;
            this.threshold = threshold;
            this.severity = severity;
            this.timestamp = timestamp;
        }

        // Getters
        public String getAlarmName() { return alarmName; }
        public String getMetricName() { return metricName; }
        public double getValue() { return value; }
        public double getThreshold() { return threshold; }
        public AlarmSeverity getSeverity() { return severity; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    /**
     * Metric statistics class
     */
    public static class MetricStatistics {
        private final String metricName;
        private final double sum;
        private final double average;
        private final double minimum;
        private final double maximum;
        private final long sampleCount;

        public MetricStatistics(String metricName, double sum, double average, 
                              double minimum, double maximum, long sampleCount) {
            this.metricName = metricName;
            this.sum = sum;
            this.average = average;
            this.minimum = minimum;
            this.maximum = maximum;
            this.sampleCount = sampleCount;
        }

        // Getters
        public String getMetricName() { return metricName; }
        public double getSum() { return sum; }
        public double getAverage() { return average; }
        public double getMinimum() { return minimum; }
        public double getMaximum() { return maximum; }
        public long getSampleCount() { return sampleCount; }
    }

    /**
     * Comparison operator enumeration
     */
    public enum ComparisonOperator {
        GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL
    }

    /**
     * Alarm severity enumeration
     */
    public enum AlarmSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}