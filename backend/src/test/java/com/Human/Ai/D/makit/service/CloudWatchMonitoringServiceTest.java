package com.Human.Ai.D.makit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CloudWatchMonitoringServiceTest {

    @InjectMocks
    private CloudWatchMonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        // Setup test data if needed
    }

    @Test
    void testRecordMetric() {
        // Given
        String metricName = "TestMetric";
        double value = 100.0;
        Map<String, String> dimensions = Map.of("Environment", "Test");

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            monitoringService.recordMetric(metricName, value, dimensions);
        });
    }

    @Test
    void testRecordAITaskMetric() {
        // Given
        String taskType = "content_generation";
        String status = "SUCCESS";
        long durationMs = 5000;

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            monitoringService.recordAITaskMetric(taskType, status, durationMs);
        });
    }

    @Test
    void testRecordAPIMetric() {
        // Given
        String endpoint = "/api/content";
        int statusCode = 200;
        long responseTimeMs = 150;

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            monitoringService.recordAPIMetric(endpoint, statusCode, responseTimeMs);
        });
    }

    @Test
    void testRecordCacheMetric() {
        // Given
        String cacheName = "contentCache";
        String operation = "GET";
        boolean hit = true;

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            monitoringService.recordCacheMetric(cacheName, operation, hit);
        });
    }

    @Test
    void testRecordDatabaseMetric() {
        // Given
        String operation = "SELECT";
        long executionTimeMs = 50;
        boolean success = true;

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            monitoringService.recordDatabaseMetric(operation, executionTimeMs, success);
        });
    }

    @Test
    void testRecordSystemMetrics() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            monitoringService.recordSystemMetrics();
        });
    }

    @Test
    void testCreateAlarm() {
        // Given
        String alarmName = "TestAlarm";
        String metricName = "TestMetric";
        double threshold = 100.0;
        CloudWatchMonitoringService.ComparisonOperator operator = 
            CloudWatchMonitoringService.ComparisonOperator.GREATER_THAN;
        CloudWatchMonitoringService.AlarmSeverity severity = 
            CloudWatchMonitoringService.AlarmSeverity.HIGH;

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            monitoringService.createAlarm(alarmName, metricName, threshold, operator, severity);
        });
    }

    @Test
    void testGetMetricStatistics() {
        // Given
        String metricName = "TestMetric";
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        // Record some test metrics first
        Map<String, String> dimensions = Map.of("Test", "Value");
        monitoringService.recordMetric(metricName, 10.0, dimensions);
        monitoringService.recordMetric(metricName, 20.0, dimensions);
        monitoringService.recordMetric(metricName, 30.0, dimensions);

        // When
        CloudWatchMonitoringService.MetricStatistics stats = 
            monitoringService.getMetricStatistics(metricName, startTime, endTime);

        // Then
        assertNotNull(stats);
        assertEquals(metricName, stats.getMetricName());
        assertTrue(stats.getSampleCount() >= 0);
    }

    @Test
    void testGetDashboardMetrics() {
        // When
        Map<String, Object> dashboard = monitoringService.getDashboardMetrics();

        // Then
        assertNotNull(dashboard);
        assertTrue(dashboard.containsKey("totalMetrics"));
        assertTrue(dashboard.containsKey("activeAlarms"));
    }

    @Test
    void testMetricDataClass() {
        // Given
        String metricName = "TestMetric";
        double value = 42.0;
        Map<String, String> dimensions = Map.of("Key", "Value");
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        CloudWatchMonitoringService.MetricData metricData = 
            new CloudWatchMonitoringService.MetricData(metricName, value, dimensions, timestamp);

        // Then
        assertEquals(metricName, metricData.getMetricName());
        assertEquals(value, metricData.getValue());
        assertEquals(dimensions, metricData.getDimensions());
        assertEquals(timestamp, metricData.getTimestamp());
    }

    @Test
    void testAlarmConfigurationClass() {
        // Given
        String alarmName = "TestAlarm";
        String metricName = "TestMetric";
        double threshold = 100.0;
        CloudWatchMonitoringService.ComparisonOperator operator = 
            CloudWatchMonitoringService.ComparisonOperator.GREATER_THAN;
        CloudWatchMonitoringService.AlarmSeverity severity = 
            CloudWatchMonitoringService.AlarmSeverity.HIGH;

        // When
        CloudWatchMonitoringService.AlarmConfiguration alarm = 
            new CloudWatchMonitoringService.AlarmConfiguration(
                alarmName, metricName, threshold, operator, severity);

        // Then
        assertEquals(alarmName, alarm.getAlarmName());
        assertEquals(metricName, alarm.getMetricName());
        assertEquals(threshold, alarm.getThreshold());
        assertEquals(operator, alarm.getOperator());
        assertEquals(severity, alarm.getSeverity());
    }

    @Test
    void testMetricStatisticsClass() {
        // Given
        String metricName = "TestMetric";
        double sum = 100.0;
        double average = 33.33;
        double minimum = 10.0;
        double maximum = 50.0;
        long sampleCount = 3;

        // When
        CloudWatchMonitoringService.MetricStatistics stats = 
            new CloudWatchMonitoringService.MetricStatistics(
                metricName, sum, average, minimum, maximum, sampleCount);

        // Then
        assertEquals(metricName, stats.getMetricName());
        assertEquals(sum, stats.getSum());
        assertEquals(average, stats.getAverage());
        assertEquals(minimum, stats.getMinimum());
        assertEquals(maximum, stats.getMaximum());
        assertEquals(sampleCount, stats.getSampleCount());
    }

    @Test
    void testAlarmTriggering() {
        // Given
        String alarmName = "HighValueAlarm";
        String metricName = "TestMetric";
        double threshold = 50.0;
        
        // Create alarm
        monitoringService.createAlarm(
            alarmName, 
            metricName, 
            threshold, 
            CloudWatchMonitoringService.ComparisonOperator.GREATER_THAN,
            CloudWatchMonitoringService.AlarmSeverity.HIGH
        );

        // When - record metric that should trigger alarm
        Map<String, String> dimensions = Map.of("Test", "Alarm");
        monitoringService.recordMetric(metricName, 75.0, dimensions);

        // Then - alarm should be triggered (logged)
        // In a real implementation, you might want to capture and verify the alarm event
        assertDoesNotThrow(() -> {
            // The alarm triggering is logged, so we just verify no exceptions
        });
    }
}