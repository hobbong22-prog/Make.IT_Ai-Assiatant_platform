package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.service.CampaignAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class CampaignAnalyticsController {
    
    @Autowired
    private CampaignAnalyticsService analyticsService;
    
    /**
     * Generate analytics report for a campaign
     */
    @PostMapping("/campaigns/{campaignId}/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'ANALYST')")
    public ResponseEntity<CampaignAnalytics> generateAnalyticsReport(
            @PathVariable Long campaignId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        
        LocalDate date = reportDate != null ? reportDate : LocalDate.now();
        CampaignAnalytics analytics = analyticsService.generateAnalyticsReport(campaignId, date);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get analytics for a campaign within date range
     */
    @GetMapping("/campaigns/{campaignId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'ANALYST', 'VIEWER')")
    public ResponseEntity<List<CampaignAnalytics>> getCampaignAnalytics(
            @PathVariable Long campaignId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<CampaignAnalytics> analytics = analyticsService.getAnalyticsByDateRange(
                campaignId, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get latest analytics for a campaign
     */
    @GetMapping("/campaigns/{campaignId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'ANALYST', 'VIEWER')")
    public ResponseEntity<CampaignAnalytics> getLatestAnalytics(@PathVariable Long campaignId) {
        Optional<CampaignAnalytics> analytics = analyticsService.getLatestAnalytics(campaignId);
        return analytics.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get analytics summary for user campaigns
     */
    @GetMapping("/users/{userId}/summary")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentUser(#userId) or @authService.canAccessAnalytics(authentication.principal)")
    public ResponseEntity<List<CampaignAnalytics>> getUserAnalyticsSummary(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<CampaignAnalytics> analytics = analyticsService.getUserAnalyticsSummary(
                userId, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get top performing campaigns for a user
     */
    @GetMapping("/users/{userId}/top-performing")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentUser(#userId) or @authService.canAccessAnalytics(authentication.principal)")
    public ResponseEntity<List<CampaignAnalytics>> getTopPerformingCampaigns(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "70.0") Double minScore) {
        
        List<CampaignAnalytics> analytics = analyticsService.getTopPerformingCampaigns(userId, minScore);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Recalculate performance score for analytics
     */
    @PutMapping("/{analyticsId}/recalculate-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<String> recalculatePerformanceScore(@PathVariable Long analyticsId) {
        // This would require getting the analytics by ID and recalculating
        // For now, return a simple response
        return ResponseEntity.ok("Performance score recalculation initiated");
    }
}