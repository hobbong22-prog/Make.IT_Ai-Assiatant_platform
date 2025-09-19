package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.CampaignAnalyticsRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictiveAnalyticsEngineTest {
    
    @Mock
    private CampaignAnalyticsRepository analyticsRepository;
    
    @Mock
    private BedrockService bedrockService;
    
    @Mock
    private DataPreprocessingService dataPreprocessingService;
    
    @InjectMocks
    private PredictiveAnalyticsEngine predictiveEngine;
    
    private Campaign testCampaign;
    private List<CampaignAnalytics> testAnalyticsData;
    
    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        testCampaign = new Campaign();
        testCampaign.setId(1L);
        testCampaign.setName("Test Campaign");
        testCampaign.setType(Campaign.CampaignType.SOCIAL_MEDIA);
        testCampaign.setTargetAudience("Young Adults");
        testCampaign.setBudget(1000.0);
        testCampaign.setUser(testUser);
        
        // Create test analytics data
        testAnalyticsData = Arrays.asList(
                createTestAnalytics(LocalDate.now().minusDays(7), 1000.0, 50.0, 5.0, 100.0, 500.0),
                createTestAnalytics(LocalDate.now().minusDays(6), 1100.0, 55.0, 6.0, 110.0, 600.0),
                createTestAnalytics(LocalDate.now().minusDays(5), 1200.0, 60.0, 7.0, 120.0, 700.0),
                createTestAnalytics(LocalDate.now().minusDays(4), 1150.0, 58.0, 6.0, 115.0, 650.0),
                createTestAnalytics(LocalDate.now().minusDays(3), 1300.0, 65.0, 8.0, 130.0, 800.0)
        );
    }
    
    private CampaignAnalytics createTestAnalytics(LocalDate date, Double impressions, Double clicks, 
                                                 Double conversions, Double cost, Double revenue) {
        CampaignAnalytics analytics = new CampaignAnalytics(testCampaign, date);
        analytics.setImpressions(impressions);
        analytics.setClicks(clicks);
        analytics.setConversions(conversions);
        analytics.setCost(cost);
        analytics.setRevenue(revenue);
        analytics.calculateMetrics();
        analytics.setPerformanceScore(75.0);
        return analytics;
    }
    
    @Test
    void testPredictCampaignPerformance_Success() throws Exception {
        // Given
        int daysAhead = 7;
        Map<String, Object> preprocessedData = Map.of("dataPoints", 5, "aggregatedMetrics", Map.of());
        
        when(analyticsRepository.findByCampaignAndDateRange(eq(testCampaign), any(), any()))
                .thenReturn(testAnalyticsData);
        when(dataPreprocessingService.preprocessForPrediction(testAnalyticsData))
                .thenReturn(preprocessedData);
        when(bedrockService.generateText(anyString(), anyString()))
                .thenReturn("Predicted performance: Impressions: 1400, Clicks: 70, Conversions: 9, Cost: 140, Revenue: 900");
        
        // When
        CompletableFuture<PredictiveAnalyticsEngine.PredictionResult> future = 
                predictiveEngine.predictCampaignPerformance(testCampaign, daysAhead);
        PredictiveAnalyticsEngine.PredictionResult result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(daysAhead, result.getPredictionPeriod());
        assertNotNull(result.getRawPrediction());
        verify(bedrockService).generateText(anyString(), eq("claude-3-sonnet-20240229-v1:0"));
    }
    
    @Test
    void testPredictCampaignPerformance_InsufficientData() throws Exception {
        // Given
        int daysAhead = 7;
        
        when(analyticsRepository.findByCampaignAndDateRange(eq(testCampaign), any(), any()))
                .thenReturn(Arrays.asList()); // Empty list
        
        // When
        CompletableFuture<PredictiveAnalyticsEngine.PredictionResult> future = 
                predictiveEngine.predictCampaignPerformance(testCampaign, daysAhead);
        PredictiveAnalyticsEngine.PredictionResult result = future.get();
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertTrue(result.getMessage().contains("Insufficient historical data"));
        verify(bedrockService, never()).generateText(anyString(), anyString());
    }
    
    @Test
    void testAnalyzeTrends_Success() throws Exception {
        // Given
        when(analyticsRepository.findByCampaignAndDateRange(eq(testCampaign), any(), any()))
                .thenReturn(testAnalyticsData);
        when(bedrockService.generateText(anyString(), anyString()))
                .thenReturn("Trend analysis: Performance is improving with consistent growth in key metrics");
        
        // When
        CompletableFuture<PredictiveAnalyticsEngine.TrendAnalysis> future = 
                predictiveEngine.analyzeTrends(testCampaign);
        PredictiveAnalyticsEngine.TrendAnalysis result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getTrendMetrics());
        assertNotNull(result.getAiInsights());
        verify(bedrockService).generateText(anyString(), eq("claude-3-haiku-20240307-v1:0"));
    }
    
    @Test
    void testAnalyzeTrends_InsufficientData() throws Exception {
        // Given
        List<CampaignAnalytics> insufficientData = testAnalyticsData.subList(0, 2); // Only 2 data points
        
        when(analyticsRepository.findByCampaignAndDateRange(eq(testCampaign), any(), any()))
                .thenReturn(insufficientData);
        
        // When
        CompletableFuture<PredictiveAnalyticsEngine.TrendAnalysis> future = 
                predictiveEngine.analyzeTrends(testCampaign);
        PredictiveAnalyticsEngine.TrendAnalysis result = future.get();
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertTrue(result.getMessage().contains("Insufficient data for trend analysis"));
        verify(bedrockService, never()).generateText(anyString(), anyString());
    }
    
    @Test
    void testGenerateSeasonalForecast_Success() throws Exception {
        // Given
        when(analyticsRepository.findByCampaignAndDateRange(eq(testCampaign), any(), any()))
                .thenReturn(testAnalyticsData);
        when(bedrockService.generateText(anyString(), anyString()))
                .thenReturn("Seasonal forecast: Expect higher performance in Q4 due to holiday shopping patterns");
        
        // When
        CompletableFuture<PredictiveAnalyticsEngine.SeasonalForecast> future = 
                predictiveEngine.generateSeasonalForecast(testCampaign);
        PredictiveAnalyticsEngine.SeasonalForecast result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getSeasonalPatterns());
        assertNotNull(result.getAiForecast());
        verify(bedrockService).generateText(anyString(), eq("claude-3-sonnet-20240229-v1:0"));
    }
    
    @Test
    void testGenerateSeasonalForecast_InsufficientData() throws Exception {
        // Given
        List<CampaignAnalytics> insufficientData = testAnalyticsData.subList(0, 1); // Only 1 data point
        
        when(analyticsRepository.findByCampaignAndDateRange(eq(testCampaign), any(), any()))
                .thenReturn(insufficientData);
        
        // When
        CompletableFuture<PredictiveAnalyticsEngine.SeasonalForecast> future = 
                predictiveEngine.generateSeasonalForecast(testCampaign);
        PredictiveAnalyticsEngine.SeasonalForecast result = future.get();
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertTrue(result.getMessage().contains("Insufficient data for seasonal analysis"));
        verify(bedrockService, never()).generateText(anyString(), anyString());
    }
    
    @Test
    void testPredictionResult_DefaultValues() {
        // Given
        PredictiveAnalyticsEngine.PredictionResult result = new PredictiveAnalyticsEngine.PredictionResult();
        
        // When
        result.setPredictionPeriod(7);
        result.setRawPrediction("Test prediction");
        result.setPredictedImpressions(1500.0);
        result.setPredictedClicks(75.0);
        result.setPredictedConversions(10.0);
        result.setPredictedCost(150.0);
        result.setPredictedRevenue(1000.0);
        result.setConfidenceLevel(85.0);
        result.setSuccessful(true);
        
        // Then
        assertEquals(7, result.getPredictionPeriod());
        assertEquals("Test prediction", result.getRawPrediction());
        assertEquals(1500.0, result.getPredictedImpressions());
        assertEquals(75.0, result.getPredictedClicks());
        assertEquals(10.0, result.getPredictedConversions());
        assertEquals(150.0, result.getPredictedCost());
        assertEquals(1000.0, result.getPredictedRevenue());
        assertEquals(85.0, result.getConfidenceLevel());
        assertTrue(result.isSuccessful());
    }
    
    @Test
    void testTrendAnalysis_DefaultValues() {
        // Given
        PredictiveAnalyticsEngine.TrendAnalysis analysis = new PredictiveAnalyticsEngine.TrendAnalysis();
        Map<String, Double> trendMetrics = Map.of("growth", 5.2, "volatility", 2.1);
        
        // When
        analysis.setTrendMetrics(trendMetrics);
        analysis.setAiInsights("Positive trend observed");
        analysis.setSuccessful(true);
        
        // Then
        assertEquals(trendMetrics, analysis.getTrendMetrics());
        assertEquals("Positive trend observed", analysis.getAiInsights());
        assertTrue(analysis.isSuccessful());
    }
    
    @Test
    void testSeasonalForecast_DefaultValues() {
        // Given
        PredictiveAnalyticsEngine.SeasonalForecast forecast = new PredictiveAnalyticsEngine.SeasonalForecast();
        Map<String, Object> patterns = Map.of("Q1", 80.0, "Q2", 85.0, "Q3", 75.0, "Q4", 95.0);
        
        // When
        forecast.setSeasonalPatterns(patterns);
        forecast.setAiForecast("Strong Q4 performance expected");
        forecast.setSuccessful(true);
        
        // Then
        assertEquals(patterns, forecast.getSeasonalPatterns());
        assertEquals("Strong Q4 performance expected", forecast.getAiForecast());
        assertTrue(forecast.isSuccessful());
    }
}