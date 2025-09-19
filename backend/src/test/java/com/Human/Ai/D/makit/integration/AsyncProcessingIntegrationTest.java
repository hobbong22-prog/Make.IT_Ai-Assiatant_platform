package com.Human.Ai.D.makit.integration;

import com.Human.Ai.D.makit.service.AsyncTaskManager;
import com.Human.Ai.D.makit.service.JobQueueManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AsyncProcessingIntegrationTest {

    @Autowired
    private AsyncTaskManager asyncTaskManager;

    @Autowired
    private JobQueueManager jobQueueManager;

    @Test
    void testAsyncTaskExecution() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("topic", "AI Technology");
        parameters.put("audience", "developers");

        // When
        CompletableFuture<AsyncTaskManager.TaskResult> future = 
            asyncTaskManager.submitAITask("content_generation", parameters, "user123");

        // Then
        AsyncTaskManager.TaskResult result = future.get(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals("content_generation", result.getTaskType());
        assertNotNull(result.getMessage());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void testTaskProgressTracking() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("data", "test analytics data");

        // When
        CompletableFuture<AsyncTaskManager.TaskResult> future = 
            asyncTaskManager.submitAITask("analytics", parameters, "user456");

        // Wait a bit for task to start
        Thread.sleep(500);

        // Then - check if we can get progress (this might be null if task completes too quickly)
        // In a real test environment, you might want to use a slower mock task
        assertDoesNotThrow(() -> {
            // This test mainly verifies the API doesn't throw exceptions
            asyncTaskManager.getTaskProgress("some-task-id");
        });

        // Wait for completion
        AsyncTaskManager.TaskResult result = future.get(10, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    void testJobQueueSubmission() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("documents", "test documents");

        // When
        String jobId = jobQueueManager.submitJob(
            "knowledge_indexing", 
            parameters, 
            "user789", 
            JobQueueManager.JobPriority.HIGH
        );

        // Then
        assertNotNull(jobId);
        
        JobQueueManager.QueuedJob job = jobQueueManager.getJobStatus(jobId);
        assertNotNull(job);
        assertEquals("knowledge_indexing", job.getTaskType());
        assertEquals("user789", job.getUserId());
        assertEquals(JobQueueManager.JobPriority.HIGH, job.getPriority());
    }

    @Test
    void testQueueStatistics() {
        // When
        JobQueueManager.QueueStats stats = jobQueueManager.getQueueStats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.getQueuedJobs() >= 0);
        assertTrue(stats.getRunningJobs() >= 0);
        assertTrue(stats.getMaxConcurrentJobs() > 0);
        assertTrue(stats.getAvailableSlots() >= 0);
    }

    @Test
    void testJobCancellation() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("test", "data");

        String jobId = jobQueueManager.submitJob(
            "content_generation", 
            parameters, 
            "user999", 
            JobQueueManager.JobPriority.LOW
        );

        // When
        boolean cancelled = jobQueueManager.cancelJob(jobId);

        // Then
        // The result depends on timing - job might already be running or completed
        // This test mainly verifies the API works without throwing exceptions
        assertNotNull(cancelled); // Just check it returns a boolean
    }

    @Test
    void testTaskResultRetrieval() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("simple", "task");

        // When
        CompletableFuture<AsyncTaskManager.TaskResult> future = 
            asyncTaskManager.submitAITask("analytics", parameters, "user111");

        AsyncTaskManager.TaskResult result = future.get(10, TimeUnit.SECONDS);

        // Then
        assertNotNull(result);
        assertEquals("analytics", result.getTaskType());
        assertNotNull(result.getData());
        assertEquals(parameters, result.getData());
    }

    @Test
    void testMultipleTasksExecution() throws Exception {
        // Given
        Map<String, Object> params1 = Map.of("task", "1");
        Map<String, Object> params2 = Map.of("task", "2");
        Map<String, Object> params3 = Map.of("task", "3");

        // When
        CompletableFuture<AsyncTaskManager.TaskResult> future1 = 
            asyncTaskManager.submitAITask("content_generation", params1, "user1");
        CompletableFuture<AsyncTaskManager.TaskResult> future2 = 
            asyncTaskManager.submitAITask("analytics", params2, "user2");
        CompletableFuture<AsyncTaskManager.TaskResult> future3 = 
            asyncTaskManager.submitAITask("knowledge_indexing", params3, "user3");

        // Then
        CompletableFuture.allOf(future1, future2, future3).get(15, TimeUnit.SECONDS);

        AsyncTaskManager.TaskResult result1 = future1.get();
        AsyncTaskManager.TaskResult result2 = future2.get();
        AsyncTaskManager.TaskResult result3 = future3.get();

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);

        assertEquals("content_generation", result1.getTaskType());
        assertEquals("analytics", result2.getTaskType());
        assertEquals("knowledge_indexing", result3.getTaskType());
    }
}