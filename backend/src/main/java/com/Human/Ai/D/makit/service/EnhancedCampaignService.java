package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class EnhancedCampaignService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedCampaignService.class);
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private CampaignTemplateRepository campaignTemplateRepository;
    
    @Autowired
    private CampaignScheduleRepository campaignScheduleRepository;
    
    @Autowired
    private CampaignAlertRepository campaignAlertRepository;
    
    @Autowired
    private CampaignService campaignService;
    
    @Autowired
    private WebSocketNotificationService notificationService;

    // Campaign Template Management
    public CampaignTemplate createTemplate(String name, String description, 
                                         Campaign.CampaignType type, String templateContent,
                                         Map<String, String> defaultParameters, User createdBy) {
        CampaignTemplate template = new CampaignTemplate(name, description, type, createdBy);
        template.setTemplateContent(templateContent);
        template.setDefaultParameters(defaultParameters);
        
        template = campaignTemplateRepository.save(template);
        logger.info("Created campaign template: {} for user: {}", name, createdBy.getUsername());
        
        return template;
    }
    
    public Campaign createCampaignFromTemplate(Long templateId, String campaignName, 
                                             Map<String, String> parameters, User user) {
        CampaignTemplate template = campaignTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Campaign template not found"));
        
        Campaign campaign = new Campaign(campaignName, template.getDescription(), template.getType(), user);
        
        // Apply template parameters
        Map<String, String> mergedParams = template.getDefaultParameters();
        if (parameters != null) {
            mergedParams.putAll(parameters);
        }
        
        // Process template content with parameters
        String processedContent = processTemplateContent(template.getTemplateContent(), mergedParams);
        campaign.setDescription(processedContent);
        
        campaign = campaignRepository.save(campaign);
        logger.info("Created campaign from template: {} -> {}", template.getName(), campaignName);
        
        return campaign;
    }
    
    public List<CampaignTemplate> getActiveTemplates() {
        return campaignTemplateRepository.findByIsActiveTrue();
    }
    
    public List<CampaignTemplate> getTemplatesByType(Campaign.CampaignType type) {
        return campaignTemplateRepository.findActiveTemplatesByType(type);
    }
    
    public CampaignTemplate updateTemplate(Long templateId, String name, String description,
                                         String templateContent, Map<String, String> defaultParameters) {
        CampaignTemplate template = campaignTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Campaign template not found"));
        
        template.setName(name);
        template.setDescription(description);
        template.setTemplateContent(templateContent);
        template.setDefaultParameters(defaultParameters);
        
        return campaignTemplateRepository.save(template);
    }
    
    public void deactivateTemplate(Long templateId) {
        CampaignTemplate template = campaignTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Campaign template not found"));
        
        template.setIsActive(false);
        campaignTemplateRepository.save(template);
    }

    // Advanced Campaign Scheduling
    public CampaignSchedule scheduleImmediateCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        CampaignSchedule schedule = new CampaignSchedule(campaign, CampaignSchedule.ScheduleType.IMMEDIATE);
        schedule = campaignScheduleRepository.save(schedule);
        
        // Activate campaign immediately
        campaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        campaignRepository.save(campaign);
        
        logger.info("Scheduled immediate campaign: {}", campaign.getName());
        return schedule;
    }
    
    public CampaignSchedule scheduleDelayedCampaign(Long campaignId, LocalDateTime startTime, 
                                                   LocalDateTime endTime) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        CampaignSchedule schedule = new CampaignSchedule(campaign, CampaignSchedule.ScheduleType.SCHEDULED);
        schedule.setScheduledStartTime(startTime);
        schedule.setScheduledEndTime(endTime);
        
        schedule = campaignScheduleRepository.save(schedule);
        logger.info("Scheduled delayed campaign: {} for {}", campaign.getName(), startTime);
        
        return schedule;
    }
    
    public CampaignSchedule scheduleRecurringCampaign(Long campaignId, 
                                                     CampaignSchedule.RecurrencePattern pattern,
                                                     Integer interval, LocalDateTime endDate) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        CampaignSchedule schedule = new CampaignSchedule(campaign, CampaignSchedule.ScheduleType.RECURRING);
        schedule.setRecurrencePattern(pattern);
        schedule.setRecurrenceInterval(interval);
        schedule.setRecurrenceEndDate(endDate);
        
        schedule = campaignScheduleRepository.save(schedule);
        logger.info("Scheduled recurring campaign: {} with pattern: {}", campaign.getName(), pattern);
        
        return schedule;
    }
    
    @Async
    public CompletableFuture<Void> processScheduledCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        
        // Process scheduled campaigns
        List<CampaignSchedule> scheduledCampaigns = campaignScheduleRepository
                .findScheduledCampaignsForExecution(now);
        
        for (CampaignSchedule schedule : scheduledCampaigns) {
            try {
                activateCampaign(schedule.getCampaign());
                logger.info("Activated scheduled campaign: {}", schedule.getCampaign().getName());
            } catch (Exception e) {
                logger.error("Failed to activate scheduled campaign: {}", 
                           schedule.getCampaign().getName(), e);
            }
        }
        
        // Process recurring campaigns
        List<CampaignSchedule> recurringCampaigns = campaignScheduleRepository
                .findRecurringCampaignsForExecution(now);
        
        for (CampaignSchedule schedule : recurringCampaigns) {
            try {
                if (schedule.isScheduledForNow()) {
                    activateCampaign(schedule.getCampaign());
                    logger.info("Activated recurring campaign: {}", schedule.getCampaign().getName());
                }
            } catch (Exception e) {
                logger.error("Failed to activate recurring campaign: {}", 
                           schedule.getCampaign().getName(), e);
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }

    // Campaign Performance Monitoring
    public void monitorCampaignPerformance(Campaign campaign) {
        List<CampaignMetrics> recentMetrics = getRecentMetrics(campaign);
        
        if (recentMetrics.isEmpty()) {
            return;
        }
        
        CampaignMetrics latestMetrics = recentMetrics.get(0);
        
        // Check for budget exceeded
        if (campaign.getBudget() != null && latestMetrics.getSpend() != null) {
            if (latestMetrics.getSpend() > campaign.getBudget()) {
                createAlert(campaign, CampaignAlert.AlertType.BUDGET_EXCEEDED,
                          CampaignAlert.AlertSeverity.HIGH,
                          "Campaign budget exceeded: $" + latestMetrics.getSpend() + " > $" + campaign.getBudget(),
                          campaign.getBudget(), latestMetrics.getSpend());
            }
        }
        
        // Check for low CTR
        if (latestMetrics.getCtr() != null && latestMetrics.getCtr() < 1.0) {
            createAlert(campaign, CampaignAlert.AlertType.LOW_CTR,
                      CampaignAlert.AlertSeverity.MEDIUM,
                      "Low click-through rate: " + latestMetrics.getCtr() + "%",
                      1.0, latestMetrics.getCtr());
        }
        
        // Check for high CPC
        if (latestMetrics.getCpc() != null && latestMetrics.getCpc() > 5.0) {
            createAlert(campaign, CampaignAlert.AlertType.HIGH_CPC,
                      CampaignAlert.AlertSeverity.MEDIUM,
                      "High cost per click: $" + latestMetrics.getCpc(),
                      5.0, latestMetrics.getCpc());
        }
        
        // Check for conversion drop (compare with previous metrics)
        if (recentMetrics.size() > 1) {
            CampaignMetrics previousMetrics = recentMetrics.get(1);
            if (latestMetrics.getConversions() != null && previousMetrics.getConversions() != null) {
                double conversionDrop = ((double) (previousMetrics.getConversions() - latestMetrics.getConversions()) 
                                       / previousMetrics.getConversions()) * 100;
                
                if (conversionDrop > 20.0) {
                    createAlert(campaign, CampaignAlert.AlertType.CONVERSION_DROP,
                              CampaignAlert.AlertSeverity.HIGH,
                              "Conversion drop detected: " + String.format("%.1f", conversionDrop) + "%",
                              (double) previousMetrics.getConversions(), (double) latestMetrics.getConversions());
                }
            }
        }
    }
    
    public CampaignAlert createAlert(Campaign campaign, CampaignAlert.AlertType alertType,
                                   CampaignAlert.AlertSeverity severity, String message,
                                   Double thresholdValue, Double actualValue) {
        // Check if similar alert already exists
        List<CampaignAlert> existingAlerts = campaignAlertRepository
                .findActiveCampaignAlertsOrderedBySeverity(campaign);
        
        boolean alertExists = existingAlerts.stream()
                .anyMatch(alert -> alert.getAlertType() == alertType && alert.isActive());
        
        if (alertExists) {
            return null; // Don't create duplicate alerts
        }
        
        CampaignAlert alert = new CampaignAlert(campaign, alertType, severity, message);
        alert.setThresholdValue(thresholdValue);
        alert.setActualValue(actualValue);
        
        alert = campaignAlertRepository.save(alert);
        
        // Send notification
        notificationService.sendCampaignAlert(campaign.getUser(), alert);
        
        logger.warn("Created campaign alert: {} for campaign: {}", message, campaign.getName());
        return alert;
    }
    
    public List<CampaignAlert> getCampaignAlerts(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        return campaignAlertRepository.findActiveCampaignAlertsOrderedBySeverity(campaign);
    }
    
    public void resolveAlert(Long alertId, User user) {
        CampaignAlert alert = campaignAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        
        alert.resolve(user);
        campaignAlertRepository.save(alert);
        
        logger.info("Resolved alert: {} by user: {}", alert.getMessage(), user.getUsername());
    }

    // Batch Operations
    @Async
    public CompletableFuture<Void> batchUpdateCampaignStatus(List<Long> campaignIds, 
                                                           Campaign.CampaignStatus status) {
        for (Long campaignId : campaignIds) {
            try {
                campaignService.updateCampaignStatus(campaignId, status);
                logger.info("Updated campaign {} status to {}", campaignId, status);
            } catch (Exception e) {
                logger.error("Failed to update campaign {} status", campaignId, e);
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Async
    public CompletableFuture<Void> batchMonitorCampaigns(List<Long> campaignIds) {
        for (Long campaignId : campaignIds) {
            try {
                Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
                if (campaign != null) {
                    monitorCampaignPerformance(campaign);
                }
            } catch (Exception e) {
                logger.error("Failed to monitor campaign {}", campaignId, e);
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }

    // Helper methods
    private void activateCampaign(Campaign campaign) {
        campaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        campaignRepository.save(campaign);
        
        // Send notification
        notificationService.sendCampaignStatusUpdate(campaign.getUser(), campaign);
    }
    
    private String processTemplateContent(String templateContent, Map<String, String> parameters) {
        String processed = templateContent;
        
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            processed = processed.replace(placeholder, entry.getValue());
        }
        
        return processed;
    }
    
    private List<CampaignMetrics> getRecentMetrics(Campaign campaign) {
        // This would typically use a repository method to get recent metrics
        // For now, return the campaign's metrics sorted by date
        return campaign.getMetrics().stream()
                .sorted((m1, m2) -> m2.getRecordedAt().compareTo(m1.getRecordedAt()))
                .limit(5)
                .toList();
    }
}