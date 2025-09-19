package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.repository.ContentApprovalRepository;
import com.Human.Ai.D.makit.repository.ContentRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentApprovalServiceTest {
    
    @Mock
    private ContentApprovalRepository contentApprovalRepository;
    
    @Mock
    private ContentRepository contentRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ApprovalNotificationService notificationService;
    
    @InjectMocks
    private ContentApprovalService contentApprovalService;
    
    private User contentCreator;
    private User marketingManager;
    private Content testContent;
    private ContentApproval testApproval;
    
    @BeforeEach
    void setUp() {
        contentCreator = new User();
        contentCreator.setId(1L);
        contentCreator.setUsername("creator");
        contentCreator.setEmail("creator@test.com");
        contentCreator.setRole(UserRole.CONTENT_CREATOR);
        contentCreator.setActive(true);
        
        marketingManager = new User();
        marketingManager.setId(2L);
        marketingManager.setUsername("manager");
        marketingManager.setEmail("manager@test.com");
        marketingManager.setRole(UserRole.MARKETING_MANAGER);
        marketingManager.setActive(true);
        
        testContent = new Content();
        testContent.setId(1L);
        testContent.setTitle("Test Content");
        testContent.setContent("Test content for approval");
        testContent.setCreatedBy(contentCreator);
        
        testApproval = new ContentApproval(testContent, contentCreator, "Test submission", Priority.MEDIUM);
        testApproval.setId(1L);
    }
    
    @Test
    void testSubmitForApproval_Success() {
        // Arrange
        when(contentRepository.findById(1L)).thenReturn(Optional.of(testContent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(contentCreator));
        when(contentApprovalRepository.findByContentId(1L)).thenReturn(Optional.empty());
        when(contentApprovalRepository.save(any(ContentApproval.class))).thenReturn(testApproval);
        
        // Act
        ContentApproval result = contentApprovalService.submitForApproval(
            1L, 1L, "Test submission", Priority.MEDIUM);
        
        // Assert
        assertNotNull(result);
        assertEquals(ApprovalStatus.PENDING, result.getStatus());
        assertEquals(Priority.MEDIUM, result.getPriority());
        assertEquals("Test submission", result.getSubmissionNotes());
        
        verify(contentApprovalRepository).save(any(ContentApproval.class));
        verify(notificationService).notifyApprovalSubmitted(any(ContentApproval.class));
    }
    
    @Test
    void testSubmitForApproval_ContentNotFound() {
        // Arrange
        when(contentRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            contentApprovalService.submitForApproval(1L, 1L, "Test", Priority.MEDIUM));
    }
    
    @Test
    void testSubmitForApproval_ContentAlreadyInApproval() {
        // Arrange
        ContentApproval existingApproval = new ContentApproval(testContent, contentCreator, "Existing", Priority.LOW);
        existingApproval.setStatus(ApprovalStatus.PENDING);
        
        when(contentRepository.findById(1L)).thenReturn(Optional.of(testContent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(contentCreator));
        when(contentApprovalRepository.findByContentId(1L)).thenReturn(Optional.of(existingApproval));
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            contentApprovalService.submitForApproval(1L, 1L, "Test", Priority.MEDIUM));
    }
    
    @Test
    void testStartReview_Success() {
        // Arrange
        when(contentApprovalRepository.findById(1L)).thenReturn(Optional.of(testApproval));
        when(userRepository.findById(2L)).thenReturn(Optional.of(marketingManager));
        when(contentApprovalRepository.save(any(ContentApproval.class))).thenReturn(testApproval);
        
        // Act
        ContentApproval result = contentApprovalService.startReview(1L, 2L);
        
        // Assert
        assertNotNull(result);
        verify(contentApprovalRepository).save(any(ContentApproval.class));
        verify(notificationService).notifyReviewStarted(any(ContentApproval.class));
    }
    
    @Test
    void testApproveContent_Success() {
        // Arrange
        testApproval.setStatus(ApprovalStatus.IN_REVIEW);
        testApproval.setCurrentReviewer(marketingManager);
        
        when(contentApprovalRepository.findById(1L)).thenReturn(Optional.of(testApproval));
        when(userRepository.findById(2L)).thenReturn(Optional.of(marketingManager));
        when(contentApprovalRepository.save(any(ContentApproval.class))).thenReturn(testApproval);
        
        // Act
        ContentApproval result = contentApprovalService.approveContent(1L, 2L, "Approved!");
        
        // Assert
        assertNotNull(result);
        verify(contentApprovalRepository).save(any(ContentApproval.class));
        verify(notificationService).notifyApprovalCompleted(any(ContentApproval.class), eq(true));
    }
    
    @Test
    void testRejectContent_Success() {
        // Arrange
        testApproval.setStatus(ApprovalStatus.IN_REVIEW);
        testApproval.setCurrentReviewer(marketingManager);
        
        when(contentApprovalRepository.findById(1L)).thenReturn(Optional.of(testApproval));
        when(userRepository.findById(2L)).thenReturn(Optional.of(marketingManager));
        when(contentApprovalRepository.save(any(ContentApproval.class))).thenReturn(testApproval);
        
        // Act
        ContentApproval result = contentApprovalService.rejectContent(1L, 2L, "Needs improvement");
        
        // Assert
        assertNotNull(result);
        verify(contentApprovalRepository).save(any(ContentApproval.class));
        verify(notificationService).notifyApprovalCompleted(any(ContentApproval.class), eq(false));
    }
    
    @Test
    void testRequestRevision_Success() {
        // Arrange
        testApproval.setStatus(ApprovalStatus.IN_REVIEW);
        testApproval.setCurrentReviewer(marketingManager);
        
        when(contentApprovalRepository.findById(1L)).thenReturn(Optional.of(testApproval));
        when(userRepository.findById(2L)).thenReturn(Optional.of(marketingManager));
        when(contentApprovalRepository.save(any(ContentApproval.class))).thenReturn(testApproval);
        
        // Act
        ContentApproval result = contentApprovalService.requestRevision(1L, 2L, "Please revise");
        
        // Assert
        assertNotNull(result);
        verify(contentApprovalRepository).save(any(ContentApproval.class));
        verify(notificationService).notifyRevisionRequested(any(ContentApproval.class));
    }
    
    @Test
    void testResubmitAfterRevision_Success() {
        // Arrange
        testApproval.setStatus(ApprovalStatus.NEEDS_REVISION);
        
        when(contentApprovalRepository.findById(1L)).thenReturn(Optional.of(testApproval));
        when(userRepository.findById(1L)).thenReturn(Optional.of(contentCreator));
        when(contentApprovalRepository.save(any(ContentApproval.class))).thenReturn(testApproval);
        
        // Act
        ContentApproval result = contentApprovalService.resubmitAfterRevision(1L, 1L, "Revised as requested");
        
        // Assert
        assertNotNull(result);
        verify(contentApprovalRepository).save(any(ContentApproval.class));
        verify(notificationService).notifyApprovalResubmitted(any(ContentApproval.class));
    }
    
    @Test
    void testResubmitAfterRevision_WrongSubmitter() {
        // Arrange
        testApproval.setStatus(ApprovalStatus.NEEDS_REVISION);
        
        when(contentApprovalRepository.findById(1L)).thenReturn(Optional.of(testApproval));
        when(userRepository.findById(2L)).thenReturn(Optional.of(marketingManager));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            contentApprovalService.resubmitAfterRevision(1L, 2L, "Revised"));
    }
    
    @Test
    void testCancelApproval_Success() {
        // Arrange
        when(contentApprovalRepository.findById(1L)).thenReturn(Optional.of(testApproval));
        when(userRepository.findById(1L)).thenReturn(Optional.of(contentCreator));
        when(contentApprovalRepository.save(any(ContentApproval.class))).thenReturn(testApproval);
        
        // Act
        ContentApproval result = contentApprovalService.cancelApproval(1L, 1L, "No longer needed");
        
        // Assert
        assertNotNull(result);
        verify(contentApprovalRepository).save(any(ContentApproval.class));
        verify(notificationService).notifyApprovalCancelled(any(ContentApproval.class));
    }
    
    @Test
    void testGetApprovalsForReviewer() {
        // Arrange
        List<ContentApproval> approvals = Arrays.asList(testApproval);
        Page<ContentApproval> page = new PageImpl<>(approvals);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(userRepository.findById(2L)).thenReturn(Optional.of(marketingManager));
        when(contentApprovalRepository.findByCurrentReviewerAndStatusIn(
            eq(marketingManager), anyList(), eq(pageable))).thenReturn(page);
        
        // Act
        Page<ContentApproval> result = contentApprovalService.getApprovalsForReviewer(2L, pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testApproval, result.getContent().get(0));
    }
    
    @Test
    void testGetApprovalsBySubmitter() {
        // Arrange
        List<ContentApproval> approvals = Arrays.asList(testApproval);
        Page<ContentApproval> page = new PageImpl<>(approvals);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(contentCreator));
        when(contentApprovalRepository.findBySubmittedBy(contentCreator, pageable)).thenReturn(page);
        
        // Act
        Page<ContentApproval> result = contentApprovalService.getApprovalsBySubmitter(1L, pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testApproval, result.getContent().get(0));
    }
    
    @Test
    void testGetOverdueApprovals() {
        // Arrange
        ContentApproval overdueApproval = new ContentApproval(testContent, contentCreator, "Overdue", Priority.HIGH);
        overdueApproval.setDueDate(LocalDateTime.now().minusDays(1));
        List<ContentApproval> overdueApprovals = Arrays.asList(overdueApproval);
        
        when(contentApprovalRepository.findOverdueApprovals(any(LocalDateTime.class), anyList()))
            .thenReturn(overdueApprovals);
        
        // Act
        List<ContentApproval> result = contentApprovalService.getOverdueApprovals();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(overdueApproval, result.get(0));
    }
    
    @Test
    void testGetApprovalsRequiringAttention() {
        // Arrange
        List<ContentApproval> attentionApprovals = Arrays.asList(testApproval);
        
        when(contentApprovalRepository.findApprovalsRequiringAttention(
            any(LocalDateTime.class), anyList(), eq(Priority.HIGH), eq(ApprovalStatus.PENDING)))
            .thenReturn(attentionApprovals);
        
        // Act
        List<ContentApproval> result = contentApprovalService.getApprovalsRequiringAttention();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testApproval, result.get(0));
    }
    
    @Test
    void testGetApprovalStatistics() {
        // Arrange
        List<Object[]> statusStats = Arrays.asList(
            new Object[]{ApprovalStatus.PENDING, 5L},
            new Object[]{ApprovalStatus.APPROVED, 10L}
        );
        List<Object[]> priorityStats = Arrays.asList(
            new Object[]{Priority.HIGH, 3L},
            new Object[]{Priority.MEDIUM, 7L}
        );
        
        when(contentApprovalRepository.getApprovalStatusStatistics()).thenReturn(statusStats);
        when(contentApprovalRepository.getActivePriorityStatistics(anyList())).thenReturn(priorityStats);
        
        // Act
        ApprovalStatistics result = contentApprovalService.getApprovalStatistics();
        
        // Assert
        assertNotNull(result);
        assertEquals(15L, result.getTotalApprovals());
        assertEquals(5L, result.getPendingCount());
        assertEquals(10L, result.getApprovedCount());
    }
}