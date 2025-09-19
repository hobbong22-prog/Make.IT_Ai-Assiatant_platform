package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.dto.*;
import com.Human.Ai.D.makit.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating and providing dashboard data from multiple sources
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignAnalyticsRepository analyticsRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OptimizationRecommendationRepository recommendationRepository;

    @Autowired
    private CacheService cacheService;

    /**
     * Get comprehensive dashboard overview for a user
     */
    @Cacheable(value = "dashboardOverview", key = "#userId + '_' + #dateRange.hashCode()")
    public DashboardOverview getDashboardOverview(Long userId, DateRange dateRange) {
        logger.info("Generating dashboard overview for user: {} in date range: {}", userId, dateRange);

        DashboardOverview overview = new DashboardOverview();
        overview.setUserId(userId);
        overview.setDateRange(dateRange);
        overview.setGeneratedAt(LocalDateTime.now());

        // Aggregate campaign metrics
        overview.setCampaignMetrics(aggregateCampaignMetrics(userId, dateRange));

        // Aggregate content metrics
        overview.setContentMetrics(aggregateContentMetrics(userId, dateRange));

        // Get recent activities
        overview.setRecentActivities(getRecentActivities(userId, dateRange));

        // Get performance trends
        overview.setPerformanceTrends(getPerformanceTrends(userId, dateRange));

        // Get top recommendations
        overview.setTopRecommendations(getTopRecommendations(userId, 5));

        // Calculate overall performance score
        overview.setOverallPerformanceScore(calculateOverallPerformanceScore(overview));

        return overview;
    }

    /**
     * Get real-time metrics summary
     */
    @Cacheable(value = "realtimeMetrics", key = "#userId", unless = "#result == null")
    public RealtimeMetrics getRealtimeMetrics(Long userId) {
        logger.debug("Fetching real-time metrics for user: {}", userId);

        RealtimeMetrics metrics = new RealtimeMetrics();
        metrics.setUserId(userId);
        metrics.setTimestamp(LocalDateTime.now());

        // Active campaigns count
        metrics.setActiveCampaignsCount(campaignRepository.countActiveCampaignsByUser(userId));

        // Content generated today
        LocalDate today = LocalDate.now();
        metrics.setContentGeneratedToday(contentRepository.countByUserAndCreatedDate(userId, today));

        // Total impressions this week
        LocalDate weekStart = today.minusDays(7);
        metrics.setWeeklyImpressions(analyticsRepository.sumImpressionsByUserAndDateRange(userId, weekStart, today));

        // Total clicks this week
        metrics.setWeeklyClicks(analyticsRepository.sumClicksByUserAndDateRange(userId, weekStart, today));

        // Pending recommendations
        metrics.setPendingRecommendations(recommendationRepository.countPendingByUser(userId));

        return metrics;
    }

    /**
     * Get campaign performance summary
     */
    @Cacheable(value = "campaignPerformance", key = "#userId + '_' + #dateRange.hashCode()")
    public CampaignPerformanceSummary getCampaignPerformanceSummary(Long userId, DateRange dateRange) {
        logger.debug("Generating campaign performance summary for user: {}", userId);

        List<CampaignAnalytics> analytics = analyticsRepository.findByUserAndDateRange(
                userId, dateRange.getStartDate(), dateRange.getEndDate());

        CampaignPerformanceSummary summary = new CampaignPerformanceSummary();
        summary.setUserId(userId);
        summary.setDateRange(dateRange);

        if (analytics.isEmpty()) {
            return summary;
        }

        // Aggregate totals
        double totalImpressions = analytics.stream().mapToDouble(a -> a.getImpressions() != null ? a.getImpressions() : 0).sum();
        double totalClicks = analytics.stream().mapToDouble(a -> a.getClicks() != null ? a.getClicks() : 0).sum();
        double totalConversions = analytics.stream().mapToDouble(a -> a.getConversions() != null ? a.getConversions() : 0).sum();
        double totalCost = analytics.stream().mapToDouble(a -> a.getCost() != null ? a.getCost() : 0).sum();
        double totalRevenue = analytics.stream().mapToDouble(a -> a.getRevenue() != null ? a.getRevenue() : 0).sum();

        summary.setTotalImpressions(totalImpressions);
        summary.setTotalClicks(totalClicks);
        summary.setTotalConversions(totalConversions);
        summary.setTotalCost(totalCost);
        summary.setTotalRevenue(totalRevenue);

        // Calculate averages
        summary.setAverageClickThroughRate(totalImpressions > 0 ? (totalClicks / totalImpressions) * 100 : 0);
        summary.setAverageConversionRate(totalClicks > 0 ? (totalConversions / totalClicks) * 100 : 0);
        summary.setAverageReturnOnAdSpend(totalCost > 0 ? totalRevenue / totalCost : 0);

        // Get top performing campaigns
        summary.setTopPerformingCampaigns(analytics.stream()
                .sorted((a1, a2) -> Double.compare(
                        a2.getPerformanceScore() != null ? a2.getPerformanceScore() : 0,
                        a1.getPerformanceScore() != null ? a1.getPerformanceScore() : 0))
                .limit(5)
                .collect(Collectors.toList()));

        return summary;
    }

    /**
     * Get content generation statistics
     */
    @Cacheable(value = "contentStats", key = "#userId + '_' + #dateRange.hashCode()")
    public ContentStatistics getContentStatistics(Long userId, DateRange dateRange) {
        logger.debug("Generating content statistics for user: {}", userId);

        List<Content> contents = contentRepository.findByUserAndDateRange(
                userId, dateRange.getStartDate().atStartOfDay(), dateRange.getEndDate().atTime(23, 59, 59));

        ContentStatistics stats = new ContentStatistics();
        stats.setUserId(userId);
        stats.setDateRange(dateRange);

        if (contents.isEmpty()) {
            return stats;
        }

        // Count by content type
        Map<String, Long> contentByType = contents.stream()
                .collect(Collectors.groupingBy(c -> c.getContentType() != null ? c.getContentType() : "UNKNOWN", Collectors.counting()));
        stats.setContentByType(contentByType);

        // Count by status
        Map<String, Long> contentByStatus = contents.stream()
                .collect(Collectors.groupingBy(c -> c.getStatus() != null ? c.getStatus().toString() : "UNKNOWN", Collectors.counting()));
        stats.setContentByStatus(contentByStatus);

        // Calculate quality scores
        OptionalDouble avgQualityScore = contents.stream()
                .filter(c -> c.getQualityScore() != null)
                .mapToDouble(Content::getQualityScore)
                .average();
        stats.setAverageQualityScore(avgQualityScore.orElse(0.0));

        // Recent content
        stats.setRecentContent(contents.stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList()));

        return stats;
    }

    /**
     * Aggregate campaign metrics for dashboard
     */
    private CampaignMetricsSummary aggregateCampaignMetrics(Long userId, DateRange dateRange) {
        List<Campaign> campaigns = campaignRepository.findByUserAndDateRange(userId, dateRange.getStartDate(), dateRange.getEndDate());
        
        CampaignMetricsSummary summary = new CampaignMetricsSummary();
        summary.setTotalCampaigns(campaigns.size());
        summary.setActiveCampaigns((int) campaigns.stream().filter(Campaign::isActive).count());
        
        // Get analytics for these campaigns
        List<CampaignAnalytics> analytics = analyticsRepository.findByCampaignsAndDateRange(
                campaigns, dateRange.getStartDate(), dateRange.getEndDate());
        
        if (!analytics.isEmpty()) {
            double totalSpend = analytics.stream().mapToDouble(a -> a.getCost() != null ? a.getCost() : 0).sum();
            double totalRevenue = analytics.stream().mapToDouble(a -> a.getRevenue() != null ? a.getRevenue() : 0).sum();
            
            summary.setTotalSpend(totalSpend);
            summary.setTotalRevenue(totalRevenue);
            summary.setAverageRoas(totalSpend > 0 ? totalRevenue / totalSpend : 0);
        }
        
        return summary;
    }

    /**
     * Aggregate content metrics for dashboard
     */
    private ContentMetricsSummary aggregateContentMetrics(Long userId, DateRange dateRange) {
        List<Content> contents = contentRepository.findByUserAndDateRange(
                userId, dateRange.getStartDate().atStartOfDay(), dateRange.getEndDate().atTime(23, 59, 59));
        
        ContentMetricsSummary summary = new ContentMetricsSummary();
        summary.setTotalContent(contents.size());
        summary.setPublishedContent((int) contents.stream().filter(c -> "PUBLISHED".equals(c.getStatus().toString())).count());
        
        OptionalDouble avgQuality = contents.stream()
                .filter(c -> c.getQualityScore() != null)
                .mapToDouble(Content::getQualityScore)
                .average();
        summary.setAverageQualityScore(avgQuality.orElse(0.0));
        
        return summary;
    }

    /**
     * Get recent activities for dashboard
     */
    private List<ActivitySummary> getRecentActivities(Long userId, DateRange dateRange) {
        List<ActivitySummary> activities = new ArrayList<>();
        
        // Recent campaigns
        List<Campaign> recentCampaigns = campaignRepository.findRecentByUser(userId, 5);
        for (Campaign campaign : recentCampaigns) {
            activities.add(new ActivitySummary(
                    "CAMPAIGN_CREATED",
                    "Campaign '" + campaign.getName() + "' was created",
                    campaign.getCreatedAt()
            ));
        }
        
        // Recent content
        List<Content> recentContent = contentRepository.findRecentByUser(userId, 5);
        for (Content content : recentContent) {
            activities.add(new ActivitySummary(
                    "CONTENT_GENERATED",
                    "Content '" + content.getTitle() + "' was generated",
                    content.getCreatedAt()
            ));
        }
        
        // Sort by timestamp descending
        activities.sort((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()));
        
        return activities.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * Get performance trends
     */
    private List<PerformanceTrend> getPerformanceTrends(Long userId, DateRange dateRange) {
        List<CampaignAnalytics> analytics = analyticsRepository.findByUserAndDateRange(
                userId, dateRange.getStartDate(), dateRange.getEndDate());
        
        Map<LocalDate, List<CampaignAnalytics>> analyticsByDate = analytics.stream()
                .collect(Collectors.groupingBy(CampaignAnalytics::getReportDate));
        
        return analyticsByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<CampaignAnalytics> dayAnalytics = entry.getValue();
                    
                    double totalImpressions = dayAnalytics.stream().mapToDouble(a -> a.getImpressions() != null ? a.getImpressions() : 0).sum();
                    double totalClicks = dayAnalytics.stream().mapToDouble(a -> a.getClicks() != null ? a.getClicks() : 0).sum();
                    double totalConversions = dayAnalytics.stream().mapToDouble(a -> a.getConversions() != null ? a.getConversions() : 0).sum();
                    
                    return new PerformanceTrend(date, totalImpressions, totalClicks, totalConversions);
                })
                .sorted(Comparator.comparing(PerformanceTrend::getDate))
                .collect(Collectors.toList());
    }

    /**
     * Get top recommendations
     */
    private List<OptimizationRecommendation> getTopRecommendations(Long userId, int limit) {
        return recommendationRepository.findTopByUserOrderByPriorityAndGeneratedAt(userId, limit);
    }

    /**
     * Calculate overall performance score
     */
    private double calculateOverallPerformanceScore(DashboardOverview overview) {
        double score = 0.0;
        int factors = 0;
        
        // Campaign performance factor
        if (overview.getCampaignMetrics().getAverageRoas() > 0) {
            score += Math.min(overview.getCampaignMetrics().getAverageRoas() * 20, 40);
            factors++;
        }
        
        // Content quality factor
        if (overview.getContentMetrics().getAverageQualityScore() > 0) {
            score += overview.getContentMetrics().getAverageQualityScore() * 30;
            factors++;
        }
        
        // Activity factor (based on content generation)
        if (overview.getContentMetrics().getTotalContent() > 0) {
            score += Math.min(overview.getContentMetrics().getTotalContent() * 2, 30);
            factors++;
        }
        
        return factors > 0 ? score / factors : 0.0;
    }

    // Inner classes for data transfer objects
    public static class DateRange {
        private LocalDate startDate;
        private LocalDate endDate;
        
        public DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
        
        // Getters and setters
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        
        @Override
        public int hashCode() {
            return Objects.hash(startDate, endDate);
        }
    }
}