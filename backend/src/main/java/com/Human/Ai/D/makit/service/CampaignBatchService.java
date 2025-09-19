package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAlert;
import com.Human.Ai.D.makit.domain.CampaignSchedule;
import com.Human.Ai.D.makit.repository.CampaignRepository;
import com.Human.Ai.D.makit.repository.CampaignScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CampaignBatchService {
    
    private static final Logger logger = LoggerFactory.getLogger(CampaignBatchService.class);
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private CampaignScheduleRepository campaignScheduleRepository;
    
    @Autowired
    private EnhancedCampaignService enhancedCampaignService;
    
    @Autowired
    private WebSocketNotificationService notificationService;

    /**
     * Process scheduled campaigns every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void processScheduledCampaigns() {
        logger.debug("Processing scheduled campaigns...");
        
        try {
            enhancedCampaignService.processScheduledCampaigns();
        } catch (Exception e) {
            logger.error("Error processing scheduled campaigns", e);
        }
    }

    /**
     * Monitor campaign performance every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorCampaignPerformance() {
        logger.debug("Monitoring campaign performance...");
        
        try {
            List<Campaign> activeCampaigns = campaignRepository.findByStatus(Campaign.CampaignStatus.ACTIVE);
            
            for (Campaign campaign : activeCampaigns) {
                enhancedCampaignService.monitorCampaignPerformance(campaign);
            }
            
            logger.info("Monitored {} active campaigns", activeCampaigns.size());
        } catch (Exception e) {
            logger.error("Error monitoring campaign performance", e);
        }
    }

    /**
     * Auto-pause campaigns that have exceeded their end date
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void autoPauseExpiredCampaigns() {
        logger.debug("Checking for expired campaigns...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Campaign> expiredCampaigns = campaignRepository.findExpiredActiveCampaigns(now);
            
            for (Campaign campaign : expiredCampaigns) {
                campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
                campaignRepository.save(campaign);
                
                // Send notification
                notificationService.sendCampaignStatusUpdate(campaign.getUser(), campaign);
                
                logger.info("Auto-completed expired campaign: {}", campaign.getName());
            }
            
            if (!expiredCampaigns.isEmpty()) {
                logger.info("Auto-completed {} expired campaigns", expiredCampaigns.size());
            }
        } catch (Exception e) {
            logger.error("Error auto-pausing expired campaigns", e);
        }
    }

    /**
     * Clean up old campaign schedules
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldSchedules() {
        logger.debug("Cleaning up old campaign schedules...");
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            
            List<CampaignSchedule> oldSchedules = campaignScheduleRepository
                    .findScheduledCampaignsBetween(LocalDateTime.now().minusYears(1), cutoffDate);
            
            for (CampaignSchedule schedule : oldSchedules) {
                if (schedule.getScheduleType() == CampaignSchedule.ScheduleType.SCHEDULED &&
                    schedule.getScheduledEndTime() != null &&
                    schedule.getScheduledEndTime().isBefore(cutoffDate)) {
                    
                    schedule.setIsActive(false);
                    campaignScheduleRepository.save(schedule);
                }
            }
            
            logger.info("Cleaned up {} old campaign schedules", oldSchedules.size());
        } catch (Exception e) {
            logger.error("Error cleaning up old schedules", e);
        }
    }

    /**
     * Generate daily campaign performance reports
     */
    @Scheduled(cron = "0 0 8 * * ?") // Daily at 8 AM
    public void generateDailyReports() {
        logger.debug("Generating daily campaign reports...");
        
        try {
            List<Campaign> activeCampaigns = campaignRepository.findByStatus(Campaign.CampaignStatus.ACTIVE);
            
            for (Campaign campaign : activeCampaigns) {
                generateCampaignReport(campaign);
            }
            
            logger.info("Generated daily reports for {} campaigns", activeCampaigns.size());
        } catch (Exception e) {
            logger.error("Error generating daily reports", e);
        }
    }

    /**
     * Send weekly campaign summary
     */
    @Scheduled(cron = "0 0 9 * * MON") // Every Monday at 9 AM
    public void sendWeeklySummary() {
        logger.debug("Sending weekly campaign summary...");
        
        try {
            // This would typically generate and send a comprehensive weekly report
            // For now, we'll just log the activity
            
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            List<Campaign> recentCampaigns = campaignRepository.findCampaignsCreatedAfter(weekStart);
            
            logger.info("Weekly summary: {} campaigns created in the last week", recentCampaigns.size());
            
            // Send summary notification to administrators
            // notificationService.sendWeeklySummary(adminUsers, summary);
            
        } catch (Exception e) {
            logger.error("Error sending weekly summary", e);
        }
    }

    /**
     * Optimize campaign budgets based on performance
     */
    @Scheduled(cron = "0 0 10 * * ?") // Daily at 10 AM
    public void optimizeCampaignBudgets() {
        logger.debug("Optimizing campaign budgets...");
        
        try {
            List<Campaign> activeCampaigns = campaignRepository.findByStatus(Campaign.CampaignStatus.ACTIVE);
            
            for (Campaign campaign : activeCampaigns) {
                optimizeCampaignBudget(campaign);
            }
            
            logger.info("Optimized budgets for {} campaigns", activeCampaigns.size());
        } catch (Exception e) {
            logger.error("Error optimizing campaign budgets", e);
        }
    }

    // Helper methods
    private void generateCampaignReport(Campaign campaign) {
        // Generate performance report for the campaign
        // This would typically create a detailed report and store it or send it
        logger.debug("Generated report for campaign: {}", campaign.getName());
    }

    private void optimizeCampaignBudget(Campaign campaign) {
        // Analyze campaign performance and suggest budget optimizations
        // This would use AI/ML models to recommend budget adjustments
        
        if (campaign.getBudget() == null) {
            return;
        }
        
        // Simple optimization logic - in practice this would be more sophisticated
        List<CampaignAlert> alerts = enhancedCampaignService.getCampaignAlerts(campaign.getId());
        
        boolean hasPerformanceIssues = alerts.stream()
                .anyMatch(alert -> alert.getAlertType() == CampaignAlert.AlertType.LOW_PERFORMANCE ||
                                 alert.getAlertType() == CampaignAlert.AlertType.LOW_CTR);
        
        if (hasPerformanceIssues) {
            logger.info("Campaign {} has performance issues, consider budget reallocation", 
                       campaign.getName());
        }
    }
}