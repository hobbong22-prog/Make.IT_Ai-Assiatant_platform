package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.repository.ContentApprovalRepository;
import com.Human.Ai.D.makit.repository.ContentRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContentApprovalService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentApprovalService.class);
    
    private final ContentApprovalRepository contentApprovalRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final ApprovalNotificationService notificationService;
    
    @Autowired
    public ContentApprovalService(ContentApprovalRepository contentApprovalRepository,
                                 ContentRepository contentRepository,
                                 UserRepository userRepository,
                                 ApprovalNotificationService notificationService) {
        this.contentApprovalRepository = contentApprovalRepository;
        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }
    
    /**
     * Submit content for approval
     */
    public ContentApproval submitForApproval(Long contentId, Long submitterId, 
                                           String submissionNotes, Priority priority) {
        logger.info("Submitting content {} for approval by user {}", contentId, submitterId);
        
        // Validate content exists
        Content content = contentRepository.findById(contentId)
            .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));
        
        // Validate submitter exists
        User submitter = userRepository.findById(submitterId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + submitterId));
        
        // Check if content is already in approval process
        Optional<ContentApproval> existingApproval = contentApprovalRepository.findByContentId(contentId);
        if (existingApproval.isPresent() && existingApproval.get().getStatus().isActive()) {
            throw new IllegalStateException("Content is already in approval process");
        }
        
        // Create new approval
        ContentApproval approval = new ContentApproval(content, submitter, submissionNotes, priority);
        approval = contentApprovalRepository.save(approval);
        
        // Assign reviewer based on priority and availability
        assignReviewer(approval);
        
        // Send notification
        notificationService.notifyApprovalSubmitted(approval);
        
        logger.info("Content approval created with ID: {}", approval.getId());
        return approval;
    }
    
    /**
     * Start review process
     */
    public ContentApproval startReview(Long approvalId, Long reviewerId) {
        logger.info("Starting review for approval {} by reviewer {}", approvalId, reviewerId);
        
        ContentApproval approval = getApprovalById(approvalId);
        User reviewer = getUserById(reviewerId);
        
        // Validate transition
        if (!approval.canTransitionTo(ApprovalStatus.IN_REVIEW)) {
            throw new IllegalStateException("Cannot start review for approval in status: " + approval.getStatus());
        }
        
        // Transition to IN_REVIEW
        approval.transitionTo(ApprovalStatus.IN_REVIEW, reviewer, "Review started");
        approval = contentApprovalRepository.save(approval);
        
        // Send notification
        notificationService.notifyReviewStarted(approval);
        
        return approval;
    }
    
    /**
     * Approve content
     */
    public ContentApproval approveContent(Long approvalId, Long reviewerId, String comments) {
        logger.info("Approving content for approval {} by reviewer {}", approvalId, reviewerId);
        
        ContentApproval approval = getApprovalById(approvalId);
        User reviewer = getUserById(reviewerId);
        
        // Validate reviewer permissions
        validateReviewerPermissions(approval, reviewer);
        
        // Validate transition
        if (!approval.canTransitionTo(ApprovalStatus.APPROVED)) {
            throw new IllegalStateException("Cannot approve content in status: " + approval.getStatus());
        }
        
        // Transition to APPROVED
        approval.transitionTo(ApprovalStatus.APPROVED, reviewer, comments);
        approval = contentApprovalRepository.save(approval);
        
        // Update content status if needed
        updateContentStatus(approval.getContent(), "APPROVED");
        
        // Send notifications
        notificationService.notifyApprovalCompleted(approval, true);
        
        return approval;
    }
    
    /**
     * Reject content
     */
    public ContentApproval rejectContent(Long approvalId, Long reviewerId, String comments) {
        logger.info("Rejecting content for approval {} by reviewer {}", approvalId, reviewerId);
        
        ContentApproval approval = getApprovalById(approvalId);
        User reviewer = getUserById(reviewerId);
        
        // Validate reviewer permissions
        validateReviewerPermissions(approval, reviewer);
        
        // Validate transition
        if (!approval.canTransitionTo(ApprovalStatus.REJECTED)) {
            throw new IllegalStateException("Cannot reject content in status: " + approval.getStatus());
        }
        
        // Transition to REJECTED
        approval.transitionTo(ApprovalStatus.REJECTED, reviewer, comments);
        approval = contentApprovalRepository.save(approval);
        
        // Send notifications
        notificationService.notifyApprovalCompleted(approval, false);
        
        return approval;
    }
    
    /**
     * Request revision
     */
    public ContentApproval requestRevision(Long approvalId, Long reviewerId, String comments) {
        logger.info("Requesting revision for approval {} by reviewer {}", approvalId, reviewerId);
        
        ContentApproval approval = getApprovalById(approvalId);
        User reviewer = getUserById(reviewerId);
        
        // Validate reviewer permissions
        validateReviewerPermissions(approval, reviewer);
        
        // Validate transition
        if (!approval.canTransitionTo(ApprovalStatus.NEEDS_REVISION)) {
            throw new IllegalStateException("Cannot request revision for content in status: " + approval.getStatus());
        }
        
        // Transition to NEEDS_REVISION
        approval.transitionTo(ApprovalStatus.NEEDS_REVISION, reviewer, comments);
        approval = contentApprovalRepository.save(approval);
        
        // Send notifications
        notificationService.notifyRevisionRequested(approval);
        
        return approval;
    }
    
    /**
     * Resubmit after revision
     */
    public ContentApproval resubmitAfterRevision(Long approvalId, Long submitterId, String revisionNotes) {
        logger.info("Resubmitting approval {} after revision by user {}", approvalId, submitterId);
        
        ContentApproval approval = getApprovalById(approvalId);
        User submitter = getUserById(submitterId);
        
        // Validate submitter is the original submitter
        if (!approval.getSubmittedBy().getId().equals(submitterId)) {
            throw new IllegalArgumentException("Only the original submitter can resubmit content");
        }
        
        // Validate transition
        if (!approval.canTransitionTo(ApprovalStatus.PENDING)) {
            throw new IllegalStateException("Cannot resubmit content in status: " + approval.getStatus());
        }
        
        // Update submission notes with revision information
        String updatedNotes = approval.getSubmissionNotes() + "\n\nRevision Notes: " + revisionNotes;
        approval.setSubmissionNotes(updatedNotes);
        
        // Transition back to PENDING
        approval.transitionTo(ApprovalStatus.PENDING, submitter, "Content resubmitted after revision");
        approval = contentApprovalRepository.save(approval);
        
        // Reassign reviewer
        assignReviewer(approval);
        
        // Send notifications
        notificationService.notifyApprovalResubmitted(approval);
        
        return approval;
    }
    
    /**
     * Cancel approval process
     */
    public ContentApproval cancelApproval(Long approvalId, Long userId, String reason) {
        logger.info("Cancelling approval {} by user {}", approvalId, userId);
        
        ContentApproval approval = getApprovalById(approvalId);
        User user = getUserById(userId);
        
        // Validate user can cancel (submitter or admin)
        if (!approval.getSubmittedBy().getId().equals(userId) && !isAdmin(user)) {
            throw new IllegalArgumentException("Only the submitter or admin can cancel approval");
        }
        
        // Validate transition
        if (!approval.canTransitionTo(ApprovalStatus.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel approval in status: " + approval.getStatus());
        }
        
        // Transition to CANCELLED
        approval.transitionTo(ApprovalStatus.CANCELLED, user, reason);
        approval = contentApprovalRepository.save(approval);
        
        // Send notifications
        notificationService.notifyApprovalCancelled(approval);
        
        return approval;
    }
    
    /**
     * Get approvals for reviewer
     */
    @Transactional(readOnly = true)
    public Page<ContentApproval> getApprovalsForReviewer(Long reviewerId, Pageable pageable) {
        User reviewer = getUserById(reviewerId);
        List<ApprovalStatus> activeStatuses = Arrays.asList(ApprovalStatus.PENDING, ApprovalStatus.IN_REVIEW);
        return contentApprovalRepository.findByCurrentReviewerAndStatusIn(reviewer, activeStatuses, pageable);
    }
    
    /**
     * Get approvals by submitter
     */
    @Transactional(readOnly = true)
    public Page<ContentApproval> getApprovalsBySubmitter(Long submitterId, Pageable pageable) {
        User submitter = getUserById(submitterId);
        return contentApprovalRepository.findBySubmittedBy(submitter, pageable);
    }
    
    /**
     * Get overdue approvals
     */
    @Transactional(readOnly = true)
    public List<ContentApproval> getOverdueApprovals() {
        List<ApprovalStatus> activeStatuses = Arrays.asList(ApprovalStatus.PENDING, ApprovalStatus.IN_REVIEW);
        return contentApprovalRepository.findOverdueApprovals(LocalDateTime.now(), activeStatuses);
    }
    
    /**
     * Get approvals requiring attention
     */
    @Transactional(readOnly = true)
    public List<ContentApproval> getApprovalsRequiringAttention() {
        List<ApprovalStatus> activeStatuses = Arrays.asList(ApprovalStatus.PENDING, ApprovalStatus.IN_REVIEW);
        return contentApprovalRepository.findApprovalsRequiringAttention(
            LocalDateTime.now(), activeStatuses, Priority.HIGH, ApprovalStatus.PENDING);
    }
    
    /**
     * Get approval statistics
     */
    @Transactional(readOnly = true)
    public ApprovalStatistics getApprovalStatistics() {
        List<Object[]> statusStats = contentApprovalRepository.getApprovalStatusStatistics();
        List<ApprovalStatus> activeStatuses = Arrays.asList(ApprovalStatus.PENDING, ApprovalStatus.IN_REVIEW, ApprovalStatus.NEEDS_REVISION);
        List<Object[]> priorityStats = contentApprovalRepository.getActivePriorityStatistics(activeStatuses);
        
        return new ApprovalStatistics(statusStats, priorityStats);
    }
    
    // Private helper methods
    private ContentApproval getApprovalById(Long approvalId) {
        return contentApprovalRepository.findById(approvalId)
            .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));
    }
    
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
    
    private void validateReviewerPermissions(ContentApproval approval, User reviewer) {
        // Check if user has reviewer role or is admin
        if (!hasReviewerRole(reviewer)) {
            throw new IllegalArgumentException("User does not have reviewer permissions");
        }
        
        // Check if reviewer is assigned to this approval
        if (approval.getCurrentReviewer() != null && 
            !approval.getCurrentReviewer().getId().equals(reviewer.getId())) {
            throw new IllegalArgumentException("User is not assigned as reviewer for this approval");
        }
    }
    
    private void assignReviewer(ContentApproval approval) {
        // Simple assignment logic - in real implementation, this could be more sophisticated
        // For now, we'll leave it unassigned and let admins assign manually
        // or implement round-robin assignment based on workload
        logger.info("Reviewer assignment needed for approval {}", approval.getId());
    }
    
    private void updateContentStatus(Content content, String status) {
        // Update content status if the Content entity has a status field
        // This would depend on the actual Content entity structure
        logger.info("Updating content {} status to {}", content.getId(), status);
    }
    
    private boolean hasReviewerRole(User user) {
        // Check if user has ADMIN or MARKETING_MANAGER role
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MARKETING_MANAGER;
    }
    
    private boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }
}