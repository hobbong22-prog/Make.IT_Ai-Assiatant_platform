package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.AudienceSegment;
import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.AudienceSegmentRepository;
import com.Human.Ai.D.makit.repository.CampaignAnalyticsRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class AudienceSegmentationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AudienceSegmentationService.class);
    
    @Autowired
    private AudienceSegmentRepository segmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CampaignAnalyticsRepository analyticsRepository;
    
    @Autowired
    private BedrockService bedrockService;
    
    /**
     * Create audience segment using AI analysis
     */
    @Async
    public CompletableFuture<AudienceSegment> createAIGeneratedSegment(Long userId, 
                                                                       String segmentName, 
                                                                       Map<String, String> criteria) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                
                // Create new segment
                AudienceSegment segment = new AudienceSegment(segmentName, user, 
                                                            AudienceSegment.SegmentType.AI_GENERATED);
                segment.setSegmentationCriteria(criteria);
                segment.setDescription("AI-generated audience segment based on behavioral analysis");
                
                // Analyze segment using AI
                analyzeSegmentWithAI(segment);
                
                // Calculate segment characteristics
                calculateSegmentCharacteristics(segment);
                
                // Save segment
                AudienceSegment savedSegment = segmentRepository.save(segment);
                
                logger.info("Created AI-generated segment '{}' for user {}", segmentName, userId);
                return savedSegment;
                
            } catch (Exception e) {
                logger.error("Failed to create AI segment for user {}: {}", userId, e.getMessage());
                throw new RuntimeException("Segment creation failed", e);
            }
        });
    }
    
    /**
     * Create custom audience segment
     */
    public AudienceSegment createCustomSegment(Long userId, String segmentName, 
                                              Map<String, String> criteria,
                                              AudienceSegment.SegmentType segmentType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        AudienceSegment segment = new AudienceSegment(segmentName, user, segmentType);
        segment.setSegmentationCriteria(criteria);
        segment.setStatus(AudienceSegment.SegmentStatus.ACTIVE);
        
        // Calculate basic characteristics
        calculateSegmentCharacteristics(segment);
        
        return segmentRepository.save(segment);
    }
    
    /**
     * Analyze existing segments for performance
     */
    @Async
    public CompletableFuture<Void> analyzeSegmentPerformance(Long segmentId) {
        return CompletableFuture.runAsync(() -> {
            try {
                AudienceSegment segment = segmentRepository.findById(segmentId)
                        .orElseThrow(() -> new RuntimeException("Segment not found: " + segmentId));
                
                // Get user's campaign analytics for performance analysis
                List<CampaignAnalytics> userAnalytics = getUserAnalyticsData(segment.getUser());
                
                if (!userAnalytics.isEmpty()) {
                    // Calculate performance metrics
                    calculatePerformanceMetrics(segment, userAnalytics);
                    
                    // Generate AI insights
                    generateSegmentInsights(segment, userAnalytics);
                    
                    segment.setLastAnalyzedAt(LocalDateTime.now());
                    segmentRepository.save(segment);
                }
                
            } catch (Exception e) {
                logger.error("Failed to analyze segment performance {}: {}", segmentId, e.getMessage());
            }
        });
    }
    
    /**
     * Get segments for a user
     */
    public List<AudienceSegment> getUserSegments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        return segmentRepository.findByUser(user);
    }
    
    /**
     * Get active segments for a user
     */
    public List<AudienceSegment> getActiveSegments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        return segmentRepository.findActiveByUserOrderByPerformance(user);
    }
    
    /**
     * Get high-performing segments
     */
    public List<AudienceSegment> getHighPerformingSegments(Long userId, Double minScore) {
        return segmentRepository.findHighPerformingSegments(userId, minScore);
    }
    
    /**
     * Get segment statistics
     */
    public Map<String, Object> getSegmentStatistics(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalSegments", segmentRepository.countByUserAndStatus(
                user, AudienceSegment.SegmentStatus.ACTIVE));
        stats.put("averagePerformanceScore", segmentRepository.getAveragePerformanceScore(user));
        stats.put("totalAudienceSize", segmentRepository.getTotalActiveAudienceSize(user));
        
        // Segment type distribution
        List<Object[]> typeDistribution = segmentRepository.getSegmentTypeDistribution(user);
        Map<String, Long> typeBreakdown = new HashMap<>();
        for (Object[] dist : typeDistribution) {
            typeBreakdown.put(dist[0].toString(), (Long) dist[1]);
        }
        stats.put("segmentTypeDistribution", typeBreakdown);
        
        return stats;
    }
    
    /**
     * Update segment status
     */
    public AudienceSegment updateSegmentStatus(Long segmentId, AudienceSegment.SegmentStatus status) {
        AudienceSegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Segment not found: " + segmentId));
        
        segment.setStatus(status);
        return segmentRepository.save(segment);
    }
    
    /**
     * Delete segment
     */
    public void deleteSegment(Long segmentId) {
        segmentRepository.deleteById(segmentId);
    }
    
    /**
     * Generate lookalike segments based on high-performing segments
     */
    @Async
    public CompletableFuture<List<AudienceSegment>> generateLookalikeSegments(Long userId, 
                                                                             Long sourceSegmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                
                AudienceSegment sourceSegment = segmentRepository.findById(sourceSegmentId)
                        .orElseThrow(() -> new RuntimeException("Source segment not found: " + sourceSegmentId));
                
                List<AudienceSegment> lookalikeSegments = new ArrayList<>();
                
                // Generate multiple lookalike segments with different similarity thresholds
                for (int i = 1; i <= 3; i++) {
                    String segmentName = "Lookalike " + i + " - " + sourceSegment.getName();
                    AudienceSegment lookalike = createLookalikeSegment(user, sourceSegment, segmentName, i);
                    lookalikeSegments.add(lookalike);
                }
                
                return segmentRepository.saveAll(lookalikeSegments);
                
            } catch (Exception e) {
                logger.error("Failed to generate lookalike segments: {}", e.getMessage());
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Analyze segment with AI
     */
    private void analyzeSegmentWithAI(AudienceSegment segment) {
        try {
            String prompt = buildSegmentAnalysisPrompt(segment);
            String aiAnalysis = bedrockService.generateText(prompt, "cohere.embed-english-v3");
            
            // Parse AI response and extract insights
            segment.setAiInsights(aiAnalysis);
            
            // Generate recommended actions
            String actionPrompt = buildActionRecommendationPrompt(segment);
            String recommendedActions = bedrockService.generateText(actionPrompt, "claude-3-haiku-20240307-v1:0");
            segment.setRecommendedActions(recommendedActions);
            
        } catch (Exception e) {
            logger.error("Failed to analyze segment with AI: {}", e.getMessage());
            segment.setAiInsights("AI analysis unavailable");
        }
    }
    
    /**
     * Calculate segment characteristics
     */
    private void calculateSegmentCharacteristics(AudienceSegment segment) {
        Map<String, String> characteristics = new HashMap<>();
        Map<String, String> criteria = segment.getSegmentationCriteria();
        
        if (criteria != null) {
            // Estimate segment size based on criteria
            Long estimatedSize = estimateSegmentSize(criteria);
            segment.setSizeEstimate(estimatedSize);
            
            // Calculate confidence score based on criteria specificity
            Double confidenceScore = calculateConfidenceScore(criteria);
            segment.setConfidenceScore(confidenceScore);
            
            // Extract key characteristics
            characteristics.put("primaryCriteria", extractPrimaryCriteria(criteria));
            characteristics.put("targetingScope", calculateTargetingScope(criteria));
            characteristics.put("expectedEngagement", estimateEngagementLevel(criteria));
        }
        
        segment.setCharacteristics(characteristics);
    }
    
    /**
     * Calculate performance metrics for segment
     */
    private void calculatePerformanceMetrics(AudienceSegment segment, List<CampaignAnalytics> analytics) {
        if (analytics.isEmpty()) {
            return;
        }
        
        // Calculate average metrics (simplified - in real implementation would filter by segment)
        double avgEngagement = analytics.stream()
                .filter(a -> a.getClickThroughRate() != null)
                .mapToDouble(CampaignAnalytics::getClickThroughRate)
                .average().orElse(0.0);
        
        double avgConversion = analytics.stream()
                .filter(a -> a.getConversionRate() != null)
                .mapToDouble(CampaignAnalytics::getConversionRate)
                .average().orElse(0.0);
        
        double avgRevenue = analytics.stream()
                .filter(a -> a.getRevenue() != null)
                .mapToDouble(CampaignAnalytics::getRevenue)
                .average().orElse(0.0);
        
        segment.setEngagementRate(avgEngagement);
        segment.setConversionRate(avgConversion);
        segment.setAverageOrderValue(avgRevenue / Math.max(1, analytics.size()));
        
        // Calculate performance score
        double performanceScore = calculatePerformanceScore(avgEngagement, avgConversion, avgRevenue);
        segment.setPerformanceScore(performanceScore);
    }
    
    /**
     * Generate AI insights for segment
     */
    private void generateSegmentInsights(AudienceSegment segment, List<CampaignAnalytics> analytics) {
        try {
            String prompt = buildPerformanceInsightPrompt(segment, analytics);
            String insights = bedrockService.generateText(prompt, "claude-3-haiku-20240307-v1:0");
            segment.setAiInsights(insights);
            
        } catch (Exception e) {
            logger.error("Failed to generate segment insights: {}", e.getMessage());
        }
    }
    
    /**
     * Get user analytics data
     */
    private List<CampaignAnalytics> getUserAnalyticsData(User user) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return analyticsRepository.findByUserAndDateRange(user.getId(), startDate, endDate);
    }
    
    /**
     * Create lookalike segment
     */
    private AudienceSegment createLookalikeSegment(User user, AudienceSegment sourceSegment, 
                                                  String segmentName, int similarity) {
        AudienceSegment lookalike = new AudienceSegment(segmentName, user, 
                                                       AudienceSegment.SegmentType.AI_GENERATED);
        
        // Copy and modify criteria for lookalike
        Map<String, String> lookalikeriteria = new HashMap<>(sourceSegment.getSegmentationCriteria());
        lookalikeriteria.put("similarity_threshold", String.valueOf(similarity * 10));
        lookalikeriteria.put("source_segment", sourceSegment.getId().toString());
        
        lookalike.setSegmentationCriteria(lookalikeriteria);
        lookalike.setDescription("Lookalike segment based on " + sourceSegment.getName());
        
        // Estimate characteristics
        lookalike.setSizeEstimate(sourceSegment.getSizeEstimate() * (4 - similarity)); // Larger for lower similarity
        lookalike.setConfidenceScore(sourceSegment.getConfidenceScore() * (0.8 + similarity * 0.1));
        
        return lookalike;
    }
    
    /**
     * Build segment analysis prompt
     */
    private String buildSegmentAnalysisPrompt(AudienceSegment segment) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this audience segment and provide insights:\n\n");
        prompt.append("Segment Name: ").append(segment.getName()).append("\n");
        prompt.append("Segment Type: ").append(segment.getSegmentType()).append("\n");
        prompt.append("Criteria: ").append(segment.getSegmentationCriteria()).append("\n\n");
        
        prompt.append("Provide analysis on:\n");
        prompt.append("1. Segment characteristics and behavior patterns\n");
        prompt.append("2. Potential marketing opportunities\n");
        prompt.append("3. Recommended messaging strategies\n");
        prompt.append("4. Expected performance indicators\n");
        
        return prompt.toString();
    }
    
    /**
     * Build action recommendation prompt
     */
    private String buildActionRecommendationPrompt(AudienceSegment segment) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on this audience segment, recommend specific marketing actions:\n\n");
        prompt.append("Segment: ").append(segment.getName()).append("\n");
        prompt.append("Type: ").append(segment.getSegmentType()).append("\n");
        prompt.append("Estimated Size: ").append(segment.getSizeEstimate()).append("\n\n");
        
        prompt.append("Provide actionable recommendations for:\n");
        prompt.append("1. Content strategy and messaging\n");
        prompt.append("2. Channel selection and timing\n");
        prompt.append("3. Budget allocation suggestions\n");
        prompt.append("4. Campaign optimization tactics\n");
        
        return prompt.toString();
    }
    
    /**
     * Build performance insight prompt
     */
    private String buildPerformanceInsightPrompt(AudienceSegment segment, List<CampaignAnalytics> analytics) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the performance of this audience segment:\n\n");
        prompt.append("Segment: ").append(segment.getName()).append("\n");
        prompt.append("Performance Score: ").append(segment.getPerformanceScore()).append("\n");
        prompt.append("Engagement Rate: ").append(segment.getEngagementRate()).append("%\n");
        prompt.append("Conversion Rate: ").append(segment.getConversionRate()).append("%\n");
        prompt.append("Data Points: ").append(analytics.size()).append("\n\n");
        
        prompt.append("Provide insights on:\n");
        prompt.append("1. Performance strengths and weaknesses\n");
        prompt.append("2. Optimization opportunities\n");
        prompt.append("3. Segment evolution recommendations\n");
        prompt.append("4. ROI improvement strategies\n");
        
        return prompt.toString();
    }
    
    /**
     * Estimate segment size based on criteria
     */
    private Long estimateSegmentSize(Map<String, String> criteria) {
        // Simplified estimation logic
        long baseSize = 10000L;
        
        for (Map.Entry<String, String> criterion : criteria.entrySet()) {
            String key = criterion.getKey().toLowerCase();
            if (key.contains("age") || key.contains("demographic")) {
                baseSize *= 0.7; // Demographic targeting reduces size
            } else if (key.contains("interest") || key.contains("behavior")) {
                baseSize *= 0.5; // Behavioral targeting is more specific
            } else if (key.contains("location") || key.contains("geographic")) {
                baseSize *= 0.8; // Geographic targeting
            }
        }
        
        return Math.max(1000L, baseSize);
    }
    
    /**
     * Calculate confidence score based on criteria specificity
     */
    private Double calculateConfidenceScore(Map<String, String> criteria) {
        if (criteria.isEmpty()) {
            return 50.0;
        }
        
        double score = 60.0; // Base score
        
        // More criteria generally means higher confidence
        score += Math.min(criteria.size() * 5, 25);
        
        // Specific criteria types increase confidence
        for (String key : criteria.keySet()) {
            if (key.toLowerCase().contains("behavior") || key.toLowerCase().contains("purchase")) {
                score += 10;
            }
        }
        
        return Math.min(95.0, score);
    }
    
    /**
     * Extract primary criteria
     */
    private String extractPrimaryCriteria(Map<String, String> criteria) {
        if (criteria.isEmpty()) {
            return "No specific criteria";
        }
        
        return criteria.entrySet().stream()
                .limit(3)
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
    }
    
    /**
     * Calculate targeting scope
     */
    private String calculateTargetingScope(Map<String, String> criteria) {
        int criteriaCount = criteria.size();
        
        if (criteriaCount <= 2) {
            return "Broad";
        } else if (criteriaCount <= 4) {
            return "Moderate";
        } else {
            return "Narrow";
        }
    }
    
    /**
     * Estimate engagement level
     */
    private String estimateEngagementLevel(Map<String, String> criteria) {
        // Simplified logic based on criteria types
        boolean hasBehavioral = criteria.keySet().stream()
                .anyMatch(key -> key.toLowerCase().contains("behavior") || 
                               key.toLowerCase().contains("interest"));
        
        return hasBehavioral ? "High" : "Medium";
    }
    
    /**
     * Calculate performance score
     */
    private double calculatePerformanceScore(double engagement, double conversion, double revenue) {
        double score = 0.0;
        
        // Engagement contribution (0-40 points)
        score += Math.min(engagement * 2, 40);
        
        // Conversion contribution (0-35 points)
        score += Math.min(conversion * 3, 35);
        
        // Revenue contribution (0-25 points)
        score += Math.min(revenue / 100, 25);
        
        return Math.min(100.0, score);
    }
}