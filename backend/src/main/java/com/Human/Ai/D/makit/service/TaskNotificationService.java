package com.Human.Ai.D.makit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending task notifications to users via WebSocket
 */
@Service
public class TaskNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(TaskNotificationService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CacheService cacheService;

    /**
     * Send task progress notification to user
     */
    public void sendTaskProgressNotification(String userId, String taskId, 
                                           AsyncTaskManager.TaskProgress progress) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "TASK_PROGRESS");
            notification.put("taskId", taskId);
            notification.put("status", progress.getStatus());
            notification.put("progress", progress.getProgressPercentage());
            notification.put("message", progress.getStatusMessage());
            notification.put("timestamp", LocalDateTime.now());

            // Send to specific user
            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/task-notifications", 
                notification
            );

            logger.debug("Sent task progress notification to user: {} for task: {}", userId, taskId);

        } catch (Exception e) {
            logger.error("Failed to send task progress notification to user: {} for task: {}", 
                        userId, taskId, e);
        }
    }

    /**
     * Send task completion notification
     */
    public void sendTaskCompletionNotification(String userId, String taskId, 
                                             AsyncTaskManager.TaskResult result) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "TASK_COMPLETED");
            notification.put("taskId", taskId);
            notification.put("taskType", result.getTaskType());
            notification.put("message", result.getMessage());
            notification.put("completedAt", result.getCompletedAt());
            notification.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/task-notifications", 
                notification
            );

            // Also cache the notification for later retrieval
            cacheService.put("notifications", userId + "_" + taskId, notification);

            logger.info("Sent task completion notification to user: {} for task: {}", userId, taskId);

        } catch (Exception e) {
            logger.error("Failed to send task completion notification to user: {} for task: {}", 
                        userId, taskId, e);
        }
    }

    /**
     * Send task failure notification
     */
    public void sendTaskFailureNotification(String userId, String taskId, String errorMessage) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "TASK_FAILED");
            notification.put("taskId", taskId);
            notification.put("error", errorMessage);
            notification.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/task-notifications", 
                notification
            );

            // Cache the error notification
            cacheService.put("notifications", userId + "_" + taskId + "_error", notification);

            logger.warn("Sent task failure notification to user: {} for task: {}", userId, taskId);

        } catch (Exception e) {
            logger.error("Failed to send task failure notification to user: {} for task: {}", 
                        userId, taskId, e);
        }
    }

    /**
     * Send job queue status notification
     */
    public void sendQueueStatusNotification(String userId, JobQueueManager.QueueStats stats) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "QUEUE_STATUS");
            notification.put("queuedJobs", stats.getQueuedJobs());
            notification.put("runningJobs", stats.getRunningJobs());
            notification.put("availableSlots", stats.getAvailableSlots());
            notification.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/system-notifications", 
                notification
            );

            logger.debug("Sent queue status notification to user: {}", userId);

        } catch (Exception e) {
            logger.error("Failed to send queue status notification to user: {}", userId, e);
        }
    }

    /**
     * Send system alert notification
     */
    public void sendSystemAlert(String userId, String alertType, String message, String severity) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "SYSTEM_ALERT");
            notification.put("alertType", alertType);
            notification.put("message", message);
            notification.put("severity", severity);
            notification.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/system-notifications", 
                notification
            );

            // Cache important alerts
            if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
                cacheService.put("alerts", userId + "_" + System.currentTimeMillis(), notification);
            }

            logger.info("Sent system alert to user: {} - {}: {}", userId, alertType, message);

        } catch (Exception e) {
            logger.error("Failed to send system alert to user: {}", userId, e);
        }
    }

    /**
     * Broadcast notification to all connected users
     */
    public void broadcastNotification(String type, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("message", message);
            notification.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/broadcasts", notification);

            logger.info("Broadcasted notification: {} - {}", type, message);

        } catch (Exception e) {
            logger.error("Failed to broadcast notification: {} - {}", type, message, e);
        }
    }

    /**
     * Get cached notifications for a user
     */
    public Map<String, Object> getCachedNotifications(String userId) {
        Map<String, Object> notifications = new HashMap<>();
        
        try {
            // Get task notifications
            var taskNotifications = cacheService.getKeys("notifications::" + userId + "*");
            for (String key : taskNotifications) {
                String notificationKey = key.replace("notifications::", "");
                Object notification = cacheService.get("notifications", notificationKey, Object.class);
                if (notification != null) {
                    notifications.put(notificationKey, notification);
                }
            }

            // Get alerts
            var alerts = cacheService.getKeys("alerts::" + userId + "*");
            for (String key : alerts) {
                String alertKey = key.replace("alerts::", "");
                Object alert = cacheService.get("alerts", alertKey, Object.class);
                if (alert != null) {
                    notifications.put(alertKey, alert);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to get cached notifications for user: {}", userId, e);
        }

        return notifications;
    }

    /**
     * Clear notifications for a user
     */
    public void clearUserNotifications(String userId) {
        try {
            cacheService.invalidateByPattern("notifications::" + userId + "*");
            cacheService.invalidateByPattern("alerts::" + userId + "*");
            
            logger.info("Cleared notifications for user: {}", userId);

        } catch (Exception e) {
            logger.error("Failed to clear notifications for user: {}", userId, e);
        }
    }
}