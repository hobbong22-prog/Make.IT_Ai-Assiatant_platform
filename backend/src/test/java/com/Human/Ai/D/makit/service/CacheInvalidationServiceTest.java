package com.Human.Ai.D.makit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheInvalidationServiceTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private CacheInvalidationService cacheInvalidationService;

    @Test
    void testInvalidateCampaignAnalytics() {
        // Given
        Long campaignId = 123L;

        // When
        cacheInvalidationService.invalidateCampaignAnalytics(campaignId);

        // Then
        verify(cacheService).invalidateByPattern("campaignAnalytics::" + campaignId + "*");
        verify(cacheService).invalidateByPattern("campaignAnalytics::latest_" + campaignId);
    }

    @Test
    void testInvalidateUserCaches() {
        // Given
        Long userId = 456L;

        // When
        cacheInvalidationService.invalidateUserCaches(userId);

        // Then
        verify(cacheService).invalidateByPattern("userData::" + userId + "*");
        verify(cacheService).invalidateByPattern("audienceSegments::user_" + userId + "*");
    }

    @Test
    void testInvalidateKnowledgeDocument() {
        // Given
        String documentId = "doc123";

        // When
        cacheInvalidationService.invalidateKnowledgeDocument(documentId);

        // Then
        verify(cacheService).invalidateByPattern("knowledgeBase::" + documentId + "*");
    }

    @Test
    void testInvalidateAudienceSegment() {
        // Given
        Long segmentId = 789L;

        // When
        cacheInvalidationService.invalidateAudienceSegment(segmentId);

        // Then
        verify(cacheService).invalidateByPattern("audienceSegments::" + segmentId + "*");
    }

    @Test
    void testEmergencyCacheClear() {
        // When
        cacheInvalidationService.emergencyCacheClear();

        // Then
        verify(cacheService).clear("contentGeneration");
        verify(cacheService).clear("campaignAnalytics");
        verify(cacheService).clear("knowledgeBase");
        verify(cacheService).clear("userData");
        verify(cacheService).clear("audienceSegments");
    }

    @Test
    void testPerformScheduledCacheCleanup() {
        // Given
        CacheService.CacheStats mockStats = new CacheService.CacheStats("testCache", 10);
        when(cacheService.getCacheStats(anyString())).thenReturn(mockStats);

        // When
        cacheInvalidationService.performScheduledCacheCleanup();

        // Then
        verify(cacheService, times(5)).getCacheStats(anyString());
        verify(cacheService).invalidateByPattern("contentGeneration::*_old_*");
        verify(cacheService).invalidateByPattern("campaignAnalytics::*_old_*");
    }

    @Test
    void testPerformScheduledCacheCleanupHandlesException() {
        // Given
        when(cacheService.getCacheStats(anyString())).thenThrow(new RuntimeException("Cache error"));

        // When & Then - should not throw exception
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> 
            cacheInvalidationService.performScheduledCacheCleanup());
    }
}