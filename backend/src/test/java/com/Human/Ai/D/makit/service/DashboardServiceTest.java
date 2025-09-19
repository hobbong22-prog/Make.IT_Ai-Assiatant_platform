package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.dto.*;
import com.Human.Ai.D.makit.repository.*;
import com.Human.Ai.D.makit.service.DashboardService.DateRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignAnalyticsRepository analyticsRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OptimizationRecommendationRepository recommendationRepository;

    // CacheService is not mocked due to Java 24 compatibility issues with Mockito

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;
    private Campaign testCampaign;
    private Content testContent;
    private CampaignAnalytics testAnalytics;
    private OptimizationRecommendation testRecommendation;
    private DateRange testDateRange;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testCampaign = new Campaign();
        testCampaign.setId(1L);
        testCampaign.setName("Test Campaign");
        testCampaign.setUser(testUser);
        testCampaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        testCampaign.setCreatedAt(LocalDateTime.now());

        testContent = new Content();
        testContent.setId(1L);
        testContent.setTitle("Test Content");
        testContent.setUser(testUser);
        testContent.setType(Content.ContentType.BLOG_POST);
        testContent.setQualityScore(85.0);
        testContent.setCreatedAt(LocalDateTime.now());

        testAnalytics = new CampaignAnalytics();
        testAnalytics.setId(1L);
        testAnalytics.setCampaign(testCampaign);
        testAnalytics.setReportDate(LocalDate.now());
        testAnalytics.setImpressions(1000.0);
        testAnalytics.setClicks(50.0);
        testAnalytics.setConversions(5.0);
        testAnalytics.setCost(100.0);
        testAnalytics.setRevenue(500.0);
        testAnalytics.setPerformanceScore(75.0);

        testRecommendation = new OptimizationRecommendation();
        testRecommendation.setId(1L);
        testRecommendation.setCampaign(testCampaign);
        testRecommendation.setRecommendationType("BUDGET_OPTIMIZATION");
        testRecommendation.setDescription("Increase budget for better performance");
        testRecommendation.setExpectedImpact(15.0);
        testRecommendation.setPriority(OptimizationRecommendation.RecommendationPriority.HIGH);

        testDateRange = new DateRange(LocalDate.now().minusDays(30), LocalDate.now());
    }

    @Test
    void testGetDashboardOverview() {
        // Arrange
        when(campaignRepository.findByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(testCampaign));
        when(analyticsRepository.findByCampaignsAndDateRange(anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(testAnalytics));
        when(contentRepository.findByUserAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testContent));
        when(campaignRepository.findRecentByUser(eq(1L), anyInt()))
                .thenReturn(Arrays.asList(testCampaign));
        when(contentRepository.findRecentByUser(eq(1L), anyInt()))
                .thenReturn(Arrays.asList(testContent));
        when(analyticsRepository.findByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(testAnalytics));
        when(recommendationRepository.findTopByUserOrderByPriorityAndGeneratedAt(eq(1L), eq(5)))
                .thenReturn(Arrays.asList(testRecommendation));

        // Act
        DashboardOverview overview = dashboardService.getDashboardOverview(1L, testDateRange);

        // Assert
        assertNotNull(overview);
        assertEquals(1L, overview.getUserId());
        assertNotNull(overview.getCampaignMetrics());
        assertNotNull(overview.getContentMetrics());
        assertNotNull(overview.getRecentActivities());
        assertNotNull(overview.getPerformanceTrends());
        assertNotNull(overview.getTopRecommendations());
        assertTrue(overview.getOverallPerformanceScore() >= 0);

        verify(campaignRepository).findByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class));
        verify(analyticsRepository).findByCampaignsAndDateRange(anyList(), any(LocalDate.class), any(LocalDate.class));
        verify(contentRepository).findByUserAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetRealtimeMetrics() {
        // Arrange
        when(campaignRepository.countActiveCampaignsByUser(1L)).thenReturn(3);
        when(contentRepository.countByUserAndCreatedDate(eq(1L), any(LocalDate.class))).thenReturn(2);
        when(analyticsRepository.sumImpressionsByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(5000.0);
        when(analyticsRepository.sumClicksByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(250.0);
        when(recommendationRepository.countPendingByUser(1L)).thenReturn(4);

        // Act
        RealtimeMetrics metrics = dashboardService.getRealtimeMetrics(1L);

        // Assert
        assertNotNull(metrics);
        assertEquals(1L, metrics.getUserId());
        assertEquals(3, metrics.getActiveCampaignsCount());
        assertEquals(2, metrics.getContentGeneratedToday());
        assertEquals(5000.0, metrics.getWeeklyImpressions());
        assertEquals(250.0, metrics.getWeeklyClicks());
        assertEquals(4, metrics.getPendingRecommendations());
        assertEquals(5.0, metrics.getWeeklyClickThroughRate(), 0.01); // (250/5000)*100

        verify(campaignRepository).countActiveCampaignsByUser(1L);
        verify(contentRepository).countByUserAndCreatedDate(eq(1L), any(LocalDate.class));
        verify(analyticsRepository).sumImpressionsByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class));
        verify(analyticsRepository).sumClicksByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class));
        verify(recommendationRepository).countPendingByUser(1L);
    }

    @Test
    void testGetCampaignPerformanceSummary() {
        // Arrange
        List<CampaignAnalytics> analyticsList = Arrays.asList(testAnalytics);
        when(analyticsRepository.findByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(analyticsList);

        // Act
        CampaignPerformanceSummary summary = dashboardService.getCampaignPerformanceSummary(1L, testDateRange);

        // Assert
        assertNotNull(summary);
        assertEquals(1L, summary.getUserId());
        assertEquals(1000.0, summary.getTotalImpressions());
        assertEquals(50.0, summary.getTotalClicks());
        assertEquals(5.0, summary.getTotalConversions());
        assertEquals(100.0, summary.getTotalCost());
        assertEquals(500.0, summary.getTotalRevenue());
        assertEquals(5.0, summary.getAverageClickThroughRate(), 0.01); // (50/1000)*100
        assertEquals(10.0, summary.getAverageConversionRate(), 0.01); // (5/50)*100
        assertEquals(5.0, summary.getAverageReturnOnAdSpend(), 0.01); // 500/100
        assertEquals("A", summary.getPerformanceGrade()); // ROAS >= 4.0
        assertNotNull(summary.getTopPerformingCampaigns());

        verify(analyticsRepository).findByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testGetContentStatistics() {
        // Arrange
        testContent.setStatus(Content.ContentStatus.PUBLISHED);
        List<Content> contentList = Arrays.asList(testContent);
        when(contentRepository.findByUserAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(contentList);

        // Act
        ContentStatistics stats = dashboardService.getContentStatistics(1L, testDateRange);

        // Assert
        assertNotNull(stats);
        assertEquals(1L, stats.getUserId());
        assertNotNull(stats.getContentByType());
        assertNotNull(stats.getContentByStatus());
        assertEquals(85.0, stats.getAverageQualityScore());
        assertEquals("B", stats.getQualityGrade()); // 80 <= score < 90
        assertNotNull(stats.getRecentContent());
        assertEquals(1, stats.getTotalContentCount());
        assertEquals(1, stats.getPublishedContentCount());
        assertEquals(100.0, stats.getPublishRate(), 0.01); // (1/1)*100

        verify(contentRepository).findByUserAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetRealtimeMetricsWithZeroImpressions() {
        // Arrange
        when(campaignRepository.countActiveCampaignsByUser(1L)).thenReturn(0);
        when(contentRepository.countByUserAndCreatedDate(eq(1L), any(LocalDate.class))).thenReturn(0);
        when(analyticsRepository.sumImpressionsByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0.0);
        when(analyticsRepository.sumClicksByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0.0);
        when(recommendationRepository.countPendingByUser(1L)).thenReturn(0);

        // Act
        RealtimeMetrics metrics = dashboardService.getRealtimeMetrics(1L);

        // Assert
        assertNotNull(metrics);
        assertEquals(0, metrics.getActiveCampaignsCount());
        assertEquals(0, metrics.getContentGeneratedToday());
        assertEquals(0.0, metrics.getWeeklyImpressions());
        assertEquals(0.0, metrics.getWeeklyClicks());
        assertEquals(0, metrics.getPendingRecommendations());
        assertEquals(0.0, metrics.getWeeklyClickThroughRate()); // Should handle division by zero
    }

    @Test
    void testGetCampaignPerformanceSummaryWithEmptyData() {
        // Arrange
        when(analyticsRepository.findByUserAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList());

        // Act
        CampaignPerformanceSummary summary = dashboardService.getCampaignPerformanceSummary(1L, testDateRange);

        // Assert
        assertNotNull(summary);
        assertEquals(1L, summary.getUserId());
        assertEquals(0.0, summary.getTotalImpressions());
        assertEquals(0.0, summary.getTotalClicks());
        assertEquals(0.0, summary.getTotalConversions());
        assertEquals(0.0, summary.getTotalCost());
        assertEquals(0.0, summary.getTotalRevenue());
        assertEquals(0.0, summary.getAverageClickThroughRate());
        assertEquals(0.0, summary.getAverageConversionRate());
        assertEquals(0.0, summary.getAverageReturnOnAdSpend());
        assertEquals("F", summary.getPerformanceGrade()); // ROAS = 0
    }

    @Test
    void testGetContentStatisticsWithEmptyData() {
        // Arrange
        when(contentRepository.findByUserAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // Act
        ContentStatistics stats = dashboardService.getContentStatistics(1L, testDateRange);

        // Assert
        assertNotNull(stats);
        assertEquals(1L, stats.getUserId());
        assertNotNull(stats.getContentByType());
        assertNotNull(stats.getContentByStatus());
        assertEquals(0.0, stats.getAverageQualityScore());
        assertEquals("F", stats.getQualityGrade()); // score = 0
        assertNotNull(stats.getRecentContent());
        assertEquals(0, stats.getTotalContentCount());
        assertEquals(0, stats.getPublishedContentCount());
        assertEquals(0.0, stats.getPublishRate());
    }

    @Test
    void testDateRangeHashCode() {
        // Arrange
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        DateRange range1 = new DateRange(start, end);
        DateRange range2 = new DateRange(start, end);
        DateRange range3 = new DateRange(start, LocalDate.of(2024, 2, 1));

        // Act & Assert
        assertEquals(range1.hashCode(), range2.hashCode());
        assertNotEquals(range1.hashCode(), range3.hashCode());
    }

    @Test
    void testPerformanceTrendCalculations() {
        // Arrange
        PerformanceTrend trend = new PerformanceTrend(
                LocalDate.now(), 1000.0, 50.0, 5.0, 100.0, 500.0);

        // Act & Assert
        assertEquals(5.0, trend.getClickThroughRate(), 0.01); // (50/1000)*100
        assertEquals(10.0, trend.getConversionRate(), 0.01); // (5/50)*100
        assertEquals(5.0, trend.getReturnOnAdSpend(), 0.01); // 500/100
        assertEquals(2.0, trend.getCostPerClick(), 0.01); // 100/50
        assertEquals(20.0, trend.getCostPerConversion(), 0.01); // 100/5
    }

    @Test
    void testActivitySummaryMethods() {
        // Arrange
        ActivitySummary activity = new ActivitySummary(
                "CAMPAIGN_CREATED", 
                "Test campaign created", 
                LocalDateTime.now().minusMinutes(30));

        // Act & Assert
        assertEquals("📊", activity.getIcon());
        assertTrue(activity.getRelativeTime().contains("minutes ago"));
        
        // Test different activity types
        activity.setActivityType("CONTENT_GENERATED");
        assertEquals("📝", activity.getIcon());
        
        activity.setActivityType("UNKNOWN_TYPE");
        assertEquals("ℹ️", activity.getIcon());
    }

    @Test
    void testCampaignMetricsSummaryCalculations() {
        // Arrange
        CampaignMetricsSummary summary = new CampaignMetricsSummary(10, 7, 1000.0, 1500.0);

        // Act & Assert
        assertEquals(500.0, summary.getProfit(), 0.01); // 1500 - 1000
        assertEquals(33.33, summary.getProfitMargin(), 0.01); // (500/1500)*100
        assertEquals(70.0, summary.getCampaignUtilizationRate(), 0.01); // (7/10)*100
    }

    @Test
    void testContentMetricsSummaryMethods() {
        // Arrange
        ContentMetricsSummary summary = new ContentMetricsSummary(100, 80, 85.0);

        // Act & Assert
        assertEquals(80.0, summary.getPublishRate(), 0.01); // (80/100)*100
        assertEquals(20, summary.getDraftContent()); // 100 - 80
        assertEquals("B", summary.getQualityGrade()); // 80 <= score < 90
        assertTrue(summary.isHealthyProduction()); // totalContent > 0 && publishRate >= 70 && qualityScore >= 75
        
        // Test unhealthy production
        ContentMetricsSummary unhealthySummary = new ContentMetricsSummary(100, 50, 60.0);
        assertFalse(unhealthySummary.isHealthyProduction());
    }
}