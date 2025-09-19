package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.AudienceSegment;
import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.AudienceSegmentRepository;
import com.Human.Ai.D.makit.repository.CampaignAnalyticsRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudienceSegmentationServiceTest {
    
    @Mock
    private AudienceSegmentRepository segmentRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private CampaignAnalyticsRepository analyticsRepository;
    
    @Mock
    private BedrockService bedrockService;
    
    @InjectMocks
    private AudienceSegmentationService segmentationService;
    
    private User testUser;
    private AudienceSegment testSegment;
    private Map<String, String> testCriteria;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        testCriteria = new HashMap<>();
        testCriteria.put("age", "25-35");
        testCriteria.put("interest", "technology");
        testCriteria.put("location", "urban");
        
        testSegment = new AudienceSegment("Test Segment", testUser, AudienceSegment.SegmentType.AI_GENERATED);
        testSegment.setId(1L);
        testSegment.setSegmentationCriteria(testCriteria);
        testSegment.setSizeEstimate(5000L);
        testSegment.setConfidenceScore(85.0);
        testSegment.setPerformanceScore(75.0);
    }
    
    @Test
    void testCreateAIGeneratedSegment_Success() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(segmentRepository.save(any(AudienceSegment.class))).thenReturn(testSegment);
        when(bedrockService.generateText(anyString(), anyString()))
                .thenReturn("AI-generated insights for the segment");
        
        // When
        CompletableFuture<AudienceSegment> future = segmentationService
                .createAIGeneratedSegment(1L, "Test Segment", testCriteria);
        AudienceSegment result = future.get();
        
        // Then
        assertNotNull(result);
        assertEquals("Test Segment", result.getName());
        assertEquals(AudienceSegment.SegmentType.AI_GENERATED, result.getSegmentType());
        assertEquals(testCriteria, result.getSegmentationCriteria());
        verify(segmentRepository).save(any(AudienceSegment.class));
    }
    
    @Test
    void testCreateAIGeneratedSegment_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        CompletableFuture<AudienceSegment> future = segmentationService
                .createAIGeneratedSegment(1L, "Test Segment", testCriteria);
        
        assertThrows(Exception.class, () -> future.get());
        verify(segmentRepository, never()).save(any(AudienceSegment.class));
    }
    
    @Test
    void testCreateCustomSegment_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(segmentRepository.save(any(AudienceSegment.class))).thenReturn(testSegment);
        
        // When
        AudienceSegment result = segmentationService.createCustomSegment(
                1L, "Custom Segment", testCriteria, AudienceSegment.SegmentType.DEMOGRAPHIC);
        
        // Then
        assertNotNull(result);
        assertEquals(AudienceSegment.SegmentStatus.ACTIVE, result.getStatus());
        verify(segmentRepository).save(any(AudienceSegment.class));
    }
    
    @Test
    void testGetUserSegments() {
        // Given
        List<AudienceSegment> expectedSegments = Arrays.asList(testSegment);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(segmentRepository.findByUser(testUser)).thenReturn(expectedSegments);
        
        // When
        List<AudienceSegment> result = segmentationService.getUserSegments(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSegment, result.get(0));
    }
    
    @Test
    void testGetActiveSegments() {
        // Given
        List<AudienceSegment> expectedSegments = Arrays.asList(testSegment);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(segmentRepository.findActiveByUserOrderByPerformance(testUser)).thenReturn(expectedSegments);
        
        // When
        List<AudienceSegment> result = segmentationService.getActiveSegments(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSegment, result.get(0));
    }
    
    @Test
    void testGetHighPerformingSegments() {
        // Given
        Double minScore = 70.0;
        List<AudienceSegment> expectedSegments = Arrays.asList(testSegment);
        when(segmentRepository.findHighPerformingSegments(1L, minScore)).thenReturn(expectedSegments);
        
        // When
        List<AudienceSegment> result = segmentationService.getHighPerformingSegments(1L, minScore);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSegment, result.get(0));
    }
    
    @Test
    void testAnalyzeSegmentPerformance_Success() throws Exception {
        // Given
        List<CampaignAnalytics> analyticsData = createTestAnalyticsData();
        when(segmentRepository.findById(1L)).thenReturn(Optional.of(testSegment));
        when(analyticsRepository.findByUserAndDateRange(eq(1L), any(), any())).thenReturn(analyticsData);
        when(bedrockService.generateText(anyString(), anyString()))
                .thenReturn("Performance insights for the segment");
        when(segmentRepository.save(any(AudienceSegment.class))).thenReturn(testSegment);
        
        // When
        CompletableFuture<Void> future = segmentationService.analyzeSegmentPerformance(1L);
        future.get(); // Wait for completion
        
        // Then
        verify(segmentRepository).save(any(AudienceSegment.class));
    }
    
    @Test
    void testUpdateSegmentStatus() {
        // Given
        when(segmentRepository.findById(1L)).thenReturn(Optional.of(testSegment));
        when(segmentRepository.save(testSegment)).thenReturn(testSegment);
        
        // When
        AudienceSegment result = segmentationService.updateSegmentStatus(
                1L, AudienceSegment.SegmentStatus.INACTIVE);
        
        // Then
        assertNotNull(result);
        assertEquals(AudienceSegment.SegmentStatus.INACTIVE, result.getStatus());
        verify(segmentRepository).save(testSegment);
    }
    
    @Test
    void testGetSegmentStatistics() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(segmentRepository.countByUserAndStatus(testUser, AudienceSegment.SegmentStatus.ACTIVE))
                .thenReturn(5L);
        when(segmentRepository.getAveragePerformanceScore(testUser)).thenReturn(78.5);
        when(segmentRepository.getTotalActiveAudienceSize(testUser)).thenReturn(25000L);
        when(segmentRepository.getSegmentTypeDistribution(testUser))
                .thenReturn(Arrays.asList(new Object[]{"AI_GENERATED", 3L}, new Object[]{"DEMOGRAPHIC", 2L}));
        
        // When
        Map<String, Object> result = segmentationService.getSegmentStatistics(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(5L, result.get("totalSegments"));
        assertEquals(78.5, result.get("averagePerformanceScore"));
        assertEquals(25000L, result.get("totalAudienceSize"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> typeDistribution = (Map<String, Long>) result.get("segmentTypeDistribution");
        assertNotNull(typeDistribution);
        assertEquals(3L, typeDistribution.get("AI_GENERATED"));
        assertEquals(2L, typeDistribution.get("DEMOGRAPHIC"));
    }
    
    @Test
    void testGenerateLookalikeSegments_Success() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(segmentRepository.findById(1L)).thenReturn(Optional.of(testSegment));
        when(segmentRepository.saveAll(anyList())).thenReturn(Arrays.asList(testSegment, testSegment, testSegment));
        
        // When
        CompletableFuture<List<AudienceSegment>> future = segmentationService
                .generateLookalikeSegments(1L, 1L);
        List<AudienceSegment> result = future.get();
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // Should generate 3 lookalike segments
        verify(segmentRepository).saveAll(anyList());
    }
    
    @Test
    void testDeleteSegment() {
        // When
        segmentationService.deleteSegment(1L);
        
        // Then
        verify(segmentRepository).deleteById(1L);
    }
    
    @Test
    void testSegmentBusinessMethods() {
        // Test activate
        testSegment.activate();
        assertEquals(AudienceSegment.SegmentStatus.ACTIVE, testSegment.getStatus());
        assertTrue(testSegment.isActive());
        
        // Test deactivate
        testSegment.deactivate();
        assertEquals(AudienceSegment.SegmentStatus.INACTIVE, testSegment.getStatus());
        assertFalse(testSegment.isActive());
        
        // Test archive
        testSegment.archive();
        assertEquals(AudienceSegment.SegmentStatus.ARCHIVED, testSegment.getStatus());
        
        // Test segment value calculation
        testSegment.setSizeEstimate(5000L);
        testSegment.setPerformanceScore(80.0);
        testSegment.setEngagementRate(75.0);
        
        double segmentValue = testSegment.calculateSegmentValue();
        assertTrue(segmentValue > 0);
        assertTrue(segmentValue <= 100);
    }
    
    private List<CampaignAnalytics> createTestAnalyticsData() {
        List<CampaignAnalytics> analytics = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            CampaignAnalytics data = new CampaignAnalytics();
            data.setReportDate(LocalDate.now().minusDays(i));
            data.setImpressions(1000.0 * i);
            data.setClicks(50.0 * i);
            data.setConversions(5.0 * i);
            data.setRevenue(500.0 * i);
            data.calculateMetrics();
            analytics.add(data);
        }
        
        return analytics;
    }
}