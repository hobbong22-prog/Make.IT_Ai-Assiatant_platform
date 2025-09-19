package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.domain.CampaignMetrics;
import com.Human.Ai.D.makit.repository.CampaignAnalyticsRepository;
import com.Human.Ai.D.makit.repository.CampaignRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class CampaignAnalyticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CampaignAnalyticsService.class);
    
    @Autowired
    private CampaignAnalyticsRepository analyticsRepository;
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private BedrockService bedrockService;
    
    /**
     * Generate analytics report for a campaign on a specific date
     */
    public CampaignAnalytics generateAnalyticsReport(Long campaignId, LocalDate reportDate) {
        Campaign campaign = campaignRepository.findByIdWithMetrics(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
        
        // Check if analytics already exist for this date
        Optional<CampaignAnalytics> existing = analyticsRepository
                .findByCampaignAndReportDate(campaign, reportDate);
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create new analytics
        CampaignAnalytics analytics = new CampaignAnalytics(campaign, reportDate);
        
        // Aggregate metrics from campaign metrics
        aggregateMetrics(analytics, campaign);
        
        // Calculate derived metrics
        analytics.calculateMetrics();
        
        // Generate AI insights asynchronously
        generateAIInsights(analytics);
        
        return analyticsRepository.save(analytics);
    }
    
    /**
     * Get analytics for a campaign within a date range
     */
    @Cacheable(value = "campaignAnalytics", key = "#campaignId + '_' + #startDate + '_' + #endDate")
    public List<CampaignAnalytics> getAnalyticsByDateRange(Long campaignId, 
                                                          LocalDate startDate, 
                                                          LocalDate endDate) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
        
        return analyticsRepository.findByCampaignAndDateRange(campaign, startDate, endDate);
    }
    
    /**
     * Get latest analytics for a campaign
     */
    @Cacheable(value = "campaignAnalytics", key = "'latest_' + #campaignId")
    public Optional<CampaignAnalytics> getLatestAnalytics(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
        
        return analyticsRepository.findLatestByCampaign(campaign);
    }
    
    /**
     * Get analytics summary for user campaigns
     */
    @Cacheable(value = "campaignAnalytics", key = "'user_' + #userId + '_' + #startDate + '_' + #endDate")
    public List<CampaignAnalytics> getUserAnalyticsSummary(Long userId, 
                                                          LocalDate startDate, 
                                                          LocalDate endDate) {
        return analyticsRepository.findByUserAndDateRange(userId, startDate, endDate);
    }
    
    /**
     * Get top performing campaigns for a user
     */
    public List<CampaignAnalytics> getTopPerformingCampaigns(Long userId, Double minScore) {
        return analyticsRepository.findTopPerformingCampaigns(userId, minScore);
    }
    
    /**
     * Calculate performance score for analytics
     */
    public void calculatePerformanceScore(CampaignAnalytics analytics) {
        double score = 0.0;
        int factors = 0;
        
        // CTR contribution (0-30 points)
        if (analytics.getClickThroughRate() != null) {
            score += Math.min(analytics.getClickThroughRate() * 10, 30);
            factors++;
        }
        
        // Conversion rate contribution (0-30 points)
        if (analytics.getConversionRate() != null) {
            score += Math.min(analytics.getConversionRate() * 15, 30);
            factors++;
        }
        
        // ROAS contribution (0-40 points)
        if (analytics.getReturnOnAdSpend() != null) {
            double roasScore = Math.min(analytics.getReturnOnAdSpend() * 20, 40);
            score += roasScore;
            factors++;
        }
        
        // Normalize score to 0-100 scale
        if (factors > 0) {
            analytics.setPerformanceScore(score / factors * (100.0 / 100.0));
        } else {
            analytics.setPerformanceScore(0.0);
        }
    }
    
    /**
     * Aggregate metrics from campaign metrics data
     */
    private void aggregateMetrics(CampaignAnalytics analytics, Campaign campaign) {
        List<CampaignMetrics> metrics = campaign.getMetrics();
        
        if (metrics.isEmpty()) {
            return;
        }
        
        // Sum up metrics for the report date
        double totalImpressions = 0;
        double totalClicks = 0;
        double totalConversions = 0;
        double totalCost = 0;
        double totalRevenue = 0;
        
        for (CampaignMetrics metric : metrics) {
            if (metric.getRecordedAt().toLocalDate().equals(analytics.getReportDate())) {
                totalImpressions += metric.getImpressions() != null ? metric.getImpressions() : 0;
                totalClicks += metric.getClicks() != null ? metric.getClicks() : 0;
                totalConversions += metric.getConversions() != null ? metric.getConversions() : 0;
                totalCost += metric.getSpend() != null ? metric.getSpend() : 0;
                totalRevenue += metric.getRevenue() != null ? metric.getRevenue() : 0;
            }
        }
        
        analytics.setImpressions(totalImpressions);
        analytics.setClicks(totalClicks);
        analytics.setConversions(totalConversions);
        analytics.setCost(totalCost);
        analytics.setRevenue(totalRevenue);
    }
    
    /**
     * Generate AI-powered insights for the analytics
     */
    private void generateAIInsights(CampaignAnalytics analytics) {
        CompletableFuture.runAsync(() -> {
            try {
                String prompt = buildInsightsPrompt(analytics);
                String insights = bedrockService.generateText(prompt, "claude-3-haiku-20240307-v1:0");
                
                analytics.setAiInsights(insights);
                
                // Generate trend analysis
                String trendPrompt = buildTrendAnalysisPrompt(analytics);
                String trendAnalysis = bedrockService.generateText(trendPrompt, "claude-3-haiku-20240307-v1:0");
                
                analytics.setTrendAnalysis(trendAnalysis);
                
                // Calculate performance score
                calculatePerformanceScore(analytics);
                
                analyticsRepository.save(analytics);
                
            } catch (Exception e) {
                logger.error("Failed to generate AI insights for analytics {}: {}", 
                           analytics.getId(), e.getMessage());
            }
        });
    }
    
    /**
     * Build prompt for AI insights generation
     */
    private String buildInsightsPrompt(CampaignAnalytics analytics) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following campaign performance data and provide actionable insights:\n\n");
        prompt.append("Campaign: ").append(analytics.getCampaign().getName()).append("\n");
        prompt.append("Report Date: ").append(analytics.getReportDate()).append("\n");
        prompt.append("Impressions: ").append(analytics.getImpressions()).append("\n");
        prompt.append("Clicks: ").append(analytics.getClicks()).append("\n");
        prompt.append("Conversions: ").append(analytics.getConversions()).append("\n");
        prompt.append("Cost: $").append(analytics.getCost()).append("\n");
        prompt.append("Revenue: $").append(analytics.getRevenue()).append("\n");
        prompt.append("CTR: ").append(analytics.getClickThroughRate()).append("%\n");
        prompt.append("Conversion Rate: ").append(analytics.getConversionRate()).append("%\n");
        prompt.append("ROAS: ").append(analytics.getReturnOnAdSpend()).append("\n\n");
        
        prompt.append("Please provide:\n");
        prompt.append("1. Key performance highlights\n");
        prompt.append("2. Areas for improvement\n");
        prompt.append("3. Specific recommendations for optimization\n");
        prompt.append("4. Benchmark comparison insights\n");
        prompt.append("\nKeep the analysis concise and actionable.");
        
        return prompt.toString();
    }
    
    /**
     * Build prompt for trend analysis
     */
    private String buildTrendAnalysisPrompt(CampaignAnalytics analytics) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on this campaign performance data, analyze trends and patterns:\n\n");
        prompt.append("Performance Score: ").append(analytics.getPerformanceScore()).append("\n");
        prompt.append("CTR: ").append(analytics.getClickThroughRate()).append("%\n");
        prompt.append("Conversion Rate: ").append(analytics.getConversionRate()).append("%\n");
        prompt.append("ROAS: ").append(analytics.getReturnOnAdSpend()).append("\n\n");
        
        prompt.append("Provide trend analysis focusing on:\n");
        prompt.append("1. Performance trajectory indicators\n");
        prompt.append("2. Seasonal or temporal patterns\n");
        prompt.append("3. Predictive insights for future performance\n");
        prompt.append("4. Risk factors and opportunities\n");
        
        return prompt.toString();
    }
}