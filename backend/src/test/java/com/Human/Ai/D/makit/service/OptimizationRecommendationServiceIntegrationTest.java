package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.domain.OptimizationRecommendation;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.CampaignAnalyticsRepository;
import com.Human.Ai.D.makit.repository.CampaignRepository;
import com.Human.Ai.D.makit.repository.OptimizationRecommendationRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OptimizationRecommendationServiceIntegrationTest {
    
    @Autowired
    private OptimizationRecommendationService recommendationService;
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private CampaignAnalyticsRepository analyticsRepository;
    
    @Autowired
    private OptimizationRecommendationRepository recommendationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @MockBean
    private BedrockService bedrockService;
    
    private User testUser;
    private Campaign testCampaign;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);
        
        // Create test campaign
        testCampaign = new Campaign("Test Campaign", "Test Description", 
                                   Campaign.CampaignType.SOCIAL_MEDIA, testUser);
        testCampaign.setBudget(1000.0);
        testCampaign.setTargetAudience("Young Adults");
        testCampaign = campaignRepository.save(testCampaign);
        
        // Create test analytics data with poor performance to trigger recommendations
        createTestAnalyticsData();
        
        // Mock Bedrock service
        when(bedrockService.generateText(anyString(), anyString()))
                .thenReturn("AI-generated optimization recommendation");
    }
    
    private void createTestAnalyticsData() {
        for (int i = 1; i <= 7; i++) {
            CampaignAnalytics analytics = new CampaignAnalytics(testCampaign, LocalDate.now().minusDays(i));
            analytics.setImpressions(1000.0);
            analytics.setClicks(15.0); // Low CTR (1.5%)
            analytics.setConversions(1.0); // Low conversion rate
            analytics.setCost(100.0);
            analytics.setRevenue(150.0); // Low ROAS (1.5)
            analytics.calculateMetrics();
            analytics.setPerformanceScore(45.0); // Poor performance
            analyticsRepository.save(analytics);
        }
    }
    
    @Test
    void testGenerateRecommendations_Success() throws Exception {
        // When
        CompletableFuture<List<OptimizationRecommendation>> future = 
                recommendationService.generateRecommendations(testCampaign.getId());
        List<OptimizationRecommendation> recommendations = future.get();
        
        // Then
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        
        // Verify recommendations were saved to database
        List<OptimizationRecommendation> savedRecommendations = 
                recommendationRepository.findByCampaign(testCampaign);
        assertFalse(savedRecommendations.isEmpty());
        
        // Check for expected recommendation types based on poor performance
        boolean hasLowCTRRecommendation = savedRecommendations.stream()
                .anyMatch(r -> "LOW_CTR".equals(r.getRecommendationType()));
        boolean hasLowConversionRecommendation = savedRecommendations.stream()
                .anyMatch(r -> "LOW_CONVERSION".equals(r.getRecommendationType()));
        boolean hasLowROASRecommendation = savedRecommendations.stream()
                .anyMatch(r -> "LOW_ROAS".equals(r.getRecommendationType()));
        
        assertTrue(hasLowCTRRecommendation || hasLowConversionRecommendation || hasLowROASRecommendation);
    }
    
    @Test
    void testGetActiveRecommendations() throws Exception {
        // Given - Generate recommendations first
        recommendationService.generateRecommendations(testCampaign.getId()).get();
        
        // When
        List<OptimizationRecommendation> activeRecommendations = 
                recommendationService.getActiveRecommendations(testCampaign.getId());
        
        // Then
        assertNotNull(activeRecommendations);
        activeRecommendations.forEach(recommendation -> {
            assertEquals(OptimizationRecommendation.RecommendationStatus.PENDING, 
                        recommendation.getStatus());
            assertEquals(testCampaign.getId(), recommendation.getCampaign().getId());
        });
    }
    
    @Test
    void testGetRecommendationsByUserAndStatus() throws Exception {
        // Given - Generate recommendations first
        recommendationService.generateRecommendations(testCampaign.getId()).get();
        
        // When
        List<OptimizationRecommendation> pendingRecommendations = 
                recommendationService.getRecommendationsByUserAndStatus(
                        testUser.getId(), OptimizationRecommendation.RecommendationStatus.PENDING);
        
        // Then
        assertNotNull(pendingRecommendations);
        pendingRecommendations.forEach(recommendation -> {
            assertEquals(OptimizationRecommendation.RecommendationStatus.PENDING, 
                        recommendation.getStatus());
            assertEquals(testUser.getId(), recommendation.getCampaign().getUser().getId());
        });
    }
    
    @Test
    void testGetHighPriorityRecommendations() throws Exception {
        // Given - Generate recommendations first
        recommendationService.generateRecommendations(testCampaign.getId()).get();
        
        // When
        List<OptimizationRecommendation> highPriorityRecommendations = 
                recommendationService.getHighPriorityRecommendations(testUser.getId());
        
        // Then
        assertNotNull(highPriorityRecommendations);
        highPriorityRecommendations.forEach(recommendation -> {
            assertTrue(recommendation.getPriority() == OptimizationRecommendation.RecommendationPriority.HIGH ||
                      recommendation.getPriority() == OptimizationRecommendation.RecommendationPriority.CRITICAL);
        });
    }
    
    @Test
    void testImplementRecommendation() throws Exception {
        // Given - Generate recommendations first
        List<OptimizationRecommendation> recommendations = 
                recommendationService.generateRecommendations(testCampaign.getId()).get();
        assertFalse(recommendations.isEmpty());
        
        OptimizationRecommendation recommendation = recommendations.get(0);
        Long recommendationId = recommendation.getId();
        
        // When
        OptimizationRecommendation implementedRecommendation = 
                recommendationService.implementRecommendation(recommendationId);
        
        // Then
        assertNotNull(implementedRecommendation);
        assertEquals(OptimizationRecommendation.RecommendationStatus.IMPLEMENTED, 
                    implementedRecommendation.getStatus());
        assertNotNull(implementedRecommendation.getImplementedAt());
        assertTrue(implementedRecommendation.isImplemented());
    }
    
    @Test
    void testDismissRecommendation() throws Exception {
        // Given - Generate recommendations first
        List<OptimizationRecommendation> recommendations = 
                recommendationService.generateRecommendations(testCampaign.getId()).get();
        assertFalse(recommendations.isEmpty());
        
        OptimizationRecommendation recommendation = recommendations.get(0);
        Long recommendationId = recommendation.getId();
        
        // When
        OptimizationRecommendation dismissedRecommendation = 
                recommendationService.dismissRecommendation(recommendationId);
        
        // Then
        assertNotNull(dismissedRecommendation);
        assertEquals(OptimizationRecommendation.RecommendationStatus.DISMISSED, 
                    dismissedRecommendation.getStatus());
    }
    
    @Test
    void testGetRecommendationStats() throws Exception {
        // Given - Generate and implement some recommendations
        List<OptimizationRecommendation> recommendations = 
                recommendationService.generateRecommendations(testCampaign.getId()).get();
        assertFalse(recommendations.isEmpty());
        
        // Implement one recommendation
        if (!recommendations.isEmpty()) {
            recommendationService.implementRecommendation(recommendations.get(0).getId());
        }
        
        // When
        Map<String, Object> stats = recommendationService.getRecommendationStats(testUser.getId());
        
        // Then
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalPending"));
        assertTrue(stats.containsKey("totalImplemented"));
        assertTrue(stats.containsKey("categoryBreakdown"));
        
        Long totalPending = (Long) stats.get("totalPending");
        Long totalImplemented = (Long) stats.get("totalImplemented");
        
        assertTrue(totalPending >= 0);
        assertTrue(totalImplemented >= 1); // We implemented one
        
        @SuppressWarnings("unchecked")
        Map<String, Long> categoryBreakdown = (Map<String, Long>) stats.get("categoryBreakdown");
        assertNotNull(categoryBreakdown);
    }
    
    @Test
    void testRecommendationPriorityCalculation() throws Exception {
        // Given - Generate recommendations
        List<OptimizationRecommendation> recommendations = 
                recommendationService.generateRecommendations(testCampaign.getId()).get();
        assertFalse(recommendations.isEmpty());
        
        // When & Then - Verify priority calculation
        for (OptimizationRecommendation recommendation : recommendations) {
            assertNotNull(recommendation.getPriority());
            
            double priorityScore = recommendation.calculatePriorityScore();
            assertTrue(priorityScore >= 0);
            
            // Verify priority assignment logic
            if (priorityScore >= 80) {
                assertEquals(OptimizationRecommendation.RecommendationPriority.CRITICAL, 
                           recommendation.getPriority());
            } else if (priorityScore >= 60) {
                assertEquals(OptimizationRecommendation.RecommendationPriority.HIGH, 
                           recommendation.getPriority());
            }
        }
    }
    
    @Test
    void testRecommendationExpiration() {
        // Given - Create a recommendation with expiration
        OptimizationRecommendation recommendation = new OptimizationRecommendation(
                testCampaign, "TEST", "Test Recommendation");
        recommendation.setExpiresAt(LocalDate.now().minusDays(1).atStartOfDay());
        
        // When & Then
        assertTrue(recommendation.isExpired());
        
        // Test non-expired recommendation
        recommendation.setExpiresAt(LocalDate.now().plusDays(1).atStartOfDay());
        assertFalse(recommendation.isExpired());
    }
}