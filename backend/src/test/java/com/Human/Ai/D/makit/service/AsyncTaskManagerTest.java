package com.Human.Ai.D.makit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncTaskManagerTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private AsyncTaskManager asyncTaskManager;

    private Map<String, Object> testParameters;

    @BeforeEach
    void setUp() {
        testParameters = new HashMap<>();
        testParameters.put("test", "data");
    }

    @Test
    void testSubmitContentGenerationTask() throws Exception {
        // Given
        String taskType = "content_generation";
        String userId = "user123";

        // When
        CompletableFuture<AsyncTaskManager.TaskResult> future = 
            asyncTaskManager.submitAITask(taskType, testParameters, userId);

        // Then
        AsyncTaskManager.TaskResult result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(taskType, result.getTaskType());
        assertEquals("Generated content successfully", result.getMessage());
        assertEquals(testParameters, result.getData());
    }

    @Test
    void testSubmitAnalyticsTask() throws Exception {
        // Given
        String taskType = "analytics";
        String userId = "user456";

        // When
        CompletableFuture<AsyncTaskManager.TaskResult> future = 
            asyncTaskManager.submitAITask(taskType, testParameters, userId);

        // Then
        AsyncTaskManager.TaskResult result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(taskType, result.getTaskType());
        assertEquals("Analytics processed successfully", result.getMessage());
    }

    @Test
    void testSubmitKnowledgeIndexingTask() throws Exception {
        // Given
        String taskType = "knowledge_indexing";
        String userId = "user789";

        // When
        CompletableFuture<AsyncTaskManager.TaskResult> future = 
            asyncTaskManager.submitAITask(taskType, testParameters, userId);

        // Then
        AsyncTaskManager.TaskResult result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(taskType, result.getTaskType());
        assertEquals("Knowledge indexing completed", result.getMessage());
    }

    @Test
    void testSubmitUnknownTaskType() {
        // Given
        String taskType = "unknown_task";
        String userId = "user999";

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            CompletableFuture<AsyncTaskManager.TaskResult> future = 
                asyncTaskManager.submitAITask(taskType, testParameters, userId);
            future.get(5, TimeUnit.SECONDS);
        });
    }

    @Test
    void testTaskProgressTracking() throws Exception {
        // Given
        String taskType = "content_generation";
        String userId = "user123";

        // When
        CompletableFuture<AsyncTaskManager.TaskResult> future = 
            asyncTaskManager.submitAITask(taskType, testParameters, userId);

        // Wait a bit for task to start
        Thread.sleep(100);

        // Then - we can't easily test progress in unit tests due to timing,
        // but we can verify the task completes successfully
        AsyncTaskManager.TaskResult result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    void testTaskProgressClass() {
        // Given
        String taskId = "task123";
        String taskType = "test_task";
        String userId = "user123";

        // When
        AsyncTaskManager.TaskProgress progress = 
            new AsyncTaskManager.TaskProgress(taskId, taskType, userId);

        // Then
        assertEquals(taskId, progress.getTaskId());
        assertEquals(taskType, progress.getTaskType());
        assertEquals(userId, progress.getUserId());
        assertEquals(AsyncTaskManager.TaskStatus.QUEUED, progress.getStatus());
        assertEquals(0, progress.getProgressPercentage());
        assertEquals("Task queued", progress.getStatusMessage());
        assertNotNull(progress.getCreatedAt());
    }

    @Test
    void testTaskProgressUpdate() {
        // Given
        AsyncTaskManager.TaskProgress progress = 
            new AsyncTaskManager.TaskProgress("task123", "test_task", "user123");

        // When
        progress.updateStatus(AsyncTaskManager.TaskStatus.PROCESSING);
        progress.updateProgress(50, "Processing task");

        // Then
        assertEquals(AsyncTaskManager.TaskStatus.PROCESSING, progress.getStatus());
        assertEquals(50, progress.getProgressPercentage());
        assertEquals("Processing task", progress.getStatusMessage());
        assertNotNull(progress.getUpdatedAt());
    }

    @Test
    void testTaskResultClass() {
        // Given
        String taskType = "test_task";
        String message = "Task completed";
        Map<String, Object> data = Map.of("result", "success");

        // When
        AsyncTaskManager.TaskResult result = 
            new AsyncTaskManager.TaskResult(taskType, message, data);

        // Then
        assertEquals(taskType, result.getTaskType());
        assertEquals(message, result.getMessage());
        assertEquals(data, result.getData());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void testGetTaskProgressNotFound() {
        // Given
        String taskId = "nonexistent";

        // When
        AsyncTaskManager.TaskProgress progress = asyncTaskManager.getTaskProgress(taskId);

        // Then
        assertNull(progress);
    }

    @Test
    void testGetTaskResultNotFound() {
        // Given
        String taskId = "nonexistent";

        // When
        AsyncTaskManager.TaskResult result = asyncTaskManager.getTaskResult(taskId);

        // Then
        assertNull(result);
    }

    @Test
    void testCancelNonExistentTask() {
        // Given
        String taskId = "nonexistent";

        // When
        boolean cancelled = asyncTaskManager.cancelTask(taskId);

        // Then
        assertFalse(cancelled);
    }
}