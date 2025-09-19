package com.Human.Ai.D.makit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Job queue manager for AI task scheduling and execution
 */
@Service
public class JobQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(JobQueueManager.class);

    @Autowired
    private AsyncTaskManager asyncTaskManager;

    @Autowired
    private CacheService cacheService;

    // Thread pool for AI tasks
    private final ExecutorService aiTaskExecutor = Executors.newFixedThreadPool(5);
    
    // Priority queue for tasks
    private final PriorityBlockingQueue<QueuedJob> jobQueue = new PriorityBlockingQueue<>();
    
    // Active jobs tracking
    private final Map<String, Future<?>> activeJobs = new ConcurrentHashMap<>();

    /**
     * Submit a job to the queue with priority
     */
    public String submitJob(String taskType, Map<String, Object> parameters, String userId, JobPriority priority) {
        String jobId = UUID.randomUUID().toString();
        
        QueuedJob job = new QueuedJob(jobId, taskType, parameters, userId, priority);
        jobQueue.offer(job);
        
        logger.info("Queued job: {} with priority: {} for user: {}", jobId, priority, userId);
        
        // Cache job info
        cacheService.put("queuedJobs", jobId, job);
        
        return jobId;
    }

    /**
     * Process jobs from the queue
     */
    @Scheduled(fixedDelay = 1000) // Check every second
    public void processJobQueue() {
        while (!jobQueue.isEmpty() && activeJobs.size() < 5) { // Max 5 concurrent jobs
            QueuedJob job = jobQueue.poll();
            if (job != null) {
                executeJob(job);
            }
        }
    }

    /**
     * Execute a job asynchronously
     */
    private void executeJob(QueuedJob job) {
        Future<?> future = aiTaskExecutor.submit(() -> {
            try {
                logger.info("Executing job: {}", job.getJobId());
                
                CompletableFuture<AsyncTaskManager.TaskResult> taskFuture = 
                    asyncTaskManager.submitAITask(job.getTaskType(), job.getParameters(), job.getUserId());
                
                AsyncTaskManager.TaskResult result = taskFuture.get(30, TimeUnit.MINUTES); // 30 min timeout
                
                job.setStatus(JobStatus.COMPLETED);
                job.setResult(result);
                
                logger.info("Completed job: {}", job.getJobId());
                
            } catch (TimeoutException e) {
                job.setStatus(JobStatus.TIMEOUT);
                logger.error("Job timed out: {}", job.getJobId());
            } catch (Exception e) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                logger.error("Job failed: {}", job.getJobId(), e);
            } finally {
                activeJobs.remove(job.getJobId());
                // Update cache with final status
                cacheService.put("queuedJobs", job.getJobId(), job);
            }
        });
        
        activeJobs.put(job.getJobId(), future);
        job.setStatus(JobStatus.RUNNING);
    }

    /**
     * Get job status
     */
    public QueuedJob getJobStatus(String jobId) {
        // Check cache first
        QueuedJob job = cacheService.get("queuedJobs", jobId, QueuedJob.class);
        if (job != null) {
            return job;
        }
        
        // Check if it's in the queue
        return jobQueue.stream()
                .filter(j -> j.getJobId().equals(jobId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Cancel a job
     */
    public boolean cancelJob(String jobId) {
        // Try to cancel if it's running
        Future<?> future = activeJobs.get(jobId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                activeJobs.remove(jobId);
                logger.info("Cancelled running job: {}", jobId);
                return true;
            }
        }
        
        // Try to remove from queue
        boolean removed = jobQueue.removeIf(job -> job.getJobId().equals(jobId));
        if (removed) {
            logger.info("Removed queued job: {}", jobId);
            return true;
        }
        
        return false;
    }

    /**
     * Get queue statistics
     */
    public QueueStats getQueueStats() {
        return new QueueStats(
            jobQueue.size(),
            activeJobs.size(),
            5 // max concurrent jobs
        );
    }

    /**
     * Clean up old completed jobs
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldJobs() {
        // Clean up jobs older than 24 hours
        String pattern = "queuedJobs::*";
        // This would need implementation to check job age and remove old ones
        logger.info("Cleaning up old jobs");
    }

    /**
     * Queued job class
     */
    public static class QueuedJob implements Comparable<QueuedJob> {
        private final String jobId;
        private final String taskType;
        private final Map<String, Object> parameters;
        private final String userId;
        private final JobPriority priority;
        private final LocalDateTime createdAt;
        private JobStatus status;
        private String errorMessage;
        private AsyncTaskManager.TaskResult result;

        public QueuedJob(String jobId, String taskType, Map<String, Object> parameters, 
                        String userId, JobPriority priority) {
            this.jobId = jobId;
            this.taskType = taskType;
            this.parameters = parameters;
            this.userId = userId;
            this.priority = priority;
            this.createdAt = LocalDateTime.now();
            this.status = JobStatus.QUEUED;
        }

        @Override
        public int compareTo(QueuedJob other) {
            // Higher priority first, then by creation time
            int priorityComparison = other.priority.compareTo(this.priority);
            if (priorityComparison != 0) {
                return priorityComparison;
            }
            return this.createdAt.compareTo(other.createdAt);
        }

        // Getters and setters
        public String getJobId() { return jobId; }
        public String getTaskType() { return taskType; }
        public Map<String, Object> getParameters() { return parameters; }
        public String getUserId() { return userId; }
        public JobPriority getPriority() { return priority; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public JobStatus getStatus() { return status; }
        public void setStatus(JobStatus status) { this.status = status; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public AsyncTaskManager.TaskResult getResult() { return result; }
        public void setResult(AsyncTaskManager.TaskResult result) { this.result = result; }
    }

    /**
     * Job priority enumeration
     */
    public enum JobPriority {
        LOW(1), NORMAL(2), HIGH(3), URGENT(4);
        
        private final int value;
        
        JobPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }

    /**
     * Job status enumeration
     */
    public enum JobStatus {
        QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT
    }

    /**
     * Queue statistics class
     */
    public static class QueueStats {
        private final int queuedJobs;
        private final int runningJobs;
        private final int maxConcurrentJobs;

        public QueueStats(int queuedJobs, int runningJobs, int maxConcurrentJobs) {
            this.queuedJobs = queuedJobs;
            this.runningJobs = runningJobs;
            this.maxConcurrentJobs = maxConcurrentJobs;
        }

        public int getQueuedJobs() { return queuedJobs; }
        public int getRunningJobs() { return runningJobs; }
        public int getMaxConcurrentJobs() { return maxConcurrentJobs; }
        public int getAvailableSlots() { return maxConcurrentJobs - runningJobs; }
    }
}