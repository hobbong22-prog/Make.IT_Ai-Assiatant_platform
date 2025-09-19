package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.domain.OptimizationRecommendation;
import com.Human.Ai.D.makit.repository.CampaignAnalyticsRepository;
import com.Human.Ai.D.makit.repository.CampaignRepository;
import com.Human.Ai.D.makit.repository.OptimizationRecommendationRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OptimizationRecommendationService {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizationRecommendationService.class);
    
    @Autowired
    private OptimizationRecommendationRepository recommendationRepository;
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private CampaignAnalyticsRepository analyticsRepository;
    
    @Autowired
    private BedrockService bedrockService;
    
    /**
     * Generate optimization recommendations for a campaign
     */
    @Async
    public CompletableFuture<List<OptimizationRecommendation>> generateRecommendations(Long campaignId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Campaign campaign = campaignRepository.findByIdWithMetrics(campaignId)
                        .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
                
                // Get recent analytics data
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusDays(30);
                List<CampaignAnalytics> analyticsData = analyticsRepository
                        .findByCampaignAndDateRange(campaign, startDate, endDate);
                
                if (analyticsData.isEmpty()) {
                    logger.warn("No analytics data found for campaign {}", campaignId);
                    return new ArrayList<>();
                }
                
                List<OptimizationRecommendation> recommendations = new ArrayList<>();
                
                // Generate different types of recommendations
                recommendations.addAll(generatePerformanceRecommendations(campaign, analyticsData));
                recommendations.addAll(generateBudgetRecommendations(campaign, analyticsData));
                recommendations.addAll(generateAudienceRecommendations(campaign, analyticsData));
                recommendations.addAll(generateContentRecommendations(campaign, analyticsData));
                recommendations.addAll(generateTimingRecommendations(campaign, analyticsData));
                
                // Score and prioritize recommendations
                recommendations.forEach(this::calculateRecommendationScore);
                
                // Save recommendations
                List<OptimizationRecommendation> savedRecommendations = 
                        recommendationRepository.saveAll(recommendations);
                
                logger.info("Generated {} recommendations for campaign {}", 
                          savedRecommendations.size(), campaignId);
                
                return savedRecommendations;
                
            } catch (Exception e) {
                logger.error("Failed to generate recommendations for campaign {}: {}", 
                           campaignId, e.getMessage());
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Get active recommendations for a campaign
     */
    public List<OptimizationRecommendation> getActiveRecommendations(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
        
        return recommendationRepository.findActiveByCampaign(campaign);
    }
    
    /**
     * Get recommendations by user and status
     */
    public List<OptimizationRecommendation> getRecommendationsByUserAndStatus(Long userId, 
                                                                             OptimizationRecommendation.RecommendationStatus status) {
        return recommendationRepository.findByUserAndStatus(userId, status);
    }
    
    /**
     * Get high priority recommendations for a user
     */
    public List<OptimizationRecommendation> getHighPriorityRecommendations(Long userId) {
        List<OptimizationRecommendation.RecommendationPriority> highPriorities = 
                Arrays.asList(OptimizationRecommendation.RecommendationPriority.HIGH,
                            OptimizationRecommendation.RecommendationPriority.CRITICAL);
        
        return recommendationRepository.findByUserAndPriorities(userId, highPriorities);
    }
    
    /**
     * Implement a recommendation
     */
    public OptimizationRecommendation implementRecommendation(Long recommendationId) {
        OptimizationRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recommendation not found: " + recommendationId));
        
        recommendation.markAsImplemented();
        return recommendationRepository.save(recommendation);
    }
    
    /**
     * Dismiss a recommendation
     */
    public OptimizationRecommendation dismissRecommendation(Long recommendationId) {
        OptimizationRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recommendation not found: " + recommendationId));
        
        recommendation.dismiss();
        return recommendationRepository.save(recommendation);
    }
    
    /**
     * Get recommendation statistics for a user
     */
    public Map<String, Object> getRecommendationStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPending", recommendationRepository.countByUserAndStatus(
                userId, OptimizationRecommendation.RecommendationStatus.PENDING));
        stats.put("totalImplemented", recommendationRepository.countByUserAndStatus(
                userId, OptimizationRecommendation.RecommendationStatus.IMPLEMENTED));
        stats.put("averageImplementedImpact", recommendationRepository.getAverageImplementedImpact(userId));
        
        // Category breakdown
        List<Object[]> categoryStats = recommendationRepository.getRecommendationCategoryStats(userId);
        Map<String, Long> categoryBreakdown = new HashMap<>();
        for (Object[] stat : categoryStats) {
            categoryBreakdown.put(stat[0].toString(), (Long) stat[1]);
        }
        stats.put("categoryBreakdown", categoryBreakdown);
        
        return stats;
    }
    
    /**
     * Clean up expired recommendations
     */
    @Async
    public void cleanupExpiredRecommendations() {
        List<OptimizationRecommendation> expired = recommendationRepository
                .findExpiredRecommendations(LocalDateTime.now());
        
        for (OptimizationRecommendation recommendation : expired) {
            recommendation.setStatus(OptimizationRecommendation.RecommendationStatus.EXPIRED);
        }
        
        recommendationRepository.saveAll(expired);
        logger.info("Marked {} recommendations as expired", expired.size());
    }
    
    /**
     * Generate performance-based recommendations
     */
    private List<OptimizationRecommendation> generatePerformanceRecommendations(Campaign campaign, 
                                                                               List<CampaignAnalytics> analyticsData) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        // Calculate average performance metrics
        double avgCTR = analyticsData.stream()
                .filter(a -> a.getClickThroughRate() != null)
                .mapToDouble(CampaignAnalytics::getClickThroughRate)
                .average().orElse(0.0);
        
        double avgConversionRate = analyticsData.stream()
                .filter(a -> a.getConversionRate() != null)
                .mapToDouble(CampaignAnalytics::getConversionRate)
                .average().orElse(0.0);
        
        double avgROAS = analyticsData.stream()
                .filter(a -> a.getReturnOnAdSpend() != null)
                .mapToDouble(CampaignAnalytics::getReturnOnAdSpend)
                .average().orElse(0.0);
        
        // Low CTR recommendation
        if (avgCTR < 2.0) {
            OptimizationRecommendation recommendation = new OptimizationRecommendation(
                    campaign, "LOW_CTR", "Improve Click-Through Rate");
            recommendation.setDescription("Your campaign's CTR is below industry average. Consider optimizing ad copy and targeting.");
            recommendation.setActionRequired("Review and update ad creative, refine audience targeting, test new headlines");
            recommendation.setCategory(OptimizationRecommendation.RecommendationCategory.CREATIVE_OPTIMIZATION);
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.HIGH);
            recommendation.setExpectedImpact(25.0);
            recommendation.setConfidenceScore(85.0);
            recommendation.setMetricTarget("Click-Through Rate");
            recommendation.setBaselineValue(avgCTR);
            recommendation.setTargetValue(avgCTR * 1.5);
            recommendation.setImplementationEffort("Medium");
            recommendation.setExpiresAt(LocalDateTime.now().plusDays(30));
            recommendations.add(recommendation);
        }
        
        // Low conversion rate recommendation
        if (avgConversionRate < 3.0) {
            OptimizationRecommendation recommendation = new OptimizationRecommendation(
                    campaign, "LOW_CONVERSION", "Optimize Conversion Rate");
            recommendation.setDescription("Conversion rate is below optimal levels. Landing page and user experience improvements needed.");
            recommendation.setActionRequired("Optimize landing pages, improve call-to-action buttons, streamline conversion funnel");
            recommendation.setCategory(OptimizationRecommendation.RecommendationCategory.PERFORMANCE_ENHANCEMENT);
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.HIGH);
            recommendation.setExpectedImpact(30.0);
            recommendation.setConfidenceScore(80.0);
            recommendation.setMetricTarget("Conversion Rate");
            recommendation.setBaselineValue(avgConversionRate);
            recommendation.setTargetValue(avgConversionRate * 1.4);
            recommendation.setImplementationEffort("High");
            recommendation.setExpiresAt(LocalDateTime.now().plusDays(45));
            recommendations.add(recommendation);
        }
        
        // Low ROAS recommendation
        if (avgROAS < 3.0) {
            OptimizationRecommendation recommendation = new OptimizationRecommendation(
                    campaign, "LOW_ROAS", "Improve Return on Ad Spend");
            recommendation.setDescription("ROAS is below target. Budget reallocation and bid optimization recommended.");
            recommendation.setActionRequired("Adjust bidding strategy, reallocate budget to high-performing segments, pause underperforming ads");
            recommendation.setCategory(OptimizationRecommendation.RecommendationCategory.BUDGET_OPTIMIZATION);
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.CRITICAL);
            recommendation.setExpectedImpact(40.0);
            recommendation.setConfidenceScore(90.0);
            recommendation.setMetricTarget("Return on Ad Spend");
            recommendation.setBaselineValue(avgROAS);
            recommendation.setTargetValue(avgROAS * 1.6);
            recommendation.setImplementationEffort("Medium");
            recommendation.setExpiresAt(LocalDateTime.now().plusDays(21));
            recommendations.add(recommendation);
        }
        
        return recommendations;
    }
    
    /**
     * Generate budget optimization recommendations
     */
    private List<OptimizationRecommendation> generateBudgetRecommendations(Campaign campaign, 
                                                                          List<CampaignAnalytics> analyticsData) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        double totalCost = analyticsData.stream()
                .filter(a -> a.getCost() != null)
                .mapToDouble(CampaignAnalytics::getCost)
                .sum();
        
        double totalRevenue = analyticsData.stream()
                .filter(a -> a.getRevenue() != null)
                .mapToDouble(CampaignAnalytics::getRevenue)
                .sum();
        
        // Budget utilization recommendation
        if (campaign.getBudget() != null && totalCost < campaign.getBudget() * 0.7) {
            OptimizationRecommendation recommendation = new OptimizationRecommendation(
                    campaign, "UNDERUTILIZED_BUDGET", "Increase Budget Utilization");
            recommendation.setDescription("Campaign is not fully utilizing allocated budget. Consider increasing bids or expanding targeting.");
            recommendation.setActionRequired("Increase daily budget limits, expand keyword targeting, raise bid amounts for high-performing segments");
            recommendation.setCategory(OptimizationRecommendation.RecommendationCategory.BUDGET_OPTIMIZATION);
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.MEDIUM);
            recommendation.setExpectedImpact(20.0);
            recommendation.setConfidenceScore(75.0);
            recommendation.setImplementationEffort("Low");
            recommendation.setExpiresAt(LocalDateTime.now().plusDays(14));
            recommendations.add(recommendation);
        }
        
        return recommendations;
    }
    
    /**
     * Generate audience targeting recommendations
     */
    private List<OptimizationRecommendation> generateAudienceRecommendations(Campaign campaign, 
                                                                            List<CampaignAnalytics> analyticsData) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        // Generate AI-powered audience insights
        try {
            String prompt = buildAudienceOptimizationPrompt(campaign, analyticsData);
            String aiRecommendation = bedrockService.generateText(prompt, "claude-3-haiku-20240307-v1:0");
            
            OptimizationRecommendation recommendation = new OptimizationRecommendation(
                    campaign, "AUDIENCE_OPTIMIZATION", "Optimize Audience Targeting");
            recommendation.setDescription("AI analysis suggests audience targeting improvements based on performance data.");
            recommendation.setActionRequired(aiRecommendation);
            recommendation.setCategory(OptimizationRecommendation.RecommendationCategory.AUDIENCE_TARGETING);
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.MEDIUM);
            recommendation.setExpectedImpact(22.0);
            recommendation.setConfidenceScore(70.0);
            recommendation.setImplementationEffort("Medium");
            recommendation.setExpiresAt(LocalDateTime.now().plusDays(30));
            recommendations.add(recommendation);
            
        } catch (Exception e) {
            logger.error("Failed to generate AI audience recommendation: {}", e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Generate content optimization recommendations
     */
    private List<OptimizationRecommendation> generateContentRecommendations(Campaign campaign, 
                                                                           List<CampaignAnalytics> analyticsData) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        // Generate AI-powered content insights
        try {
            String prompt = buildContentOptimizationPrompt(campaign, analyticsData);
            String aiRecommendation = bedrockService.generateText(prompt, "claude-3-haiku-20240307-v1:0");
            
            OptimizationRecommendation recommendation = new OptimizationRecommendation(
                    campaign, "CONTENT_OPTIMIZATION", "Improve Content Performance");
            recommendation.setDescription("Content analysis reveals opportunities for creative optimization.");
            recommendation.setActionRequired(aiRecommendation);
            recommendation.setCategory(OptimizationRecommendation.RecommendationCategory.CONTENT_IMPROVEMENT);
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.MEDIUM);
            recommendation.setExpectedImpact(18.0);
            recommendation.setConfidenceScore(65.0);
            recommendation.setImplementationEffort("High");
            recommendation.setExpiresAt(LocalDateTime.now().plusDays(45));
            recommendations.add(recommendation);
            
        } catch (Exception e) {
            logger.error("Failed to generate AI content recommendation: {}", e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Generate timing optimization recommendations
     */
    private List<OptimizationRecommendation> generateTimingRecommendations(Campaign campaign, 
                                                                          List<CampaignAnalytics> analyticsData) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        // Analyze performance by day of week
        Map<Integer, Double> dayPerformance = analyticsData.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getReportDate().getDayOfWeek().getValue(),
                        Collectors.averagingDouble(a -> a.getPerformanceScore() != null ? a.getPerformanceScore() : 0.0)
                ));
        
        if (!dayPerformance.isEmpty()) {
            OptimizationRecommendation recommendation = new OptimizationRecommendation(
                    campaign, "TIMING_OPTIMIZATION", "Optimize Ad Scheduling");
            recommendation.setDescription("Performance varies by day of week. Adjust ad scheduling for optimal results.");
            recommendation.setActionRequired("Increase budget allocation for high-performing days, reduce spend on low-performing days");
            recommendation.setCategory(OptimizationRecommendation.RecommendationCategory.TIMING_OPTIMIZATION);
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.LOW);
            recommendation.setExpectedImpact(15.0);
            recommendation.setConfidenceScore(60.0);
            recommendation.setImplementationEffort("Low");
            recommendation.setExpiresAt(LocalDateTime.now().plusDays(21));
            recommendations.add(recommendation);
        }
        
        return recommendations;
    }
    
    /**
     * Calculate recommendation score and priority
     */
    private void calculateRecommendationScore(OptimizationRecommendation recommendation) {
        double score = recommendation.calculatePriorityScore();
        
        // Adjust priority based on score
        if (score >= 80) {
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.CRITICAL);
        } else if (score >= 60) {
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.HIGH);
        } else if (score >= 40) {
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.MEDIUM);
        } else {
            recommendation.setPriority(OptimizationRecommendation.RecommendationPriority.LOW);
        }
    }
    
    /**
     * Build prompt for audience optimization
     */
    private String buildAudienceOptimizationPrompt(Campaign campaign, List<CampaignAnalytics> analyticsData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this campaign's performance and suggest audience targeting optimizations:\n\n");
        prompt.append("Campaign: ").append(campaign.getName()).append("\n");
        prompt.append("Campaign Type: ").append(campaign.getType()).append("\n");
        prompt.append("Current Target Audience: ").append(campaign.getTargetAudience()).append("\n\n");
        
        prompt.append("Performance Summary:\n");
        double avgPerformance = analyticsData.stream()
                .filter(a -> a.getPerformanceScore() != null)
                .mapToDouble(CampaignAnalytics::getPerformanceScore)
                .average().orElse(0.0);
        prompt.append("Average Performance Score: ").append(avgPerformance).append("\n\n");
        
        prompt.append("Provide specific, actionable recommendations for audience targeting optimization. ");
        prompt.append("Focus on demographics, interests, behaviors, and lookalike audiences. ");
        prompt.append("Keep recommendations concise and implementable.");
        
        return prompt.toString();
    }
    
    /**
     * Build prompt for content optimization
     */
    private String buildContentOptimizationPrompt(Campaign campaign, List<CampaignAnalytics> analyticsData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this campaign's performance and suggest content optimizations:\n\n");
        prompt.append("Campaign: ").append(campaign.getName()).append("\n");
        prompt.append("Campaign Type: ").append(campaign.getType()).append("\n\n");
        
        double avgCTR = analyticsData.stream()
                .filter(a -> a.getClickThroughRate() != null)
                .mapToDouble(CampaignAnalytics::getClickThroughRate)
                .average().orElse(0.0);
        
        prompt.append("Current CTR: ").append(avgCTR).append("%\n");
        prompt.append("Data Points: ").append(analyticsData.size()).append(" days\n\n");
        
        prompt.append("Provide specific recommendations for improving ad creative, copy, and visual elements. ");
        prompt.append("Focus on headlines, descriptions, call-to-action buttons, and visual design. ");
        prompt.append("Keep recommendations actionable and specific.");
        
        return prompt.toString();
    }
}