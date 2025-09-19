package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.dto.*;
import com.Human.Ai.D.makit.service.DashboardService;
import com.Human.Ai.D.makit.service.DashboardService.DateRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST controller for dashboard data endpoints
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    /**
     * Get comprehensive dashboard overview
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DashboardOverview> getDashboardOverview(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Fetching dashboard overview for user: {} from {} to {}", userId, startDate, endDate);
        
        try {
            DateRange dateRange = new DateRange(startDate, endDate);
            DashboardOverview overview = dashboardService.getDashboardOverview(userId, dateRange);
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            logger.error("Error fetching dashboard overview for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get real-time metrics
     */
    @GetMapping("/realtime")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RealtimeMetrics> getRealtimeMetrics(@RequestParam Long userId) {
        logger.debug("Fetching real-time metrics for user: {}", userId);
        
        try {
            RealtimeMetrics metrics = dashboardService.getRealtimeMetrics(userId);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error fetching real-time metrics for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get campaign performance summary
     */
    @GetMapping("/campaigns/performance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CampaignPerformanceSummary> getCampaignPerformance(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.debug("Fetching campaign performance for user: {} from {} to {}", userId, startDate, endDate);
        
        try {
            DateRange dateRange = new DateRange(startDate, endDate);
            CampaignPerformanceSummary summary = dashboardService.getCampaignPerformanceSummary(userId, dateRange);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error fetching campaign performance for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get content statistics
     */
    @GetMapping("/content/statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ContentStatistics> getContentStatistics(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.debug("Fetching content statistics for user: {} from {} to {}", userId, startDate, endDate);
        
        try {
            DateRange dateRange = new DateRange(startDate, endDate);
            ContentStatistics statistics = dashboardService.getContentStatistics(userId, dateRange);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error fetching content statistics for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get dashboard summary for quick overview
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getDashboardSummary(@RequestParam Long userId) {
        logger.debug("Fetching dashboard summary for user: {}", userId);
        
        try {
            RealtimeMetrics realtime = dashboardService.getRealtimeMetrics(userId);
            
            // Create a quick summary
            Map<String, Object> summary = Map.of(
                "activeCampaigns", realtime.getActiveCampaignsCount(),
                "contentToday", realtime.getContentGeneratedToday(),
                "weeklyImpressions", realtime.getWeeklyImpressions(),
                "weeklyClicks", realtime.getWeeklyClicks(),
                "weeklyClickThroughRate", realtime.getWeeklyClickThroughRate(),
                "pendingRecommendations", realtime.getPendingRecommendations(),
                "timestamp", realtime.getTimestamp()
            );
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error fetching dashboard summary for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get dashboard data for a specific date range with custom aggregation
     */
    @PostMapping("/custom")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getCustomDashboardData(
            @RequestBody DashboardRequest request) {
        
        logger.debug("Fetching custom dashboard data for user: {}", request.getUserId());
        
        try {
            DateRange dateRange = new DateRange(request.getStartDate(), request.getEndDate());
            
            Map<String, Object> customData = Map.of(
                "overview", dashboardService.getDashboardOverview(request.getUserId(), dateRange),
                "campaignPerformance", dashboardService.getCampaignPerformanceSummary(request.getUserId(), dateRange),
                "contentStatistics", dashboardService.getContentStatistics(request.getUserId(), dateRange),
                "realtimeMetrics", dashboardService.getRealtimeMetrics(request.getUserId())
            );
            
            return ResponseEntity.ok(customData);
        } catch (Exception e) {
            logger.error("Error fetching custom dashboard data for user: {}", request.getUserId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint for dashboard service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "DashboardService",
            "timestamp", LocalDate.now().toString()
        ));
    }

    /**
     * Request DTO for custom dashboard data
     */
    public static class DashboardRequest {
        private Long userId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String[] metrics;

        // Constructors
        public DashboardRequest() {}

        // Getters and setters
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public String[] getMetrics() {
            return metrics;
        }

        public void setMetrics(String[] metrics) {
            this.metrics = metrics;
        }
    }
}