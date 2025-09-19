package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignAnalyticsRepository extends JpaRepository<CampaignAnalytics, Long> {
    
    List<CampaignAnalytics> findByCampaign(Campaign campaign);
    
    List<CampaignAnalytics> findByCampaignOrderByReportDateDesc(Campaign campaign);
    
    Optional<CampaignAnalytics> findByCampaignAndReportDate(Campaign campaign, LocalDate reportDate);
    
    @Query("SELECT ca FROM CampaignAnalytics ca WHERE ca.campaign = :campaign " +
           "AND ca.reportDate BETWEEN :startDate AND :endDate ORDER BY ca.reportDate DESC")
    List<CampaignAnalytics> findByCampaignAndDateRange(
            @Param("campaign") Campaign campaign,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ca FROM CampaignAnalytics ca WHERE ca.campaign.user.id = :userId " +
           "AND ca.reportDate BETWEEN :startDate AND :endDate ORDER BY ca.reportDate DESC")
    List<CampaignAnalytics> findByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT AVG(ca.performanceScore) FROM CampaignAnalytics ca WHERE ca.campaign = :campaign")
    Double getAveragePerformanceScore(@Param("campaign") Campaign campaign);
    
    @Query("SELECT SUM(ca.revenue) FROM CampaignAnalytics ca WHERE ca.campaign = :campaign")
    Double getTotalRevenue(@Param("campaign") Campaign campaign);
    
    @Query("SELECT SUM(ca.cost) FROM CampaignAnalytics ca WHERE ca.campaign = :campaign")
    Double getTotalCost(@Param("campaign") Campaign campaign);
    
    @Query("SELECT ca FROM CampaignAnalytics ca WHERE ca.campaign = :campaign " +
           "ORDER BY ca.reportDate DESC LIMIT 1")
    Optional<CampaignAnalytics> findLatestByCampaign(@Param("campaign") Campaign campaign);
    
    @Query("SELECT ca FROM CampaignAnalytics ca WHERE ca.campaign.user.id = :userId " +
           "AND ca.performanceScore >= :minScore ORDER BY ca.performanceScore DESC")
    List<CampaignAnalytics> findTopPerformingCampaigns(
            @Param("userId") Long userId,
            @Param("minScore") Double minScore);
    
    // Dashboard-specific methods
    @Query("SELECT COALESCE(SUM(ca.impressions), 0) FROM CampaignAnalytics ca WHERE ca.campaign.user.id = :userId " +
           "AND ca.reportDate BETWEEN :startDate AND :endDate")
    Double sumImpressionsByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(ca.clicks), 0) FROM CampaignAnalytics ca WHERE ca.campaign.user.id = :userId " +
           "AND ca.reportDate BETWEEN :startDate AND :endDate")
    Double sumClicksByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ca FROM CampaignAnalytics ca WHERE ca.campaign IN :campaigns " +
           "AND ca.reportDate BETWEEN :startDate AND :endDate ORDER BY ca.reportDate DESC")
    List<CampaignAnalytics> findByCampaignsAndDateRange(
            @Param("campaigns") List<Campaign> campaigns,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}