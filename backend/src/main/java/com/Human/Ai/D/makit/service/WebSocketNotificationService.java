package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAlert;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WebSocketNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    
    @Autowired
    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate,
                                      UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }
    
    /**
     * Send notification to a specific user
     */
    public void sendToUser(Long userId, ApprovalNotification notification) {
        try {
            String destination = "/queue/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, notification);
            logger.info("Sent WebSocket notification to user {}: {}", userId, notification.getType());
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification to user {}", userId, e);
        }
    }
    
    /**
     * Send notification to all reviewers (admins and marketing managers)
     */
    public void sendToReviewers(ApprovalNotification notification) {
        try {
            List<Long> reviewerIds = userRepository.findByUserRoleIn(
                List.of(UserRole.ADMIN, UserRole.MARKETING_MANAGER))
                .stream()
                .map(user -> user.getId())
                .toList();
            
            for (Long reviewerId : reviewerIds) {
                sendToUser(reviewerId, notification);
            }
            
            logger.info("Sent WebSocket notification to {} reviewers: {}", 
                       reviewerIds.size(), notification.getType());
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification to reviewers", e);
        }
    }
    
    /**
     * Send notification to all users with specific roles
     */
    public void sendToRoles(List<UserRole> roles, ApprovalNotification notification) {
        try {
            List<Long> userIds = userRepository.findByUserRoleIn(roles)
                .stream()
                .map(user -> user.getId())
                .toList();
            
            for (Long userId : userIds) {
                sendToUser(userId, notification);
            }
            
            logger.info("Sent WebSocket notification to {} users with roles {}: {}", 
                       userIds.size(), roles, notification.getType());
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification to roles {}", roles, e);
        }
    }
    
    /**
     * Broadcast notification to all connected users
     */
    public void broadcast(ApprovalNotification notification) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            logger.info("Broadcasted WebSocket notification: {}", notification.getType());
        } catch (Exception e) {
            logger.error("Failed to broadcast WebSocket notification", e);
        }
    }
    
    /**
     * Send campaign alert notification to user
     */
    public void sendCampaignAlert(User user, CampaignAlert alert) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "CAMPAIGN_ALERT",
                "alertType", alert.getAlertType().name(),
                "severity", alert.getSeverity().name(),
                "message", alert.getMessage(),
                "campaignId", alert.getCampaign().getId(),
                "campaignName", alert.getCampaign().getName(),
                "timestamp", alert.getTriggeredAt()
            );
            
            String destination = "/queue/notifications/" + user.getId();
            messagingTemplate.convertAndSend(destination, notification);
            logger.info("Sent campaign alert notification to user {}: {}", user.getId(), alert.getAlertType());
        } catch (Exception e) {
            logger.error("Failed to send campaign alert notification to user {}", user.getId(), e);
        }
    }
    
    /**
     * Send campaign status update notification to user
     */
    public void sendCampaignStatusUpdate(User user, Campaign campaign) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "CAMPAIGN_STATUS_UPDATE",
                "campaignId", campaign.getId(),
                "campaignName", campaign.getName(),
                "status", campaign.getStatus().name(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            String destination = "/queue/notifications/" + user.getId();
            messagingTemplate.convertAndSend(destination, notification);
            logger.info("Sent campaign status update notification to user {}: {} -> {}", 
                       user.getId(), campaign.getName(), campaign.getStatus());
        } catch (Exception e) {
            logger.error("Failed to send campaign status update notification to user {}", user.getId(), e);
        }
    }
}