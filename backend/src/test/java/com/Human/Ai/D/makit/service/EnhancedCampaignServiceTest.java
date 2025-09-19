package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnhancedCampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignTemplateRepository campaignTemplateRepository;

    @Mock
    private CampaignScheduleRepository campaignScheduleRepository;

    @Mock
    private CampaignAlertRepository campaignAlertRepository;

    @Mock
    private CampaignService campaignService;

    @Mock
    private WebSocketNotificationService notificationService;

    @InjectMocks
    private EnhancedCampaignService enhancedCampaignService;

    private User testUser;
    private Campaign testCampaign;
    private CampaignTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCampaign = new Campaign();
        testCampaign.setId(1L);
        testCampaign.setName("Test Campaign");
        testCampaign.setType(Campaign.CampaignType.EMAIL);
        testCampaign.setUser(testUser);
        testCampaign.setBudget(1000.0);

        testTemplate = new CampaignTemplate();
        testTemplate.setId(1L);
        testTemplate.setName("Test Template");
        testTemplate.setType(Campaign.CampaignType.EMAIL);
        testTemplate.setTemplateContent("Hello {{name}}, welcome to {{company}}!");
        testTemplate.setDefaultParameters(Map.of("company", "MarKIT"));
    }

    @Test
    void testCreateTemplate() {
        // Given
        Map<String, String> defaultParams = Map.of("company", "MarKIT");
        when(campaignTemplateRepository.save(any(CampaignTemplate.class))).thenReturn(testTemplate);

        // When
        CampaignTemplate result = enhancedCampaignService.createTemplate(
                "Test Template", "Description", Campaign.CampaignType.EMAIL,
                "Template content", defaultParams, testUser);

        // Then
        assertNotNull(result);
        assertEquals("Test Template", result.getName());
        verify(campaignTemplateRepository).save(any(CampaignTemplate.class));
    }

    @Test
    void testCreateCampaignFromTemplate() {
        // Given
        Map<String, String> parameters = Map.of("name", "John");
        when(campaignTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(testCampaign);

        // When
        Campaign result = enhancedCampaignService.createCampaignFromTemplate(
                1L, "New Campaign", parameters, testUser);

        // Then
        assertNotNull(result);
        assertEquals("New Campaign", result.getName());
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    void testScheduleImmediateCampaign() {
        // Given
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
        when(campaignScheduleRepository.save(any(CampaignSchedule.class)))
                .thenReturn(new CampaignSchedule(testCampaign, CampaignSchedule.ScheduleType.IMMEDIATE));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(testCampaign);

        // When
        CampaignSchedule result = enhancedCampaignService.scheduleImmediateCampaign(1L);

        // Then
        assertNotNull(result);
        assertEquals(CampaignSchedule.ScheduleType.IMMEDIATE, result.getScheduleType());
        verify(campaignRepository).save(testCampaign);
        assertEquals(Campaign.CampaignStatus.ACTIVE, testCampaign.getStatus());
    }

    @Test
    void testScheduleDelayedCampaign() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
        
        CampaignSchedule expectedSchedule = new CampaignSchedule(testCampaign, CampaignSchedule.ScheduleType.SCHEDULED);
        expectedSchedule.setScheduledStartTime(startTime);
        expectedSchedule.setScheduledEndTime(endTime);
        
        when(campaignScheduleRepository.save(any(CampaignSchedule.class))).thenReturn(expectedSchedule);

        // When
        CampaignSchedule result = enhancedCampaignService.scheduleDelayedCampaign(1L, startTime, endTime);

        // Then
        assertNotNull(result);
        assertEquals(CampaignSchedule.ScheduleType.SCHEDULED, result.getScheduleType());
        assertEquals(startTime, result.getScheduledStartTime());
        assertEquals(endTime, result.getScheduledEndTime());
    }

    @Test
    void testMonitorCampaignPerformance_BudgetExceeded() {
        // Given
        CampaignMetrics metrics = new CampaignMetrics(testCampaign);
        metrics.setSpend(1500.0); // Exceeds budget of 1000.0
        testCampaign.setMetrics(List.of(metrics));

        CampaignAlert expectedAlert = new CampaignAlert(testCampaign, 
                CampaignAlert.AlertType.BUDGET_EXCEEDED, 
                CampaignAlert.AlertSeverity.HIGH, 
                "Budget exceeded");
        
        when(campaignAlertRepository.findActiveCampaignAlertsOrderedBySeverity(testCampaign))
                .thenReturn(Collections.emptyList());
        when(campaignAlertRepository.save(any(CampaignAlert.class))).thenReturn(expectedAlert);

        // When
        enhancedCampaignService.monitorCampaignPerformance(testCampaign);

        // Then
        verify(campaignAlertRepository).save(any(CampaignAlert.class));
        verify(notificationService).sendCampaignAlert(eq(testUser), any(CampaignAlert.class));
    }

    @Test
    void testMonitorCampaignPerformance_LowCTR() {
        // Given
        CampaignMetrics metrics = new CampaignMetrics(testCampaign);
        metrics.setCtr(0.5); // Low CTR
        testCampaign.setMetrics(List.of(metrics));

        when(campaignAlertRepository.findActiveCampaignAlertsOrderedBySeverity(testCampaign))
                .thenReturn(Collections.emptyList());
        when(campaignAlertRepository.save(any(CampaignAlert.class)))
                .thenReturn(new CampaignAlert(testCampaign, CampaignAlert.AlertType.LOW_CTR, 
                           CampaignAlert.AlertSeverity.MEDIUM, "Low CTR"));

        // When
        enhancedCampaignService.monitorCampaignPerformance(testCampaign);

        // Then
        verify(campaignAlertRepository).save(any(CampaignAlert.class));
    }

    @Test
    void testCreateAlert_DuplicateAlert() {
        // Given
        CampaignAlert existingAlert = new CampaignAlert(testCampaign, 
                CampaignAlert.AlertType.BUDGET_EXCEEDED, 
                CampaignAlert.AlertSeverity.HIGH, 
                "Existing alert");
        existingAlert.setStatus(CampaignAlert.AlertStatus.ACTIVE);

        when(campaignAlertRepository.findActiveCampaignAlertsOrderedBySeverity(testCampaign))
                .thenReturn(List.of(existingAlert));

        // When
        CampaignAlert result = enhancedCampaignService.createAlert(testCampaign,
                CampaignAlert.AlertType.BUDGET_EXCEEDED,
                CampaignAlert.AlertSeverity.HIGH,
                "New alert", 1000.0, 1500.0);

        // Then
        assertNull(result); // Should not create duplicate alert
        verify(campaignAlertRepository, never()).save(any(CampaignAlert.class));
    }

    @Test
    void testResolveAlert() {
        // Given
        CampaignAlert alert = new CampaignAlert(testCampaign, 
                CampaignAlert.AlertType.BUDGET_EXCEEDED, 
                CampaignAlert.AlertSeverity.HIGH, 
                "Test alert");
        alert.setId(1L);

        when(campaignAlertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(campaignAlertRepository.save(any(CampaignAlert.class))).thenReturn(alert);

        // When
        enhancedCampaignService.resolveAlert(1L, testUser);

        // Then
        assertEquals(CampaignAlert.AlertStatus.RESOLVED, alert.getStatus());
        assertEquals(testUser, alert.getResolvedBy());
        assertNotNull(alert.getResolvedAt());
        verify(campaignAlertRepository).save(alert);
    }

    @Test
    void testGetActiveTemplates() {
        // Given
        List<CampaignTemplate> templates = List.of(testTemplate);
        when(campaignTemplateRepository.findByIsActiveTrue()).thenReturn(templates);

        // When
        List<CampaignTemplate> result = enhancedCampaignService.getActiveTemplates();

        // Then
        assertEquals(1, result.size());
        assertEquals(testTemplate, result.get(0));
    }

    @Test
    void testGetTemplatesByType() {
        // Given
        List<CampaignTemplate> templates = List.of(testTemplate);
        when(campaignTemplateRepository.findActiveTemplatesByType(Campaign.CampaignType.EMAIL))
                .thenReturn(templates);

        // When
        List<CampaignTemplate> result = enhancedCampaignService.getTemplatesByType(Campaign.CampaignType.EMAIL);

        // Then
        assertEquals(1, result.size());
        assertEquals(testTemplate, result.get(0));
    }

    @Test
    void testUpdateTemplate() {
        // Given
        Map<String, String> newParams = Map.of("company", "NewCompany");
        when(campaignTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
        when(campaignTemplateRepository.save(any(CampaignTemplate.class))).thenReturn(testTemplate);

        // When
        CampaignTemplate result = enhancedCampaignService.updateTemplate(1L, 
                "Updated Template", "New description", "New content", newParams);

        // Then
        assertNotNull(result);
        verify(campaignTemplateRepository).save(testTemplate);
    }

    @Test
    void testDeactivateTemplate() {
        // Given
        when(campaignTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
        when(campaignTemplateRepository.save(any(CampaignTemplate.class))).thenReturn(testTemplate);

        // When
        enhancedCampaignService.deactivateTemplate(1L);

        // Then
        assertFalse(testTemplate.getIsActive());
        verify(campaignTemplateRepository).save(testTemplate);
    }

    @Test
    void testScheduleRecurringCampaign() {
        // Given
        LocalDateTime endDate = LocalDateTime.now().plusMonths(1);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign));
        
        CampaignSchedule expectedSchedule = new CampaignSchedule(testCampaign, CampaignSchedule.ScheduleType.RECURRING);
        expectedSchedule.setRecurrencePattern(CampaignSchedule.RecurrencePattern.WEEKLY);
        expectedSchedule.setRecurrenceInterval(1);
        expectedSchedule.setRecurrenceEndDate(endDate);
        
        when(campaignScheduleRepository.save(any(CampaignSchedule.class))).thenReturn(expectedSchedule);

        // When
        CampaignSchedule result = enhancedCampaignService.scheduleRecurringCampaign(1L, 
                CampaignSchedule.RecurrencePattern.WEEKLY, 1, endDate);

        // Then
        assertNotNull(result);
        assertEquals(CampaignSchedule.ScheduleType.RECURRING, result.getScheduleType());
        assertEquals(CampaignSchedule.RecurrencePattern.WEEKLY, result.getRecurrencePattern());
        assertEquals(1, result.getRecurrenceInterval());
        assertEquals(endDate, result.getRecurrenceEndDate());
    }
}