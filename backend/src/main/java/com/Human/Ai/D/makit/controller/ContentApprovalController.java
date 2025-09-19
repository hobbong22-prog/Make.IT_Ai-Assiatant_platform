package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.ContentApproval;
import com.Human.Ai.D.makit.dto.*;
import com.Human.Ai.D.makit.service.ApprovalStatistics;
import com.Human.Ai.D.makit.service.ContentApprovalService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/content-approvals")
@CrossOrigin(origins = "*")
public class ContentApprovalController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentApprovalController.class);
    
    private final ContentApprovalService contentApprovalService;
    
    @Autowired
    public ContentApprovalController(ContentApprovalService contentApprovalService) {
        this.contentApprovalService = contentApprovalService;
    }
    
    /**
     * Submit content for approval
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('CONTENT_CREATOR') or hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContentApprovalResponse> submitForApproval(
            @Valid @RequestBody ContentApprovalRequest request,
            Authentication authentication) {
        
        try {
            logger.info("Submitting content {} for approval by user {}", 
                       request.getContentId(), authentication.getName());
            
            Long userId = getUserIdFromAuthentication(authentication);
            
            ContentApproval approval = contentApprovalService.submitForApproval(
                request.getContentId(),
                userId,
                request.getSubmissionNotes(),
                request.getPriority()
            );
            
            ContentApprovalResponse response = convertToResponse(approval);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for content approval submission", e);
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid state for content approval submission", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error submitting content for approval", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Start review process
     */
    @PostMapping("/{approvalId}/start-review")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContentApprovalResponse> startReview(
            @PathVariable Long approvalId,
            Authentication authentication) {
        
        try {
            logger.info("Starting review for approval {} by user {}", approvalId, authentication.getName());
            
            Long userId = getUserIdFromAuthentication(authentication);
            
            ContentApproval approval = contentApprovalService.startReview(approvalId, userId);
            ContentApprovalResponse response = convertToResponse(approval);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for starting review", e);
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid state for starting review", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error starting review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Approve content
     */
    @PostMapping("/{approvalId}/approve")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContentApprovalResponse> approveContent(
            @PathVariable Long approvalId,
            @Valid @RequestBody ApprovalActionRequest request,
            Authentication authentication) {
        
        try {
            logger.info("Approving content for approval {} by user {}", approvalId, authentication.getName());
            
            Long userId = getUserIdFromAuthentication(authentication);
            
            ContentApproval approval = contentApprovalService.approveContent(
                approvalId, userId, request.getComments());
            ContentApprovalResponse response = convertToResponse(approval);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for content approval", e);
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid state for content approval", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error approving content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Reject content
     */
    @PostMapping("/{approvalId}/reject")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContentApprovalResponse> rejectContent(
            @PathVariable Long approvalId,
            @Valid @RequestBody ApprovalActionRequest request,
            Authentication authentication) {
        
        try {
            logger.info("Rejecting content for approval {} by user {}", approvalId, authentication.getName());
            
            Long userId = getUserIdFromAuthentication(authentication);
            
            ContentApproval approval = contentApprovalService.rejectContent(
                approvalId, userId, request.getComments());
            ContentApprovalResponse response = convertToResponse(approval);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for content rejection", e);
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid state for content rejection", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error rejecting content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Request revision
     */
    @PostMapping("/{approvalId}/request-revision")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContentApprovalResponse> requestRevision(
            @PathVariable Long approvalId,
            @Valid @RequestBody ApprovalActionRequest request,
            Authentication authentication) {
        
        try {
            logger.info("Requesting revision for approval {} by user {}", approvalId, authentication.getName());
            
            Long userId = getUserIdFromAuthentication(authentication);
            
            ContentApproval approval = contentApprovalService.requestRevision(
                approvalId, userId, request.getComments());
            ContentApprovalResponse response = convertToResponse(approval);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for revision request", e);
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid state for revision request", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error requesting revision", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Resubmit after revision
     */
    @PostMapping("/{approvalId}/resubmit")
    @PreAuthorize("hasRole('CONTENT_CREATOR') or hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContentApprovalResponse> resubmitAfterRevision(
            @PathVariable Long approvalId,
            @RequestBody String revisionNotes,
            Authentication authentication) {
        
        try {
            logger.info("Resubmitting approval {} after revision by user {}", 
                       approvalId, authentication.getName());
            
            Long userId = getUserIdFromAuthentication(authentication);
            
            ContentApproval approval = contentApprovalService.resubmitAfterRevision(
                approvalId, userId, revisionNotes);
            ContentApprovalResponse response = convertToResponse(approval);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for resubmission", e);
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid state for resubmission", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error resubmitting content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Cancel approval
     */
    @PostMapping("/{approvalId}/cancel")
    @PreAuthorize("hasRole('CONTENT_CREATOR') or hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContentApprovalResponse> cancelApproval(
            @PathVariable Long approvalId,
            @RequestBody String reason,
            Authentication authentication) {
        
        try {
            logger.info("Cancelling approval {} by user {}", approvalId, authentication.getName());
            
            Long userId = getUserIdFromAuthentication(authentication);
            
            ContentApproval approval = contentApprovalService.cancelApproval(approvalId, userId, reason);
            ContentApprovalResponse response = convertToResponse(approval);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for approval cancellation", e);
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid state for approval cancellation", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error cancelling approval", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get approvals for current user as reviewer
     */
    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Page<ContentApprovalResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? 
                               Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<ContentApproval> approvals = contentApprovalService.getApprovalsForReviewer(userId, pageable);
            Page<ContentApprovalResponse> responses = approvals.map(this::convertToResponse);
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error retrieving reviews for user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get approvals submitted by current user
     */
    @GetMapping("/my-submissions")
    public ResponseEntity<Page<ContentApprovalResponse>> getMySubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? 
                               Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<ContentApproval> approvals = contentApprovalService.getApprovalsBySubmitter(userId, pageable);
            Page<ContentApprovalResponse> responses = approvals.map(this::convertToResponse);
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error retrieving submissions for user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get overdue approvals
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ContentApprovalResponse>> getOverdueApprovals() {
        
        try {
            List<ContentApproval> overdueApprovals = contentApprovalService.getOverdueApprovals();
            List<ContentApprovalResponse> responses = overdueApprovals.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error retrieving overdue approvals", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get approvals requiring attention
     */
    @GetMapping("/requiring-attention")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ContentApprovalResponse>> getApprovalsRequiringAttention() {
        
        try {
            List<ContentApproval> approvals = contentApprovalService.getApprovalsRequiringAttention();
            List<ContentApprovalResponse> responses = approvals.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error retrieving approvals requiring attention", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get approval statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApprovalStatistics> getApprovalStatistics() {
        
        try {
            ApprovalStatistics statistics = contentApprovalService.getApprovalStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error retrieving approval statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Helper methods
    private Long getUserIdFromAuthentication(Authentication authentication) {
        // In a real implementation, this would extract the user ID from the JWT token
        // For now, we'll use a placeholder implementation
        return 1L; // TODO: Implement proper user ID extraction
    }
    
    private ContentApprovalResponse convertToResponse(ContentApproval approval) {
        ContentApprovalResponse response = new ContentApprovalResponse();
        response.setId(approval.getId());
        response.setContentId(approval.getContent().getId());
        response.setContentTitle(approval.getContent().getTitle());
        response.setSubmittedByUsername(approval.getSubmittedBy().getUsername());
        
        if (approval.getCurrentReviewer() != null) {
            response.setCurrentReviewerUsername(approval.getCurrentReviewer().getUsername());
        }
        
        response.setStatus(approval.getStatus());
        response.setCurrentStep(approval.getCurrentStep());
        response.setSubmissionNotes(approval.getSubmissionNotes());
        response.setReviewerComments(approval.getReviewerComments());
        response.setSubmittedAt(approval.getSubmittedAt());
        response.setReviewedAt(approval.getReviewedAt());
        response.setDueDate(approval.getDueDate());
        response.setPriority(approval.getPriority());
        response.setOverdue(approval.isOverdue());
        response.setRequiresAttention(approval.requiresAttention());
        
        // Convert history
        List<ApprovalHistoryResponse> historyResponses = approval.getApprovalHistory().stream()
            .map(history -> new ApprovalHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getUsername() : null,
                history.getComments(),
                history.getChangedAt()
            ))
            .collect(Collectors.toList());
        
        response.setHistory(historyResponses);
        
        return response;
    }
}