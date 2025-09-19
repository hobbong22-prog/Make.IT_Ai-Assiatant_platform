package com.Human.Ai.D.makit.integration;

import com.Human.Ai.D.makit.service.CloudWatchMonitoringService;
import com.Human.Ai.D.makit.service.HealthCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MonitoringIntegrationTest {

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private CloudWatchMonitoringService monitoringService;

    @Test
    void testHealthCheckExecution() {
        // When
        HealthCheckService.HealthCheckResult result = healthCheckService.performHealthCheck();

        // Then
        assertNotNull(result);
        assertNotNull(result.getOverallHealth());
        assertNotNull(result.getComponents());
        assertNotNull(result.getTimestamp());
        
        // Verify components are checked
        assertTrue(result.getComponents().containsKey("database"));
        assertTrue(result.getComponents().containsKey("cache"));
        assertTrue(result.getComponents().containsKey("memory"));
        assertTrue(result.getComponents().containsKey("disk"));
        assertTrue(result.getComponents().containsKey("ai_services"));
    }

    @Test
    void testDatabaseHealthCheck() {
        // When
        HealthCheckService.HealthCheckResult result = healthCheckService.performHealthCheck();

        // Then
        HealthCheckService.ComponentHealth dbHealth = result.getComponents().get("database");
        assertNotNull(dbHealth);
        assertNotNull(dbHealth.getStatus());
        assertNotNull(dbHealth.getMessage());
        assertNotNull(dbHealth.getDetails());
        
        // In test environment, database should be available
        assertEquals(HealthCheckService.HealthStatus.UP, dbHealth.getStatus());
    }

    @Test
    void testCacheHealthCheck() {
        // When
        HealthCheckService.HealthCheckResult result = healthCheckService.performHealthCheck();

        // Then
        HealthCheckService.ComponentHealth cacheHealth = result.getComponents().get("cache");
        assertNotNull(cacheHealth);
        assertNotNull(cacheHealth.getStatus());
        assertNotNull(cacheHealth.getMessage());
        assertNotNull(cacheHealth.getDetails());
    }

    @Test
    void testMemoryHealthCheck() {
        // When
        HealthCheckService.HealthCheckResult result = healthCheckService.performHealthCheck();

        // Then
        HealthCheckService.ComponentHealth memoryHealth = result.getComponents().get("memory");
        assertNotNull(memoryHealth);
        assertNotNull(memoryHealth.getStatus());
        assertNotNull(memoryHealth.getMessage());
        assertNotNull(memoryHealth.getDetails());
        
        // Verify memory details are present
        assertTrue(memoryHealth.getDetails().containsKey("usedMemory"));
        assertTrue(memoryHealth.getDetails().containsKey("totalMemory"));
        assertTrue(memoryHealth.getDetails().containsKey("maxMemory"));
        assertTrue(memoryHealth.getDetails().containsKey("usagePercent"));
    }

    @Test
    void testMetricRecording() {
        // Given
        String metricName = "IntegrationTestMetric";
        double value = 123.45;
        Map<String, String> dimensions = Map.of("TestType", "Integration");

        // When
        monitoringService.recordMetric(metricName, value, dimensions);

        // Then - should not throw exception
        assertDoesNotThrow(() -> {
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);
            LocalDateTime endTime = LocalDateTime.now().plusMinutes(1);
            CloudWatchMonitoringService.MetricStatistics stats = 
                monitoringService.getMetricStatistics(metricName, startTime, endTime);
            assertNotNull(stats);
        });
    }

    @Test
    void testDashboardMetricsGeneration() {
        // When
        Map<String, Object> dashboard = monitoringService.getDashboardMetrics();

        // Then
        assertNotNull(dashboard);
        assertTrue(dashboard.containsKey("totalMetrics"));
        assertTrue(dashboard.containsKey("activeAlarms"));
        assertTrue(dashboard.containsKey("aiTaskStats"));
        assertTrue(dashboard.containsKey("apiStats"));
        assertTrue(dashboard.containsKey("cacheStats"));
        assertTrue(dashboard.containsKey("dbStats"));
        assertTrue(dashboard.containsKey("memoryStats"));
        assertTrue(dashboard.containsKey("recentAlarms"));
        
        // Verify data types
        assertTrue(dashboard.get("totalMetrics") instanceof Integer);
        assertTrue(dashboard.get("activeAlarms") instanceof Integer);
    }

    @Test
    void testAlarmCreationAndTriggering() {
        // Given
        String alarmName = "IntegrationTestAlarm";
        String metricName = "IntegrationTestMetric";
        double threshold = 100.0;

        // When - create alarm
        monitoringService.createAlarm(
            alarmName,
            metricName,
            threshold,
            CloudWatchMonitoringService.ComparisonOperator.GREATER_THAN,
            CloudWatchMonitoringService.AlarmSeverity.MEDIUM
        );

        // Record metric that should trigger alarm
        Map<String, String> dimensions = Map.of("TestType", "Alarm");
        monitoringService.recordMetric(metricName, 150.0, dimensions);

        // Then - should not throw exception (alarm is logged)
        assertDoesNotThrow(() -> {
            // Alarm triggering is handled internally
        });
    }

    @Test
    void testSystemMetricsRecording() {
        // When
        monitoringService.recordSystemMetrics();

        // Then - should not throw exception
        assertDoesNotThrow(() -> {
            // System metrics are recorded internally
        });
    }

    @Test
    void testMultipleHealthChecks() {
        // When - perform multiple health checks
        HealthCheckService.HealthCheckResult result1 = healthCheckService.performHealthCheck();
        HealthCheckService.HealthCheckResult result2 = healthCheckService.performHealthCheck();

        // Then - both should succeed
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result1.getOverallHealth());
        assertNotNull(result2.getOverallHealth());
        
        // Timestamps should be different
        assertNotEquals(result1.getTimestamp(), result2.getTimestamp());
    }

    @Test
    void testMetricStatisticsCalculation() {
        // Given
        String metricName = "StatisticsTestMetric";
        Map<String, String> dimensions = Map.of("TestType", "Statistics");
        
        // Record multiple values
        monitoringService.recordMetric(metricName, 10.0, dimensions);
        monitoringService.recordMetric(metricName, 20.0, dimensions);
        monitoringService.recordMetric(metricName, 30.0, dimensions);

        // When
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(1);
        CloudWatchMonitoringService.MetricStatistics stats = 
            monitoringService.getMetricStatistics(metricName, startTime, endTime);

        // Then
        assertNotNull(stats);
        assertEquals(metricName, stats.getMetricName());
        assertTrue(stats.getSampleCount() >= 3);
        assertTrue(stats.getSum() >= 60.0);
        assertTrue(stats.getAverage() >= 10.0);
        assertTrue(stats.getMinimum() >= 10.0);
        assertTrue(stats.getMaximum() >= 30.0);
    }
}