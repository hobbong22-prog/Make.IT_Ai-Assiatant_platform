package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.dto.CampaignCreateRequest;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.EnhancedCampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns/enhanced")
@CrossOrigin(origins = "*")
public class EnhancedCampaignController {
    
    @Autowired
    private EnhancedCampaignService enhancedCampaignService;
    
    @Autowired
    private UserRepository userRepository;

    // Campaign Template Management
    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<CampaignTemplate> createTemplate(
            @RequestBody CreateTemplateRequest request) {
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CampaignTemplate template = enhancedCampaignService.createTemplate(
                request.getName(),
                request.getDescription(),
                request.getType(),
                request.getTemplateContent(),
                request.getDefaultParameters(),
                user
        );
        
        return ResponseEntity.ok(template);
    }
    
    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<List<CampaignTemplate>> getActiveTemplates() {
        List<CampaignTemplate> templates = enhancedCampaignService.getActiveTemplates();
        return ResponseEntity.ok(templates);
    }
    
    @GetMapping("/templates/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<List<CampaignTemplate>> getTemplatesByType(@PathVariable Campaign.CampaignType type) {
        List<CampaignTemplate> templates = enhancedCampaignService.getTemplatesByType(type);
        return ResponseEntity.ok(templates);
    }
    
    @PostMapping("/templates/{templateId}/campaigns")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<Campaign> createCampaignFromTemplate(
            @PathVariable Long templateId,
            @RequestBody CreateFromTemplateRequest request) {
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Campaign campaign = enhancedCampaignService.createCampaignFromTemplate(
                templateId,
                request.getCampaignName(),
                request.getParameters(),
                user
        );
        
        return ResponseEntity.ok(campaign);
    }
    
    @PutMapping("/templates/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<CampaignTemplate> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody UpdateTemplateRequest request) {
        
        CampaignTemplate template = enhancedCampaignService.updateTemplate(
                templateId,
                request.getName(),
                request.getDescription(),
                request.getTemplateContent(),
                request.getDefaultParameters()
        );
        
        return ResponseEntity.ok(template);
    }
    
    @DeleteMapping("/templates/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateTemplate(@PathVariable Long templateId) {
        enhancedCampaignService.deactivateTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    // Campaign Scheduling
    @PostMapping("/{campaignId}/schedule/immediate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<CampaignSchedule> scheduleImmediateCampaign(@PathVariable Long campaignId) {
        CampaignSchedule schedule = enhancedCampaignService.scheduleImmediateCampaign(campaignId);
        return ResponseEntity.ok(schedule);
    }
    
    @PostMapping("/{campaignId}/schedule/delayed")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<CampaignSchedule> scheduleDelayedCampaign(
            @PathVariable Long campaignId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        CampaignSchedule schedule = enhancedCampaignService.scheduleDelayedCampaign(
                campaignId, startTime, endTime);
        return ResponseEntity.ok(schedule);
    }
    
    @PostMapping("/{campaignId}/schedule/recurring")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<CampaignSchedule> scheduleRecurringCampaign(
            @PathVariable Long campaignId,
            @RequestParam CampaignSchedule.RecurrencePattern pattern,
            @RequestParam Integer interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        CampaignSchedule schedule = enhancedCampaignService.scheduleRecurringCampaign(
                campaignId, pattern, interval, endDate);
        return ResponseEntity.ok(schedule);
    }

    // Campaign Monitoring and Alerts
    @GetMapping("/{campaignId}/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'ANALYST')")
    public ResponseEntity<List<CampaignAlert>> getCampaignAlerts(@PathVariable Long campaignId) {
        List<CampaignAlert> alerts = enhancedCampaignService.getCampaignAlerts(campaignId);
        return ResponseEntity.ok(alerts);
    }
    
    @PostMapping("/alerts/{alertId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<Void> resolveAlert(
            @PathVariable Long alertId,
            @RequestParam Long userId) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        enhancedCampaignService.resolveAlert(alertId, user);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{campaignId}/monitor")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<Void> monitorCampaign(@PathVariable Long campaignId) {
        // This would typically be called automatically, but can be triggered manually
        return ResponseEntity.ok().build();
    }

    // Batch Operations
    @PostMapping("/batch/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<Void> batchUpdateStatus(@RequestBody BatchStatusUpdateRequest request) {
        enhancedCampaignService.batchUpdateCampaignStatus(request.getCampaignIds(), request.getStatus());
        return ResponseEntity.accepted().build();
    }
    
    @PostMapping("/batch/monitor")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<Void> batchMonitorCampaigns(@RequestBody BatchMonitorRequest request) {
        enhancedCampaignService.batchMonitorCampaigns(request.getCampaignIds());
        return ResponseEntity.accepted().build();
    }

    // DTOs for request bodies
    public static class CreateTemplateRequest {
        private String name;
        private String description;
        private Campaign.CampaignType type;
        private String templateContent;
        private Map<String, String> defaultParameters;
        private Long userId;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Campaign.CampaignType getType() { return type; }
        public void setType(Campaign.CampaignType type) { this.type = type; }
        public String getTemplateContent() { return templateContent; }
        public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }
        public Map<String, String> getDefaultParameters() { return defaultParameters; }
        public void setDefaultParameters(Map<String, String> defaultParameters) { this.defaultParameters = defaultParameters; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class CreateFromTemplateRequest {
        private String campaignName;
        private Map<String, String> parameters;
        private Long userId;

        // Getters and setters
        public String getCampaignName() { return campaignName; }
        public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
        public Map<String, String> getParameters() { return parameters; }
        public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class UpdateTemplateRequest {
        private String name;
        private String description;
        private String templateContent;
        private Map<String, String> defaultParameters;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTemplateContent() { return templateContent; }
        public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }
        public Map<String, String> getDefaultParameters() { return defaultParameters; }
        public void setDefaultParameters(Map<String, String> defaultParameters) { this.defaultParameters = defaultParameters; }
    }

    public static class BatchStatusUpdateRequest {
        private List<Long> campaignIds;
        private Campaign.CampaignStatus status;

        // Getters and setters
        public List<Long> getCampaignIds() { return campaignIds; }
        public void setCampaignIds(List<Long> campaignIds) { this.campaignIds = campaignIds; }
        public Campaign.CampaignStatus getStatus() { return status; }
        public void setStatus(Campaign.CampaignStatus status) { this.status = status; }
    }

    public static class BatchMonitorRequest {
        private List<Long> campaignIds;

        // Getters and setters
        public List<Long> getCampaignIds() { return campaignIds; }
        public void setCampaignIds(List<Long> campaignIds) { this.campaignIds = campaignIds; }
    }
}