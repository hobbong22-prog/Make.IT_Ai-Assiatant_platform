package com.Human.Ai.D.makit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Service for managing cache invalidation strategies
 */
@Service
public class CacheInvalidationService {

    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationService.class);

    @Autowired
    private CacheService cacheService;

    /**
     * Invalidate all campaign-related caches when campaign data changes
     */
    @CacheEvict(value = {"campaignAnalytics", "audienceSegments"}, allEntries = true)
    public void invalidateCampaignCaches() {
        logger.info("Invalidated all campaign-related caches");
    }

    /**
     * Invalidate specific campaign analytics cache
     */
    @CacheEvict(value = "campaignAnalytics", key = "#campaignId")
    public void invalidateCampaignAnalytics(Long campaignId) {
        // Also invalidate related patterns
        cacheService.invalidateByPattern("campaignAnalytics::" + campaignId + "*");
        cacheService.invalidateByPattern("campaignAnalytics::latest_" + campaignId);
        logger.info("Invalidated campaign analytics cache for campaign: {}", campaignId);
    }

    /**
     * Invalidate user-specific caches
     */
    @CacheEvict(value = {"userData", "audienceSegments"}, key = "#userId")
    public void invalidateUserCaches(Long userId) {
        cacheService.invalidateByPattern("userData::" + userId + "*");
        cacheService.invalidateByPattern("audienceSegments::user_" + userId + "*");
        logger.info("Invalidated user-specific caches for user: {}", userId);
    }

    /**
     * Invalidate content generation caches
     */
    @CacheEvict(value = "contentGeneration", allEntries = true)
    public void invalidateContentGenerationCaches() {
        logger.info("Invalidated all content generation caches");
    }

    /**
     * Invalidate knowledge base caches when documents are updated
     */
    @CacheEvict(value = "knowledgeBase", allEntries = true)
    public void invalidateKnowledgeBaseCaches() {
        logger.info("Invalidated all knowledge base caches");
    }

    /**
     * Invalidate specific knowledge document cache
     */
    @CacheEvict(value = "knowledgeBase", key = "#documentId")
    public void invalidateKnowledgeDocument(String documentId) {
        cacheService.invalidateByPattern("knowledgeBase::" + documentId + "*");
        logger.info("Invalidated knowledge document cache: {}", documentId);
    }

    /**
     * Invalidate audience segment caches
     */
    @CacheEvict(value = "audienceSegments", allEntries = true)
    public void invalidateAudienceSegmentCaches() {
        logger.info("Invalidated all audience segment caches");
    }

    /**
     * Invalidate specific audience segment cache
     */
    @CacheEvict(value = "audienceSegments", key = "#segmentId")
    public void invalidateAudienceSegment(Long segmentId) {
        cacheService.invalidateByPattern("audienceSegments::" + segmentId + "*");
        logger.info("Invalidated audience segment cache: {}", segmentId);
    }

    /**
     * Scheduled cache cleanup - remove expired entries
     */
    public void performScheduledCacheCleanup() {
        try {
            // Get cache statistics
            var contentStats = cacheService.getCacheStats("contentGeneration");
            var analyticsStats = cacheService.getCacheStats("campaignAnalytics");
            var knowledgeStats = cacheService.getCacheStats("knowledgeBase");
            var userStats = cacheService.getCacheStats("userData");
            var segmentStats = cacheService.getCacheStats("audienceSegments");

            logger.info("Cache statistics - Content: {}, Analytics: {}, Knowledge: {}, User: {}, Segments: {}",
                    contentStats.getEntryCount(),
                    analyticsStats.getEntryCount(),
                    knowledgeStats.getEntryCount(),
                    userStats.getEntryCount(),
                    segmentStats.getEntryCount());

            // Perform cleanup based on patterns or age
            cleanupExpiredEntries();

        } catch (Exception e) {
            logger.error("Error during scheduled cache cleanup", e);
        }
    }

    /**
     * Clean up expired cache entries
     */
    private void cleanupExpiredEntries() {
        // Clean up old content generation entries (older than 2 hours)
        cacheService.invalidateByPattern("contentGeneration::*_old_*");
        
        // Clean up old analytics entries (older than 1 hour)
        cacheService.invalidateByPattern("campaignAnalytics::*_old_*");
        
        logger.debug("Cleaned up expired cache entries");
    }

    /**
     * Emergency cache clear - clears all caches
     */
    public void emergencyCacheClear() {
        cacheService.clear("contentGeneration");
        cacheService.clear("campaignAnalytics");
        cacheService.clear("knowledgeBase");
        cacheService.clear("userData");
        cacheService.clear("audienceSegments");
        
        logger.warn("Emergency cache clear performed - all caches cleared");
    }

    /**
     * Warm up critical caches with frequently accessed data
     */
    public void warmUpCaches() {
        logger.info("Starting cache warm-up process");
        
        // This would typically pre-load frequently accessed data
        // Implementation depends on specific business requirements
        
        logger.info("Cache warm-up completed");
    }
}