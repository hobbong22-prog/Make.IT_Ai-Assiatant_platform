package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.domain.CampaignMetrics;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.CampaignAnalyticsRepository;
import com.Human.Ai.D.makit.repository.CampaignRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignAnalyticsServiceTest {
    
    @Mock
    private CampaignAnalyticsRepository analyticsRepository;
    
    @Mock
    private CampaignRepository campaignRepository;
    
    @Mock
    private BedrockService bedrockService;
    
    @InjectMocks
    private CampaignAnalyticsService analyticsService;
    
    private Campaign testCampaign;
    private User testUser;
    private CampaignMetrics testMetrics;
    private CampaignAnalytics testAnalytics;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        testCampaign = new Campaign();
        testCampaign.setId(1L);
        testCampaign.setName("Test Campaign");
        testCampaign.setUser(testUser);
        
        testMetrics = new CampaignMetrics(testCampaign);
        testMetrics.setImpressions(1000);
        testMetrics.setClicks(50);
        testMetrics.setConversions(5);
        testMetrics.setSpend(100.0);
        testMetrics.setRevenue(500.0);
        testMetrics.setRecordedAt(LocalDateTime.now());
        
        testCampaign.setMetrics(Arrays.asList(testMetrics));
        
        testAnalytics = new CampaignAnalytics(testCampaign, LocalDate.now());
        testAnalytics.setId(1L);
    }
    
    @Test
    void testGenerateAnalyticsReport_NewReport() {
        // Given
        LocalDate reportDate = LocalDate.now();
        when(campaignRepository.findByIdWithMetrics(1L)).thenReturn(Optional.of(testCampaign));
        when(analyticsRepository.findByCampaignAndReportDate(testCampaign, reportDate))
                .thenReturn(Optional.empty());
        when(analyticsRepository.save(any(CampaignAnalytics.class))).thenReturn(testAnalytics);
        when(bedrockService.generateText(anyString(), anyString())).thenReturn("AI generated insights");
        
        // When
        CampaignAnalytics result = analyticsService.generateAnalyticsReport(1L, reportDate);
        
        // Then
        assertNotNull(result);
        assertEquals(testCampaign, result.getCampaign());
        assertEquals(reportDate, result.getReportDate());
        verify(analyticsRepository).save(any(CampaignAnalytics.class));
    }
    
    @Test
    void testGenerateAnalyticsReport_ExistingReport() {
        // Given
        LocalDate reportDate = LocalDate.now();
        when(campaignRepository.findByIdWithMetrics(1L)).thenReturn(Optional.of(testCampaign));
        when(analyticsRepository.findByCampaignAndReportDate(testCampaign, reportDate))
                .thenReturn(Optional.of(testAnalytics));
        
        // When
        CampaignAnalytics result = analyticsService.generateAnalyticsReport(1L, reportDate);
        
        // Then
        assertNotNull(result);
        assertEquals(testAnalytics, result);
        verify(analyticsRepository, never()).save(any(CampaignAnalytics.class));
    }
    
    @Test
    void testGenerateAnalyticsReport_CampaignNotFound() {
        // Given
        LocalDate reportDate = LocalDate.now();
        when(campaignRepository.findByIdWithMetrics(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            analyticsService.generateAnalyticsReport(1L, reportDate);
        });
    }
    
    @Test
    void testGetAnalyticsByDateRange() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        List<CampaignAnalytics> expectedAnalytics = Arrays.asList(testAnalytics);
        
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
        when(analyticsRepository.findByCampaignAndDateRange(testCampaign, startDate, endDate))
                .thenReturn(expectedAnalytics);
        
        // When
        List<CampaignAnalytics> result = analyticsService.getAnalyticsByDateRange(1L, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAnalytics, result.get(0));
    }
    
    @Test
    void testGetLatestAnalytics() {
        // Given
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
        when(analyticsRepository.findLatestByCampaign(testCampaign))
                .thenReturn(Optional.of(testAnalytics));
        
        // When
        Optional<CampaignAnalytics> result = analyticsService.getLatestAnalytics(1L);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testAnalytics, result.get());
    }
    
    @Test
    void testGetUserAnalyticsSummary() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        List<CampaignAnalytics> expectedAnalytics = Arrays.asList(testAnalytics);
        
        when(analyticsRepository.findByUserAndDateRange(1L, startDate, endDate))
                .thenReturn(expectedAnalytics);
        
        // When
        List<CampaignAnalytics> result = analyticsService.getUserAnalyticsSummary(1L, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAnalytics, result.get(0));
    }
    
    @Test
    void testGetTopPerformingCampaigns() {
        // Given
        Double minScore = 70.0;
        List<CampaignAnalytics> expectedAnalytics = Arrays.asList(testAnalytics);
        
        when(analyticsRepository.findTopPerformingCampaigns(1L, minScore))
                .thenReturn(expectedAnalytics);
        
        // When
        List<CampaignAnalytics> result = analyticsService.getTopPerformingCampaigns(1L, minScore);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAnalytics, result.get(0));
    }
    
    @Test
    void testCalculatePerformanceScore() {
        // Given
        CampaignAnalytics analytics = new CampaignAnalytics();
        analytics.setClickThroughRate(2.5); // Should contribute 25 points
        analytics.setConversionRate(3.0);   // Should contribute 45 points (capped at 30)
        analytics.setReturnOnAdSpend(4.0);  // Should contribute 40 points (capped at 40)
        
        // When
        analyticsService.calculatePerformanceScore(analytics);
        
        // Then
        assertNotNull(analytics.getPerformanceScore());
        assertTrue(analytics.getPerformanceScore() > 0);
        assertTrue(analytics.getPerformanceScore() <= 100);
    }
    
    @Test
    void testCalculatePerformanceScore_NoMetrics() {
        // Given
        CampaignAnalytics analytics = new CampaignAnalytics();
        
        // When
        analyticsService.calculatePerformanceScore(analytics);
        
        // Then
        assertNotNull(analytics.getPerformanceScore());
        assertEquals(0.0, analytics.getPerformanceScore());
    }
}