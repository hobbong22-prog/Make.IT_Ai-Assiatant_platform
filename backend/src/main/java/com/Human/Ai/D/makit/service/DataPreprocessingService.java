package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataPreprocessingService {
    
    /**
     * Preprocess campaign analytics data for prediction models
     */
    public Map<String, Object> preprocessForPrediction(List<CampaignAnalytics> analyticsData) {
        Map<String, Object> preprocessedData = new HashMap<>();
        
        if (analyticsData.isEmpty()) {
            return preprocessedData;
        }
        
        // Sort data by date
        List<CampaignAnalytics> sortedData = analyticsData.stream()
                .sorted(Comparator.comparing(CampaignAnalytics::getReportDate))
                .collect(Collectors.toList());
        
        // Basic statistics
        preprocessedData.put("dataPoints", sortedData.size());
        preprocessedData.put("dateRange", calculateDateRange(sortedData));
        
        // Aggregate metrics
        preprocessedData.put("aggregatedMetrics", calculateAggregatedMetrics(sortedData));
        
        // Time series features
        preprocessedData.put("timeSeriesFeatures", extractTimeSeriesFeatures(sortedData));
        
        // Trend indicators
        preprocessedData.put("trendIndicators", calculateTrendIndicators(sortedData));
        
        // Seasonality features
        preprocessedData.put("seasonalityFeatures", extractSeasonalityFeatures(sortedData));
        
        // Performance patterns
        preprocessedData.put("performancePatterns", analyzePerformancePatterns(sortedData));
        
        return preprocessedData;
    }
    
    /**
     * Normalize data for machine learning models
     */
    public Map<String, Object> normalizeData(List<CampaignAnalytics> analyticsData) {
        Map<String, Object> normalizedData = new HashMap<>();
        
        if (analyticsData.isEmpty()) {
            return normalizedData;
        }
        
        // Extract numeric features
        List<Double> impressions = extractNumericFeature(analyticsData, "impressions");
        List<Double> clicks = extractNumericFeature(analyticsData, "clicks");
        List<Double> conversions = extractNumericFeature(analyticsData, "conversions");
        List<Double> costs = extractNumericFeature(analyticsData, "cost");
        List<Double> revenues = extractNumericFeature(analyticsData, "revenue");
        
        // Normalize using min-max scaling
        normalizedData.put("normalizedImpressions", minMaxNormalize(impressions));
        normalizedData.put("normalizedClicks", minMaxNormalize(clicks));
        normalizedData.put("normalizedConversions", minMaxNormalize(conversions));
        normalizedData.put("normalizedCosts", minMaxNormalize(costs));
        normalizedData.put("normalizedRevenues", minMaxNormalize(revenues));
        
        // Store normalization parameters for inverse transformation
        normalizedData.put("normalizationParams", calculateNormalizationParams(analyticsData));
        
        return normalizedData;
    }
    
    /**
     * Clean and validate data
     */
    public List<CampaignAnalytics> cleanData(List<CampaignAnalytics> analyticsData) {
        return analyticsData.stream()
                .filter(this::isValidAnalytics)
                .map(this::fillMissingValues)
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate date range of the data
     */
    private Map<String, Object> calculateDateRange(List<CampaignAnalytics> sortedData) {
        Map<String, Object> dateRange = new HashMap<>();
        
        LocalDate startDate = sortedData.get(0).getReportDate();
        LocalDate endDate = sortedData.get(sortedData.size() - 1).getReportDate();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        
        dateRange.put("startDate", startDate);
        dateRange.put("endDate", endDate);
        dateRange.put("totalDays", daysBetween);
        
        return dateRange;
    }
    
    /**
     * Calculate aggregated metrics
     */
    private Map<String, Double> calculateAggregatedMetrics(List<CampaignAnalytics> data) {
        Map<String, Double> metrics = new HashMap<>();
        
        // Sum metrics
        metrics.put("totalImpressions", data.stream()
                .filter(a -> a.getImpressions() != null)
                .mapToDouble(CampaignAnalytics::getImpressions)
                .sum());
        
        metrics.put("totalClicks", data.stream()
                .filter(a -> a.getClicks() != null)
                .mapToDouble(CampaignAnalytics::getClicks)
                .sum());
        
        metrics.put("totalConversions", data.stream()
                .filter(a -> a.getConversions() != null)
                .mapToDouble(CampaignAnalytics::getConversions)
                .sum());
        
        metrics.put("totalCost", data.stream()
                .filter(a -> a.getCost() != null)
                .mapToDouble(CampaignAnalytics::getCost)
                .sum());
        
        metrics.put("totalRevenue", data.stream()
                .filter(a -> a.getRevenue() != null)
                .mapToDouble(CampaignAnalytics::getRevenue)
                .sum());
        
        // Average metrics
        metrics.put("avgCTR", data.stream()
                .filter(a -> a.getClickThroughRate() != null)
                .mapToDouble(CampaignAnalytics::getClickThroughRate)
                .average()
                .orElse(0.0));
        
        metrics.put("avgConversionRate", data.stream()
                .filter(a -> a.getConversionRate() != null)
                .mapToDouble(CampaignAnalytics::getConversionRate)
                .average()
                .orElse(0.0));
        
        metrics.put("avgROAS", data.stream()
                .filter(a -> a.getReturnOnAdSpend() != null)
                .mapToDouble(CampaignAnalytics::getReturnOnAdSpend)
                .average()
                .orElse(0.0));
        
        return metrics;
    }
    
    /**
     * Extract time series features
     */
    private Map<String, Object> extractTimeSeriesFeatures(List<CampaignAnalytics> data) {
        Map<String, Object> features = new HashMap<>();
        
        // Moving averages
        features.put("movingAverage7Days", calculateMovingAverage(data, 7));
        features.put("movingAverage14Days", calculateMovingAverage(data, 14));
        features.put("movingAverage30Days", calculateMovingAverage(data, 30));
        
        // Volatility measures
        features.put("volatility", calculateVolatility(data));
        
        // Lag features
        features.put("lagFeatures", calculateLagFeatures(data));
        
        return features;
    }
    
    /**
     * Calculate trend indicators
     */
    private Map<String, Double> calculateTrendIndicators(List<CampaignAnalytics> data) {
        Map<String, Double> indicators = new HashMap<>();
        
        if (data.size() < 2) {
            return indicators;
        }
        
        // Linear trend slope for key metrics
        indicators.put("impressionsTrend", calculateLinearTrend(data, "impressions"));
        indicators.put("clicksTrend", calculateLinearTrend(data, "clicks"));
        indicators.put("conversionsTrend", calculateLinearTrend(data, "conversions"));
        indicators.put("revenueTrend", calculateLinearTrend(data, "revenue"));
        indicators.put("performanceScoreTrend", calculateLinearTrend(data, "performanceScore"));
        
        return indicators;
    }
    
    /**
     * Extract seasonality features
     */
    private Map<String, Object> extractSeasonalityFeatures(List<CampaignAnalytics> data) {
        Map<String, Object> features = new HashMap<>();
        
        // Day of week patterns
        Map<Integer, Double> dayOfWeekPerformance = new HashMap<>();
        for (CampaignAnalytics analytics : data) {
            int dayOfWeek = analytics.getReportDate().getDayOfWeek().getValue();
            Double performance = analytics.getPerformanceScore();
            if (performance != null) {
                dayOfWeekPerformance.merge(dayOfWeek, performance, Double::sum);
            }
        }
        features.put("dayOfWeekPatterns", dayOfWeekPerformance);
        
        // Month patterns
        Map<Integer, Double> monthPerformance = new HashMap<>();
        for (CampaignAnalytics analytics : data) {
            int month = analytics.getReportDate().getMonthValue();
            Double performance = analytics.getPerformanceScore();
            if (performance != null) {
                monthPerformance.merge(month, performance, Double::sum);
            }
        }
        features.put("monthlyPatterns", monthPerformance);
        
        return features;
    }
    
    /**
     * Analyze performance patterns
     */
    private Map<String, Object> analyzePerformancePatterns(List<CampaignAnalytics> data) {
        Map<String, Object> patterns = new HashMap<>();
        
        // Performance distribution
        List<Double> performanceScores = data.stream()
                .filter(a -> a.getPerformanceScore() != null)
                .map(CampaignAnalytics::getPerformanceScore)
                .collect(Collectors.toList());
        
        if (!performanceScores.isEmpty()) {
            patterns.put("performanceDistribution", calculateDistributionStats(performanceScores));
        }
        
        // Correlation analysis
        patterns.put("correlations", calculateCorrelations(data));
        
        return patterns;
    }
    
    /**
     * Extract numeric feature from analytics data
     */
    private List<Double> extractNumericFeature(List<CampaignAnalytics> data, String feature) {
        return data.stream()
                .map(analytics -> {
                    switch (feature) {
                        case "impressions": return analytics.getImpressions();
                        case "clicks": return analytics.getClicks();
                        case "conversions": return analytics.getConversions();
                        case "cost": return analytics.getCost();
                        case "revenue": return analytics.getRevenue();
                        case "performanceScore": return analytics.getPerformanceScore();
                        default: return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Min-max normalization
     */
    private List<Double> minMaxNormalize(List<Double> values) {
        if (values.isEmpty()) {
            return values;
        }
        
        double min = Collections.min(values);
        double max = Collections.max(values);
        
        if (max == min) {
            return values.stream().map(v -> 0.5).collect(Collectors.toList());
        }
        
        return values.stream()
                .map(v -> (v - min) / (max - min))
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate normalization parameters
     */
    private Map<String, Map<String, Double>> calculateNormalizationParams(List<CampaignAnalytics> data) {
        Map<String, Map<String, Double>> params = new HashMap<>();
        
        String[] features = {"impressions", "clicks", "conversions", "cost", "revenue"};
        
        for (String feature : features) {
            List<Double> values = extractNumericFeature(data, feature);
            if (!values.isEmpty()) {
                Map<String, Double> featureParams = new HashMap<>();
                featureParams.put("min", Collections.min(values));
                featureParams.put("max", Collections.max(values));
                params.put(feature, featureParams);
            }
        }
        
        return params;
    }
    
    /**
     * Validate analytics data
     */
    private boolean isValidAnalytics(CampaignAnalytics analytics) {
        return analytics.getReportDate() != null &&
               analytics.getCampaign() != null &&
               (analytics.getImpressions() != null || 
                analytics.getClicks() != null || 
                analytics.getConversions() != null);
    }
    
    /**
     * Fill missing values with appropriate defaults
     */
    private CampaignAnalytics fillMissingValues(CampaignAnalytics analytics) {
        if (analytics.getImpressions() == null) {
            analytics.setImpressions(0.0);
        }
        if (analytics.getClicks() == null) {
            analytics.setClicks(0.0);
        }
        if (analytics.getConversions() == null) {
            analytics.setConversions(0.0);
        }
        if (analytics.getCost() == null) {
            analytics.setCost(0.0);
        }
        if (analytics.getRevenue() == null) {
            analytics.setRevenue(0.0);
        }
        
        return analytics;
    }
    
    /**
     * Calculate moving average for performance scores
     */
    private List<Double> calculateMovingAverage(List<CampaignAnalytics> data, int window) {
        List<Double> movingAverages = new ArrayList<>();
        List<Double> performanceScores = extractNumericFeature(data, "performanceScore");
        
        for (int i = 0; i < performanceScores.size(); i++) {
            int start = Math.max(0, i - window + 1);
            double average = performanceScores.subList(start, i + 1).stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            movingAverages.add(average);
        }
        
        return movingAverages;
    }
    
    /**
     * Calculate volatility of performance scores
     */
    private double calculateVolatility(List<CampaignAnalytics> data) {
        List<Double> performanceScores = extractNumericFeature(data, "performanceScore");
        
        if (performanceScores.size() < 2) {
            return 0.0;
        }
        
        double mean = performanceScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = performanceScores.stream()
                .mapToDouble(score -> Math.pow(score - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * Calculate lag features
     */
    private Map<String, List<Double>> calculateLagFeatures(List<CampaignAnalytics> data) {
        Map<String, List<Double>> lagFeatures = new HashMap<>();
        
        List<Double> performanceScores = extractNumericFeature(data, "performanceScore");
        
        // 1-day lag
        List<Double> lag1 = new ArrayList<>();
        lag1.add(0.0); // First value has no lag
        for (int i = 1; i < performanceScores.size(); i++) {
            lag1.add(performanceScores.get(i - 1));
        }
        lagFeatures.put("lag1", lag1);
        
        // 7-day lag
        List<Double> lag7 = new ArrayList<>();
        for (int i = 0; i < performanceScores.size(); i++) {
            if (i >= 7) {
                lag7.add(performanceScores.get(i - 7));
            } else {
                lag7.add(0.0);
            }
        }
        lagFeatures.put("lag7", lag7);
        
        return lagFeatures;
    }
    
    /**
     * Calculate linear trend slope
     */
    private double calculateLinearTrend(List<CampaignAnalytics> data, String feature) {
        List<Double> values = extractNumericFeature(data, feature);
        
        if (values.size() < 2) {
            return 0.0;
        }
        
        // Simple linear regression slope calculation
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }
    
    /**
     * Calculate distribution statistics
     */
    private Map<String, Double> calculateDistributionStats(List<Double> values) {
        Map<String, Double> stats = new HashMap<>();
        
        Collections.sort(values);
        
        stats.put("min", Collections.min(values));
        stats.put("max", Collections.max(values));
        stats.put("mean", values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        stats.put("median", calculateMedian(values));
        stats.put("q25", calculatePercentile(values, 25));
        stats.put("q75", calculatePercentile(values, 75));
        
        return stats;
    }
    
    /**
     * Calculate median
     */
    private double calculateMedian(List<Double> sortedValues) {
        int n = sortedValues.size();
        if (n % 2 == 0) {
            return (sortedValues.get(n / 2 - 1) + sortedValues.get(n / 2)) / 2.0;
        } else {
            return sortedValues.get(n / 2);
        }
    }
    
    /**
     * Calculate percentile
     */
    private double calculatePercentile(List<Double> sortedValues, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }
    
    /**
     * Calculate correlations between metrics
     */
    private Map<String, Double> calculateCorrelations(List<CampaignAnalytics> data) {
        Map<String, Double> correlations = new HashMap<>();
        
        List<Double> impressions = extractNumericFeature(data, "impressions");
        List<Double> clicks = extractNumericFeature(data, "clicks");
        List<Double> conversions = extractNumericFeature(data, "conversions");
        List<Double> revenue = extractNumericFeature(data, "revenue");
        
        if (impressions.size() == clicks.size() && impressions.size() > 1) {
            correlations.put("impressions_clicks", calculateCorrelation(impressions, clicks));
        }
        
        if (clicks.size() == conversions.size() && clicks.size() > 1) {
            correlations.put("clicks_conversions", calculateCorrelation(clicks, conversions));
        }
        
        if (conversions.size() == revenue.size() && conversions.size() > 1) {
            correlations.put("conversions_revenue", calculateCorrelation(conversions, revenue));
        }
        
        return correlations;
    }
    
    /**
     * Calculate Pearson correlation coefficient
     */
    private double calculateCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.size() < 2) {
            return 0.0;
        }
        
        int n = x.size();
        double sumX = x.stream().mapToDouble(Double::doubleValue).sum();
        double sumY = y.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0, sumX2 = 0, sumY2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumXY += x.get(i) * y.get(i);
            sumX2 += x.get(i) * x.get(i);
            sumY2 += y.get(i) * y.get(i);
        }
        
        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        
        return denominator != 0 ? numerator / denominator : 0.0;
    }
}