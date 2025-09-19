package com.Human.Ai.D.makit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.dto.DashboardOverview;
import com.Human.Ai.D.makit.dto.RealtimeMetrics;
import com.Human.Ai.D.makit.repository.*;
import com.Human.Ai.D.makit.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class DashboardIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private CampaignAnalyticsRepository analyticsRepository;

    @Autowired
    private OptimizationRecommendationRepository recommendationRepository;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private User testUser;
    private Campaign testCampaign;
    private Content testContent;
    private CampaignAnalytics testAnalytics;
    private OptimizationRecommendation testRecommendation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // Create test user
        testUser = new User();
        testUser.setUsername("dashboardtestuser");
        testUser.setEmail("dashboard@test.com");
        testUser.setUserRole(UserRole.MARKETING_MANAGER);
        testUser = userRepository.save(testUser);

        // Create test campaign
        testCampaign = new Campaign();
        testCampaign.setName("Dashboard Test Campaign");
        testCampaign.setDescription("Test campaign for dashboard");
        testCampaign.setUser(testUser);
        testCampaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        testCampaign.setCreatedAt(LocalDateTime.now());
        testCampaign.setStartDate(LocalDateTime.now().minusDays(10));
        testCampaign.setEndDate(LocalDateTime.now().plusDays(10));
        testCampaign = campaignRepository.save(testCampaign);

        // Create test content
        testContent = new Content();
        testContent.setTitle("Dashboard Test Content");
        testContent.setContent("Test content for dashboard");
        testContent.setType(Content.ContentType.BLOG_POST);
        testContent.setUser(testUser);
        testContent.setCampaign(testCampaign);
        testContent.setStatus(Content.ContentStatus.PUBLISHED);
        testContent.setQualityScore(85.0);
        testContent.setCreatedAt(LocalDateTime.now());
        testContent = contentRepository.save(testContent);

        // Create test analytics
        testAnalytics = new CampaignAnalytics();
        testAnalytics.setCampaign(testCampaign);
        testAnalytics.setReportDate(LocalDate.now());
        testAnalytics.setImpressions(1000.0);
        testAnalytics.setClicks(50.0);
        testAnalytics.setConversions(5.0);
        testAnalytics.setCost(100.0);
        testAnalytics.setRevenue(500.0);
        testAnalytics.setClickThroughRate(5.0);
        testAnalytics.setConversionRate(10.0);
        testAnalytics.setReturnOnAdSpend(5.0);
        testAnalytics.setPerformanceScore(75.0);
        testAnalytics.setCalculatedAt(LocalDateTime.now());
        testAnalytics = analyticsRepository.save(testAnalytics);

        // Create test recommendation
        testRecommendation = new OptimizationRecommendation();
        testRecommendation.setCampaign(testCampaign);
        testRecommendation.setRecommendationType("BUDGET_OPTIMIZATION");
        testRecommendation.setDescription("Increase budget for better performance");
        testRecommendation.setActionRequired("Increase daily budget by 20%");
        testRecommendation.setExpectedImpact(15.0);
        testRecommendation.setPriority(OptimizationRecommendation.RecommendationPriority.HIGH);
        testRecommendation.setStatus(OptimizationRecommendation.RecommendationStatus.PENDING);
        testRecommendation.setGeneratedAt(LocalDateTime.now());
        testRecommendation = recommendationRepository.save(testRecommendation);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetDashboardOverview() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/dashboard/overview")
                .param("userId", testUser.getId().toString())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.campaignMetrics").exists())
                .andExpect(jsonPath("$.contentMetrics").exists())
                .andExpect(jsonPath("$.recentActivities").exists())
                .andExpect(jsonPath("$.performanceTrends").exists())
                .andExpect(jsonPath("$.topRecommendations").exists())
                .andExpect(jsonPath("$.overallPerformanceScore").isNumber())
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetRealtimeMetrics() throws Exception {
        mockMvc.perform(get("/api/dashboard/realtime")
                .param("userId", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.activeCampaignsCount").isNumber())
                .andExpect(jsonPath("$.contentGeneratedToday").isNumber())
                .andExpect(jsonPath("$.weeklyImpressions").isNumber())
                .andExpect(jsonPath("$.weeklyClicks").isNumber())
                .andExpect(jsonPath("$.pendingRecommendations").isNumber())
                .andExpect(jsonPath("$.weeklyClickThroughRate").isNumber())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetCampaignPerformance() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/dashboard/campaigns/performance")
                .param("userId", testUser.getId().toString())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.totalImpressions").isNumber())
                .andExpect(jsonPath("$.totalClicks").isNumber())
                .andExpect(jsonPath("$.totalConversions").isNumber())
                .andExpect(jsonPath("$.totalCost").isNumber())
                .andExpect(jsonPath("$.totalRevenue").isNumber())
                .andExpect(jsonPath("$.averageClickThroughRate").isNumber())
                .andExpect(jsonPath("$.averageConversionRate").isNumber())
                .andExpect(jsonPath("$.averageReturnOnAdSpend").isNumber())
                .andExpect(jsonPath("$.performanceGrade").isString())
                .andExpect(jsonPath("$.profitMargin").isNumber());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetContentStatistics() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/dashboard/content/statistics")
                .param("userId", testUser.getId().toString())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.contentByType").exists())
                .andExpect(jsonPath("$.contentByStatus").exists())
                .andExpect(jsonPath("$.averageQualityScore").isNumber())
                .andExpect(jsonPath("$.recentContent").isArray())
                .andExpect(jsonPath("$.totalContentCount").isNumber())
                .andExpect(jsonPath("$.publishedContentCount").isNumber())
                .andExpect(jsonPath("$.publishRate").isNumber())
                .andExpect(jsonPath("$.qualityGrade").isString());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                .param("userId", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCampaigns").isNumber())
                .andExpect(jsonPath("$.contentToday").isNumber())
                .andExpect(jsonPath("$.weeklyImpressions").isNumber())
                .andExpect(jsonPath("$.weeklyClicks").isNumber())
                .andExpect(jsonPath("$.weeklyClickThroughRate").isNumber())
                .andExpect(jsonPath("$.pendingRecommendations").isNumber())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetCustomDashboardData() throws Exception {
        Map<String, Object> request = Map.of(
                "userId", testUser.getId(),
                "startDate", LocalDate.now().minusDays(30).toString(),
                "endDate", LocalDate.now().toString(),
                "metrics", new String[]{"overview", "campaigns", "content"}
        );

        mockMvc.perform(post("/api/dashboard/custom")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview").exists())
                .andExpect(jsonPath("$.campaignPerformance").exists())
                .andExpect(jsonPath("$.contentStatistics").exists())
                .andExpect(jsonPath("$.realtimeMetrics").exists());
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/dashboard/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("DashboardService"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview")
                .param("userId", "1")
                .param("startDate", LocalDate.now().minusDays(30).toString())
                .param("endDate", LocalDate.now().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testInvalidDateRange() throws Exception {
        // Test with end date before start date
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(30);

        mockMvc.perform(get("/api/dashboard/overview")
                .param("userId", testUser.getId().toString())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Service should handle this gracefully
    }

    @Test
    @WithMockUser(roles = "USER")
    void testNonExistentUser() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/dashboard/overview")
                .param("userId", "99999")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Should return empty data, not error
                .andExpect(jsonPath("$.userId").value(99999))
                .andExpect(jsonPath("$.campaignMetrics").exists())
                .andExpect(jsonPath("$.contentMetrics").exists());
    }

    @Test
    void testServiceDirectly() {
        // Test the service directly to ensure it works without web layer
        DashboardService.DateRange dateRange = new DashboardService.DateRange(
                LocalDate.now().minusDays(30), LocalDate.now());

        DashboardOverview overview = dashboardService.getDashboardOverview(testUser.getId(), dateRange);
        assertNotNull(overview);
        assertEquals(testUser.getId(), overview.getUserId());

        RealtimeMetrics metrics = dashboardService.getRealtimeMetrics(testUser.getId());
        assertNotNull(metrics);
        assertEquals(testUser.getId(), metrics.getUserId());
    }

    private void assertNotNull(Object object) {
        org.junit.jupiter.api.Assertions.assertNotNull(object);
    }

    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}