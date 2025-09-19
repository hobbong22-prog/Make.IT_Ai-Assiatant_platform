package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.repository.CampaignAnalyticsRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class PredictiveAnalyticsEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(PredictiveAnalyticsEngine.class);
    
    @Autowired
    private CampaignAnalyticsRepository analyticsRepository;
    
    @Autowired
    private BedrockService bedrockService;
    
    @Autowired
    private DataPreprocessingService dataPreprocessingService;
    
    /**
     * Generate performance predictions for a campaign
     */
    public CompletableFuture<PredictionResult> predictCampaignPerformance(Campaign campaign, int daysAhead) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get historical data
                List<CampaignAnalytics> historicalData = getHistoricalData(campaign, 30);
                
                if (historicalData.isEmpty()) {
                    return new PredictionResult("Insufficient historical data for prediction", false);
                }
                
                // Preprocess data for prediction
                Map<String, Object> preprocessedData = dataPreprocessingService
                        .preprocessForPrediction(historicalData);
                
                // Generate prediction using AI model
                String predictionPrompt = buildPredictionPrompt(campaign, preprocessedData, daysAhead);
                String aiPrediction = bedrockService.generateText(predictionPrompt, "claude-3-sonnet-20240229-v1:0");
                
                // Parse and structure the prediction
                PredictionResult result = parsePredictionResult(aiPrediction, daysAhead);
                result.setSuccessful(true);
                
                return result;
                
            } catch (Exception e) {
                logger.error("Failed to generate prediction for campaign {}: {}", 
                           campaign.getId(), e.getMessage());
                return new PredictionResult("Prediction generation failed: " + e.getMessage(), false);
            }
        });
    }
    
    /**
     * Analyze trends in campaign performance
     */
    public CompletableFuture<TrendAnalysis> analyzeTrends(Campaign campaign) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<CampaignAnalytics> data = getHistoricalData(campaign, 60);
                
                if (data.size() < 7) {
                    return new TrendAnalysis("Insufficient data for trend analysis", false);
                }
                
                // Calculate trend metrics
                Map<String, Double> trendMetrics = calculateTrendMetrics(data);
                
                // Generate AI-powered trend insights
                String trendPrompt = buildTrendAnalysisPrompt(campaign, data, trendMetrics);
                String aiAnalysis = bedrockService.generateText(trendPrompt, "claude-3-haiku-20240307-v1:0");
                
                TrendAnalysis analysis = new TrendAnalysis();
                analysis.setTrendMetrics(trendMetrics);
                analysis.setAiInsights(aiAnalysis);
                analysis.setSuccessful(true);
                
                return analysis;
                
            } catch (Exception e) {
                logger.error("Failed to analyze trends for campaign {}: {}", 
                           campaign.getId(), e.getMessage());
                return new TrendAnalysis("Trend analysis failed: " + e.getMessage(), false);
            }
        });
    }
    
    /**
     * Predict seasonal performance patterns
     */
    public CompletableFuture<SeasonalForecast> generateSeasonalForecast(Campaign campaign) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<CampaignAnalytics> yearlyData = getHistoricalData(campaign, 365);
                
                if (yearlyData.size() < 30) {
                    return new SeasonalForecast("Insufficient data for seasonal analysis", false);
                }
                
                // Analyze seasonal patterns
                Map<String, Object> seasonalPatterns = analyzeSeasonalPatterns(yearlyData);
                
                // Generate forecast using AI
                String forecastPrompt = buildSeasonalForecastPrompt(campaign, seasonalPatterns);
                String aiForecast = bedrockService.generateText(forecastPrompt, "claude-3-sonnet-20240229-v1:0");
                
                SeasonalForecast forecast = new SeasonalForecast();
                forecast.setSeasonalPatterns(seasonalPatterns);
                forecast.setAiForecast(aiForecast);
                forecast.setSuccessful(true);
                
                return forecast;
                
            } catch (Exception e) {
                logger.error("Failed to generate seasonal forecast for campaign {}: {}", 
                           campaign.getId(), e.getMessage());
                return new SeasonalForecast("Seasonal forecast failed: " + e.getMessage(), false);
            }
        });
    }
    
    /**
     * Get historical analytics data for a campaign
     */
    private List<CampaignAnalytics> getHistoricalData(Campaign campaign, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return analyticsRepository.findByCampaignAndDateRange(campaign, startDate, endDate);
    }
    
    /**
     * Build prediction prompt for AI model
     */
    private String buildPredictionPrompt(Campaign campaign, Map<String, Object> data, int daysAhead) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("As a marketing analytics expert, predict the performance of this campaign:\n\n");
        prompt.append("Campaign: ").append(campaign.getName()).append("\n");
        prompt.append("Campaign Type: ").append(campaign.getType()).append("\n");
        prompt.append("Target Audience: ").append(campaign.getTargetAudience()).append("\n");
        prompt.append("Budget: $").append(campaign.getBudget()).append("\n\n");
        
        prompt.append("Historical Performance Data:\n");
        prompt.append(data.toString()).append("\n\n");
        
        prompt.append("Please predict performance for the next ").append(daysAhead).append(" days.\n");
        prompt.append("Provide predictions for:\n");
        prompt.append("1. Expected impressions\n");
        prompt.append("2. Expected clicks\n");
        prompt.append("3. Expected conversions\n");
        prompt.append("4. Expected cost\n");
        prompt.append("5. Expected revenue\n");
        prompt.append("6. Confidence level (0-100%)\n");
        prompt.append("7. Key factors influencing the prediction\n");
        prompt.append("8. Potential risks and opportunities\n\n");
        
        prompt.append("Format the response as structured data with clear metrics and explanations.");
        
        return prompt.toString();
    }
    
    /**
     * Build trend analysis prompt
     */
    private String buildTrendAnalysisPrompt(Campaign campaign, List<CampaignAnalytics> data, 
                                          Map<String, Double> trendMetrics) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the performance trends for this campaign:\n\n");
        prompt.append("Campaign: ").append(campaign.getName()).append("\n");
        prompt.append("Data Points: ").append(data.size()).append(" days\n\n");
        
        prompt.append("Calculated Trend Metrics:\n");
        trendMetrics.forEach((key, value) -> 
            prompt.append(key).append(": ").append(value).append("\n"));
        
        prompt.append("\nProvide analysis on:\n");
        prompt.append("1. Overall performance trend (improving/declining/stable)\n");
        prompt.append("2. Key performance drivers\n");
        prompt.append("3. Anomalies or significant changes\n");
        prompt.append("4. Recommendations for trend optimization\n");
        
        return prompt.toString();
    }
    
    /**
     * Build seasonal forecast prompt
     */
    private String buildSeasonalForecastPrompt(Campaign campaign, Map<String, Object> patterns) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a seasonal performance forecast for this campaign:\n\n");
        prompt.append("Campaign: ").append(campaign.getName()).append("\n");
        prompt.append("Campaign Type: ").append(campaign.getType()).append("\n\n");
        
        prompt.append("Identified Seasonal Patterns:\n");
        prompt.append(patterns.toString()).append("\n\n");
        
        prompt.append("Provide forecast for:\n");
        prompt.append("1. Next 3 months performance expectations\n");
        prompt.append("2. Seasonal peaks and valleys\n");
        prompt.append("3. Optimal timing for budget allocation\n");
        prompt.append("4. Seasonal optimization strategies\n");
        
        return prompt.toString();
    }
    
    /**
     * Calculate trend metrics from historical data
     */
    private Map<String, Double> calculateTrendMetrics(List<CampaignAnalytics> data) {
        Map<String, Double> metrics = new HashMap<>();
        
        if (data.size() < 2) {
            return metrics;
        }
        
        // Calculate growth rates
        CampaignAnalytics latest = data.get(0);
        CampaignAnalytics previous = data.get(data.size() - 1);
        
        if (previous.getImpressions() != null && previous.getImpressions() > 0) {
            double impressionGrowth = ((latest.getImpressions() - previous.getImpressions()) 
                                    / previous.getImpressions()) * 100;
            metrics.put("impressionGrowthRate", impressionGrowth);
        }
        
        if (previous.getClicks() != null && previous.getClicks() > 0) {
            double clickGrowth = ((latest.getClicks() - previous.getClicks()) 
                                / previous.getClicks()) * 100;
            metrics.put("clickGrowthRate", clickGrowth);
        }
        
        if (previous.getRevenue() != null && previous.getRevenue() > 0) {
            double revenueGrowth = ((latest.getRevenue() - previous.getRevenue()) 
                                  / previous.getRevenue()) * 100;
            metrics.put("revenueGrowthRate", revenueGrowth);
        }
        
        // Calculate average performance score
        double avgPerformanceScore = data.stream()
                .filter(analytics -> analytics.getPerformanceScore() != null)
                .mapToDouble(CampaignAnalytics::getPerformanceScore)
                .average()
                .orElse(0.0);
        metrics.put("averagePerformanceScore", avgPerformanceScore);
        
        return metrics;
    }
    
    /**
     * Analyze seasonal patterns in the data
     */
    private Map<String, Object> analyzeSeasonalPatterns(List<CampaignAnalytics> data) {
        Map<String, Object> patterns = new HashMap<>();
        
        // Group data by month and calculate averages
        Map<Integer, Double> monthlyPerformance = new HashMap<>();
        Map<Integer, Integer> monthlyCount = new HashMap<>();
        
        for (CampaignAnalytics analytics : data) {
            int month = analytics.getReportDate().getMonthValue();
            Double score = analytics.getPerformanceScore();
            
            if (score != null) {
                monthlyPerformance.merge(month, score, Double::sum);
                monthlyCount.merge(month, 1, Integer::sum);
            }
        }
        
        // Calculate monthly averages
        Map<Integer, Double> monthlyAverages = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : monthlyPerformance.entrySet()) {
            int month = entry.getKey();
            double average = entry.getValue() / monthlyCount.get(month);
            monthlyAverages.put(month, average);
        }
        
        patterns.put("monthlyAverages", monthlyAverages);
        patterns.put("dataPoints", data.size());
        
        return patterns;
    }
    
    /**
     * Parse AI prediction result into structured format
     */
    private PredictionResult parsePredictionResult(String aiPrediction, int daysAhead) {
        PredictionResult result = new PredictionResult();
        result.setPredictionPeriod(daysAhead);
        result.setRawPrediction(aiPrediction);
        
        // In a real implementation, you would parse the AI response
        // to extract structured metrics. For now, we'll set default values.
        result.setPredictedImpressions(0.0);
        result.setPredictedClicks(0.0);
        result.setPredictedConversions(0.0);
        result.setPredictedCost(0.0);
        result.setPredictedRevenue(0.0);
        result.setConfidenceLevel(75.0);
        
        return result;
    }
    
    // Inner classes for structured results
    public static class PredictionResult {
        private String message;
        private boolean successful;
        private int predictionPeriod;
        private String rawPrediction;
        private Double predictedImpressions;
        private Double predictedClicks;
        private Double predictedConversions;
        private Double predictedCost;
        private Double predictedRevenue;
        private Double confidenceLevel;
        
        public PredictionResult() {}
        
        public PredictionResult(String message, boolean successful) {
            this.message = message;
            this.successful = successful;
        }
        
        // Getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        
        public int getPredictionPeriod() { return predictionPeriod; }
        public void setPredictionPeriod(int predictionPeriod) { this.predictionPeriod = predictionPeriod; }
        
        public String getRawPrediction() { return rawPrediction; }
        public void setRawPrediction(String rawPrediction) { this.rawPrediction = rawPrediction; }
        
        public Double getPredictedImpressions() { return predictedImpressions; }
        public void setPredictedImpressions(Double predictedImpressions) { this.predictedImpressions = predictedImpressions; }
        
        public Double getPredictedClicks() { return predictedClicks; }
        public void setPredictedClicks(Double predictedClicks) { this.predictedClicks = predictedClicks; }
        
        public Double getPredictedConversions() { return predictedConversions; }
        public void setPredictedConversions(Double predictedConversions) { this.predictedConversions = predictedConversions; }
        
        public Double getPredictedCost() { return predictedCost; }
        public void setPredictedCost(Double predictedCost) { this.predictedCost = predictedCost; }
        
        public Double getPredictedRevenue() { return predictedRevenue; }
        public void setPredictedRevenue(Double predictedRevenue) { this.predictedRevenue = predictedRevenue; }
        
        public Double getConfidenceLevel() { return confidenceLevel; }
        public void setConfidenceLevel(Double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    }
    
    public static class TrendAnalysis {
        private String message;
        private boolean successful;
        private Map<String, Double> trendMetrics;
        private String aiInsights;
        
        public TrendAnalysis() {}
        
        public TrendAnalysis(String message, boolean successful) {
            this.message = message;
            this.successful = successful;
        }
        
        // Getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        
        public Map<String, Double> getTrendMetrics() { return trendMetrics; }
        public void setTrendMetrics(Map<String, Double> trendMetrics) { this.trendMetrics = trendMetrics; }
        
        public String getAiInsights() { return aiInsights; }
        public void setAiInsights(String aiInsights) { this.aiInsights = aiInsights; }
    }
    
    public static class SeasonalForecast {
        private String message;
        private boolean successful;
        private Map<String, Object> seasonalPatterns;
        private String aiForecast;
        
        public SeasonalForecast() {}
        
        public SeasonalForecast(String message, boolean successful) {
            this.message = message;
            this.successful = successful;
        }
        
        // Getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        
        public Map<String, Object> getSeasonalPatterns() { return seasonalPatterns; }
        public void setSeasonalPatterns(Map<String, Object> seasonalPatterns) { this.seasonalPatterns = seasonalPatterns; }
        
        public String getAiForecast() { return aiForecast; }
        public void setAiForecast(String aiForecast) { this.aiForecast = aiForecast; }
    }
}