package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.ContentApproval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApprovalReminderService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApprovalReminderService.class);
    
    private final ContentApprovalService contentApprovalService;
    private final ApprovalNotificationService notificationService;
    
    @Autowired
    public ApprovalReminderService(ContentApprovalService contentApprovalService,
                                 ApprovalNotificationService notificationService) {
        this.contentApprovalService = contentApprovalService;
        this.notificationService = notificationService;
    }
    
    /**
     * Send reminders for overdue approvals every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 milliseconds
    public void sendOverdueReminders() {
        logger.info("Starting overdue approval reminder job");
        
        try {
            List<ContentApproval> overdueApprovals = contentApprovalService.getOverdueApprovals();
            
            if (!overdueApprovals.isEmpty()) {
                logger.info("Found {} overdue approvals, sending reminders", overdueApprovals.size());
                notificationService.sendOverdueReminders(overdueApprovals);
            } else {
                logger.debug("No overdue approvals found");
            }
            
        } catch (Exception e) {
            logger.error("Error in overdue approval reminder job", e);
        }
    }
    
    /**
     * Send daily summary of pending approvals at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI") // 9 AM on weekdays
    public void sendDailySummary() {
        logger.info("Starting daily approval summary job");
        
        try {
            List<ContentApproval> requiresAttention = contentApprovalService.getApprovalsRequiringAttention();
            
            if (!requiresAttention.isEmpty()) {
                logger.info("Found {} approvals requiring attention, sending daily summary", 
                           requiresAttention.size());
                // TODO: Implement daily summary notification
                // notificationService.sendDailySummary(requiresAttention);
            }
            
        } catch (Exception e) {
            logger.error("Error in daily approval summary job", e);
        }
    }
}