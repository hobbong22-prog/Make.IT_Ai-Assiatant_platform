package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.service.AsyncTaskManager;
import com.Human.Ai.D.makit.service.JobQueueManager;
import com.Human.Ai.D.makit.service.TaskNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing async AI tasks
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class AsyncTaskController {

    @Autowired
    private AsyncTaskManager asyncTaskManager;

    @Autowired
    private JobQueueManager jobQueueManager;

    @Autowired
    private TaskNotificationService notificationService;

    /**
     * Submit a new AI task
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitTask(@RequestBody TaskSubmissionRequest request) {
        try {
            String jobId = jobQueueManager.submitJob(
                request.getTaskType(),
                request.getParameters(),
                request.getUserId(),
                request.getPriority()
            );

            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "message", "Task submitted successfully",
                "status", "QUEUED"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to submit task",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get task progress
     */
    @GetMapping("/{taskId}/progress")
    public ResponseEntity<?> getTaskProgress(@PathVariable String taskId) {
        try {
            AsyncTaskManager.TaskProgress progress = asyncTaskManager.getTaskProgress(taskId);
            
            if (progress == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Map.of(
                "taskId", progress.getTaskId(),
                "status", progress.getStatus(),
                "progress", progress.getProgressPercentage(),
                "message", progress.getStatusMessage(),
                "createdAt", progress.getCreatedAt(),
                "updatedAt", progress.getUpdatedAt()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get task progress",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get task result
     */
    @GetMapping("/{taskId}/result")
    public ResponseEntity<?> getTaskResult(@PathVariable String taskId) {
        try {
            AsyncTaskManager.TaskResult result = asyncTaskManager.getTaskResult(taskId);
            
            if (result == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Map.of(
                "taskId", taskId,
                "taskType", result.getTaskType(),
                "message", result.getMessage(),
                "data", result.getData(),
                "completedAt", result.getCompletedAt()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get task result",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Cancel a task
     */
    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<?> cancelTask(@PathVariable String taskId) {
        try {
            boolean cancelled = jobQueueManager.cancelJob(taskId);
            
            if (cancelled) {
                return ResponseEntity.ok(Map.of(
                    "taskId", taskId,
                    "message", "Task cancelled successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to cancel task",
                    "message", "Task may not be cancellable or doesn't exist"
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to cancel task",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get job status
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        try {
            JobQueueManager.QueuedJob job = jobQueueManager.getJobStatus(jobId);
            
            if (job == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Map.of(
                "jobId", job.getJobId(),
                "taskType", job.getTaskType(),
                "status", job.getStatus(),
                "priority", job.getPriority(),
                "createdAt", job.getCreatedAt(),
                "userId", job.getUserId()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get job status",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get queue statistics
     */
    @GetMapping("/queue/stats")
    public ResponseEntity<?> getQueueStats() {
        try {
            JobQueueManager.QueueStats stats = jobQueueManager.getQueueStats();
            
            return ResponseEntity.ok(Map.of(
                "queuedJobs", stats.getQueuedJobs(),
                "runningJobs", stats.getRunningJobs(),
                "maxConcurrentJobs", stats.getMaxConcurrentJobs(),
                "availableSlots", stats.getAvailableSlots()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get queue stats",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get user notifications
     */
    @GetMapping("/notifications/{userId}")
    public ResponseEntity<?> getUserNotifications(@PathVariable String userId) {
        try {
            Map<String, Object> notifications = notificationService.getCachedNotifications(userId);
            
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "notifications", notifications
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get notifications",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Clear user notifications
     */
    @DeleteMapping("/notifications/{userId}")
    public ResponseEntity<?> clearUserNotifications(@PathVariable String userId) {
        try {
            notificationService.clearUserNotifications(userId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Notifications cleared successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to clear notifications",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Task submission request DTO
     */
    public static class TaskSubmissionRequest {
        private String taskType;
        private Map<String, Object> parameters;
        private String userId;
        private JobQueueManager.JobPriority priority = JobQueueManager.JobPriority.NORMAL;

        // Getters and setters
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public JobQueueManager.JobPriority getPriority() { return priority; }
        public void setPriority(JobQueueManager.JobPriority priority) { this.priority = priority; }
    }
}