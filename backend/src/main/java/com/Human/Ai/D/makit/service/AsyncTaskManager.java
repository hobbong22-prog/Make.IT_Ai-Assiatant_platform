package com.Human.Ai.D.makit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced async task manager for AI operations with progress tracking
 */
@Service
public class AsyncTaskManager {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskManager.class);

    @Autowired
    private CacheService cacheService;

    // In-memory task tracking (in production, consider using Redis or database)
    private final Map<String, TaskProgress> taskProgressMap = new ConcurrentHashMap<>();

    /**
     * Submit an async AI task with progress tracking
     */
    @Async
    public CompletableFuture<TaskResult> submitAITask(String taskType, Map<String, Object> parameters, String userId) {
        String taskId = UUID.randomUUID().toString();
        
        TaskProgress progress = new TaskProgress(taskId, taskType, userId);
        taskProgressMap.put(taskId, progress);
        
        logger.info("Starting AI task: {} for user: {}", taskId, userId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                progress.updateStatus(TaskStatus.PROCESSING);
                progress.updateProgress(10, "Initializing AI task");
                
                // Cache task progress
                cacheService.put("taskProgress", taskId, progress);
                
                // Simulate AI processing with progress updates
                TaskResult result = processAITask(taskType, parameters, progress);
                
                progress.updateStatus(TaskStatus.COMPLETED);
                progress.updateProgress(100, "Task completed successfully");
                progress.setResult(result);
                
                // Cache final result
                cacheService.put("taskResults", taskId, result);
                
                logger.info("Completed AI task: {}", taskId);
                return result;
                
            } catch (Exception e) {
                progress.updateStatus(TaskStatus.FAILED);
                progress.setErrorMessage(e.getMessage());
                
                logger.error("Failed AI task: {}", taskId, e);
                throw new RuntimeException("AI task failed: " + taskId, e);
            }
        });
    }

    /**
     * Get task progress by ID
     */
    public TaskProgress getTaskProgress(String taskId) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            // Try to get from cache
            progress = cacheService.get("taskProgress", taskId, TaskProgress.class);
        }
        return progress;
    }

    /**
     * Get task result by ID
     */
    public TaskResult getTaskResult(String taskId) {
        TaskProgress progress = getTaskProgress(taskId);
        if (progress != null && progress.getResult() != null) {
            return progress.getResult();
        }
        
        // Try to get from cache
        return cacheService.get("taskResults", taskId, TaskResult.class);
    }

    /**
     * Cancel a running task
     */
    public boolean cancelTask(String taskId) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress != null && progress.getStatus() == TaskStatus.PROCESSING) {
            progress.updateStatus(TaskStatus.CANCELLED);
            logger.info("Cancelled AI task: {}", taskId);
            return true;
        }
        return false;
    }

    /**
     * Process AI task with progress tracking
     */
    private TaskResult processAITask(String taskType, Map<String, Object> parameters, TaskProgress progress) {
        switch (taskType.toLowerCase()) {
            case "content_generation":
                return processContentGeneration(parameters, progress);
            case "analytics":
                return processAnalytics(parameters, progress);
            case "knowledge_indexing":
                return processKnowledgeIndexing(parameters, progress);
            default:
                throw new IllegalArgumentException("Unknown task type: " + taskType);
        }
    }

    /**
     * Process content generation task
     */
    private TaskResult processContentGeneration(Map<String, Object> parameters, TaskProgress progress) {
        try {
            progress.updateProgress(20, "Preparing content generation");
            Thread.sleep(1000); // Simulate processing time
            
            progress.updateProgress(50, "Generating content with AI");
            Thread.sleep(2000);
            
            progress.updateProgress(80, "Finalizing content");
            Thread.sleep(500);
            
            return new TaskResult("content_generation", "Generated content successfully", parameters);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Content generation interrupted", e);
        }
    }

    /**
     * Process analytics task
     */
    private TaskResult processAnalytics(Map<String, Object> parameters, TaskProgress progress) {
        try {
            progress.updateProgress(25, "Collecting analytics data");
            Thread.sleep(800);
            
            progress.updateProgress(60, "Processing with AI models");
            Thread.sleep(1500);
            
            progress.updateProgress(90, "Generating insights");
            Thread.sleep(700);
            
            return new TaskResult("analytics", "Analytics processed successfully", parameters);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Analytics processing interrupted", e);
        }
    }

    /**
     * Process knowledge indexing task
     */
    private TaskResult processKnowledgeIndexing(Map<String, Object> parameters, TaskProgress progress) {
        try {
            progress.updateProgress(15, "Preparing documents for indexing");
            Thread.sleep(600);
            
            progress.updateProgress(40, "Generating embeddings");
            Thread.sleep(2500);
            
            progress.updateProgress(75, "Storing in knowledge base");
            Thread.sleep(1000);
            
            progress.updateProgress(95, "Updating search index");
            Thread.sleep(300);
            
            return new TaskResult("knowledge_indexing", "Knowledge indexing completed", parameters);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Knowledge indexing interrupted", e);
        }
    }

    /**
     * Clean up completed tasks older than specified time
     */
    public void cleanupOldTasks(int hoursOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursOld);
        
        taskProgressMap.entrySet().removeIf(entry -> {
            TaskProgress progress = entry.getValue();
            return progress.getCreatedAt().isBefore(cutoff) && 
                   (progress.getStatus() == TaskStatus.COMPLETED || 
                    progress.getStatus() == TaskStatus.FAILED ||
                    progress.getStatus() == TaskStatus.CANCELLED);
        });
        
        logger.info("Cleaned up old tasks older than {} hours", hoursOld);
    }

    /**
     * Task progress tracking class
     */
    public static class TaskProgress {
        private final String taskId;
        private final String taskType;
        private final String userId;
        private final LocalDateTime createdAt;
        private TaskStatus status;
        private int progressPercentage;
        private String statusMessage;
        private String errorMessage;
        private TaskResult result;
        private LocalDateTime updatedAt;

        public TaskProgress(String taskId, String taskType, String userId) {
            this.taskId = taskId;
            this.taskType = taskType;
            this.userId = userId;
            this.createdAt = LocalDateTime.now();
            this.status = TaskStatus.QUEUED;
            this.progressPercentage = 0;
            this.statusMessage = "Task queued";
            this.updatedAt = LocalDateTime.now();
        }

        public void updateStatus(TaskStatus status) {
            this.status = status;
            this.updatedAt = LocalDateTime.now();
        }

        public void updateProgress(int percentage, String message) {
            this.progressPercentage = percentage;
            this.statusMessage = message;
            this.updatedAt = LocalDateTime.now();
        }

        // Getters and setters
        public String getTaskId() { return taskId; }
        public String getTaskType() { return taskType; }
        public String getUserId() { return userId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public TaskStatus getStatus() { return status; }
        public int getProgressPercentage() { return progressPercentage; }
        public String getStatusMessage() { return statusMessage; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public TaskResult getResult() { return result; }
        public void setResult(TaskResult result) { this.result = result; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    /**
     * Task result class
     */
    public static class TaskResult {
        private final String taskType;
        private final String message;
        private final Map<String, Object> data;
        private final LocalDateTime completedAt;

        public TaskResult(String taskType, String message, Map<String, Object> data) {
            this.taskType = taskType;
            this.message = message;
            this.data = data;
            this.completedAt = LocalDateTime.now();
        }

        // Getters
        public String getTaskType() { return taskType; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
        public LocalDateTime getCompletedAt() { return completedAt; }
    }

    /**
     * Task status enumeration
     */
    public enum TaskStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
}