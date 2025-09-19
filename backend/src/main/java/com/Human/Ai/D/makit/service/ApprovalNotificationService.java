package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.ContentApproval;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApprovalNotificationService.class);
    
    private final UserRepository userRepository;
    private final EmailNotificationService emailService;
    private final WebSocketNotificationService webSocketService;
    
    @Autowired
    public ApprovalNotificationService(UserRepository userRepository,
                                     EmailNotificationService emailService,
                                     WebSocketNotificationService webSocketService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.webSocketService = webSocketService;
    }
    
    /**
     * Notify when content is submitted for approval
     */
    public void notifyApprovalSubmitted(ContentApproval approval) {
        logger.info("Sending approval submission notifications for approval {}", approval.getId());
        
        try {
            // Notify reviewers (admins and marketing managers)
            List<User> reviewers = getAvailableReviewers();
            
            String subject = String.format("New Content Approval Request - Priority: %s", 
                                         approval.getPriority().getDisplayName());
            String message = buildApprovalSubmittedMessage(approval);
            
            // Send email notifications
            for (User reviewer : reviewers) {
                emailService.sendNotification(reviewer.getEmail(), subject, message);
            }
            
            // Send real-time notifications
            ApprovalNotification notification = new ApprovalNotification(
                "APPROVAL_SUBMITTED",
                approval.getId(),
                approval.getContent().getTitle(),
                approval.getSubmittedBy().getUsername(),
                approval.getPriority().getDisplayName(),
                message
            );
            
            webSocketService.sendToReviewers(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send approval submission notifications", e);
        }
    }
    
    /**
     * Notify when review is started
     */
    public void notifyReviewStarted(ContentApproval approval) {
        logger.info("Sending review started notifications for approval {}", approval.getId());
        
        try {
            String subject = "Content Review Started";
            String message = buildReviewStartedMessage(approval);
            
            // Notify submitter
            emailService.sendNotification(approval.getSubmittedBy().getEmail(), subject, message);
            
            // Send real-time notification
            ApprovalNotification notification = new ApprovalNotification(
                "REVIEW_STARTED",
                approval.getId(),
                approval.getContent().getTitle(),
                approval.getCurrentReviewer().getUsername(),
                null,
                message
            );
            
            webSocketService.sendToUser(approval.getSubmittedBy().getId(), notification);
            
        } catch (Exception e) {
            logger.error("Failed to send review started notifications", e);
        }
    }
    
    /**
     * Notify when approval is completed (approved or rejected)
     */
    public void notifyApprovalCompleted(ContentApproval approval, boolean approved) {
        logger.info("Sending approval completion notifications for approval {} - approved: {}", 
                   approval.getId(), approved);
        
        try {
            String subject = approved ? "Content Approved" : "Content Rejected";
            String message = buildApprovalCompletedMessage(approval, approved);
            
            // Notify submitter
            emailService.sendNotification(approval.getSubmittedBy().getEmail(), subject, message);
            
            // Send real-time notification
            ApprovalNotification notification = new ApprovalNotification(
                approved ? "CONTENT_APPROVED" : "CONTENT_REJECTED",
                approval.getId(),
                approval.getContent().getTitle(),
                approval.getCurrentReviewer().getUsername(),
                null,
                message
            );
            
            webSocketService.sendToUser(approval.getSubmittedBy().getId(), notification);
            
            // If approved, notify relevant team members
            if (approved) {
                notifyTeamOfApproval(approval);
            }
            
        } catch (Exception e) {
            logger.error("Failed to send approval completion notifications", e);
        }
    }
    
    /**
     * Notify when revision is requested
     */
    public void notifyRevisionRequested(ContentApproval approval) {
        logger.info("Sending revision request notifications for approval {}", approval.getId());
        
        try {
            String subject = "Content Revision Required";
            String message = buildRevisionRequestedMessage(approval);
            
            // Notify submitter
            emailService.sendNotification(approval.getSubmittedBy().getEmail(), subject, message);
            
            // Send real-time notification
            ApprovalNotification notification = new ApprovalNotification(
                "REVISION_REQUESTED",
                approval.getId(),
                approval.getContent().getTitle(),
                approval.getCurrentReviewer().getUsername(),
                null,
                message
            );
            
            webSocketService.sendToUser(approval.getSubmittedBy().getId(), notification);
            
        } catch (Exception e) {
            logger.error("Failed to send revision request notifications", e);
        }
    }
    
    /**
     * Notify when content is resubmitted after revision
     */
    public void notifyApprovalResubmitted(ContentApproval approval) {
        logger.info("Sending resubmission notifications for approval {}", approval.getId());
        
        try {
            String subject = "Content Resubmitted After Revision";
            String message = buildResubmissionMessage(approval);
            
            // Notify reviewers
            List<User> reviewers = getAvailableReviewers();
            for (User reviewer : reviewers) {
                emailService.sendNotification(reviewer.getEmail(), subject, message);
            }
            
            // Send real-time notification
            ApprovalNotification notification = new ApprovalNotification(
                "CONTENT_RESUBMITTED",
                approval.getId(),
                approval.getContent().getTitle(),
                approval.getSubmittedBy().getUsername(),
                null,
                message
            );
            
            webSocketService.sendToReviewers(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send resubmission notifications", e);
        }
    }
    
    /**
     * Notify when approval is cancelled
     */
    public void notifyApprovalCancelled(ContentApproval approval) {
        logger.info("Sending cancellation notifications for approval {}", approval.getId());
        
        try {
            String subject = "Content Approval Cancelled";
            String message = buildCancellationMessage(approval);
            
            // Notify current reviewer if assigned
            if (approval.getCurrentReviewer() != null) {
                emailService.sendNotification(approval.getCurrentReviewer().getEmail(), subject, message);
                
                ApprovalNotification notification = new ApprovalNotification(
                    "APPROVAL_CANCELLED",
                    approval.getId(),
                    approval.getContent().getTitle(),
                    approval.getSubmittedBy().getUsername(),
                    null,
                    message
                );
                
                webSocketService.sendToUser(approval.getCurrentReviewer().getId(), notification);
            }
            
        } catch (Exception e) {
            logger.error("Failed to send cancellation notifications", e);
        }
    }
    
    /**
     * Send overdue approval reminders
     */
    public void sendOverdueReminders(List<ContentApproval> overdueApprovals) {
        logger.info("Sending overdue reminders for {} approvals", overdueApprovals.size());
        
        for (ContentApproval approval : overdueApprovals) {
            try {
                String subject = "URGENT: Overdue Content Approval";
                String message = buildOverdueReminderMessage(approval);
                
                // Send to current reviewer if assigned
                if (approval.getCurrentReviewer() != null) {
                    emailService.sendNotification(approval.getCurrentReviewer().getEmail(), subject, message);
                } else {
                    // Send to all reviewers if no specific reviewer assigned
                    List<User> reviewers = getAvailableReviewers();
                    for (User reviewer : reviewers) {
                        emailService.sendNotification(reviewer.getEmail(), subject, message);
                    }
                }
                
                // Send real-time notification
                ApprovalNotification notification = new ApprovalNotification(
                    "APPROVAL_OVERDUE",
                    approval.getId(),
                    approval.getContent().getTitle(),
                    null,
                    approval.getPriority().getDisplayName(),
                    message
                );
                
                if (approval.getCurrentReviewer() != null) {
                    webSocketService.sendToUser(approval.getCurrentReviewer().getId(), notification);
                } else {
                    webSocketService.sendToReviewers(notification);
                }
                
            } catch (Exception e) {
                logger.error("Failed to send overdue reminder for approval {}", approval.getId(), e);
            }
        }
    }
    
    // Private helper methods
    private List<User> getAvailableReviewers() {
        return userRepository.findByUserRoleIn(List.of(UserRole.ADMIN, UserRole.MARKETING_MANAGER))
                           .stream()
                           .filter(User::isActive)
                           .collect(Collectors.toList());
    }
    
    private void notifyTeamOfApproval(ContentApproval approval) {
        // Notify content creators and other relevant team members
        List<User> teamMembers = userRepository.findByUserRoleIn(
            List.of(UserRole.CONTENT_CREATOR, UserRole.MARKETING_MANAGER));
        
        String subject = "Content Approved and Ready for Publication";
        String message = buildTeamApprovalMessage(approval);
        
        for (User member : teamMembers) {
            if (!member.getId().equals(approval.getSubmittedBy().getId())) {
                emailService.sendNotification(member.getEmail(), subject, message);
            }
        }
    }
    
    // Message building methods
    private String buildApprovalSubmittedMessage(ContentApproval approval) {
        return String.format(
            "A new content approval request has been submitted:\n\n" +
            "Content: %s\n" +
            "Submitted by: %s\n" +
            "Priority: %s\n" +
            "Due Date: %s\n" +
            "Submission Notes: %s\n\n" +
            "Please review and take appropriate action.",
            approval.getContent().getTitle(),
            approval.getSubmittedBy().getUsername(),
            approval.getPriority().getDisplayName(),
            approval.getDueDate(),
            approval.getSubmissionNotes()
        );
    }
    
    private String buildReviewStartedMessage(ContentApproval approval) {
        return String.format(
            "Review has started for your content:\n\n" +
            "Content: %s\n" +
            "Reviewer: %s\n" +
            "Status: In Review\n\n" +
            "You will be notified once the review is complete.",
            approval.getContent().getTitle(),
            approval.getCurrentReviewer().getUsername()
        );
    }
    
    private String buildApprovalCompletedMessage(ContentApproval approval, boolean approved) {
        String status = approved ? "APPROVED" : "REJECTED";
        return String.format(
            "Your content has been %s:\n\n" +
            "Content: %s\n" +
            "Reviewer: %s\n" +
            "Status: %s\n" +
            "Comments: %s\n\n" +
            "%s",
            status.toLowerCase(),
            approval.getContent().getTitle(),
            approval.getCurrentReviewer().getUsername(),
            status,
            approval.getReviewerComments(),
            approved ? "Your content is now ready for publication." : 
                      "Please review the comments and make necessary changes."
        );
    }
    
    private String buildRevisionRequestedMessage(ContentApproval approval) {
        return String.format(
            "Revision has been requested for your content:\n\n" +
            "Content: %s\n" +
            "Reviewer: %s\n" +
            "Comments: %s\n\n" +
            "Please make the requested changes and resubmit for approval.",
            approval.getContent().getTitle(),
            approval.getCurrentReviewer().getUsername(),
            approval.getReviewerComments()
        );
    }
    
    private String buildResubmissionMessage(ContentApproval approval) {
        return String.format(
            "Content has been resubmitted after revision:\n\n" +
            "Content: %s\n" +
            "Submitted by: %s\n" +
            "Priority: %s\n\n" +
            "Please review the updated content.",
            approval.getContent().getTitle(),
            approval.getSubmittedBy().getUsername(),
            approval.getPriority().getDisplayName()
        );
    }
    
    private String buildCancellationMessage(ContentApproval approval) {
        return String.format(
            "Content approval has been cancelled:\n\n" +
            "Content: %s\n" +
            "Submitted by: %s\n" +
            "Reason: %s\n\n" +
            "No further action is required.",
            approval.getContent().getTitle(),
            approval.getSubmittedBy().getUsername(),
            approval.getReviewerComments()
        );
    }
    
    private String buildOverdueReminderMessage(ContentApproval approval) {
        return String.format(
            "URGENT: Content approval is overdue:\n\n" +
            "Content: %s\n" +
            "Submitted by: %s\n" +
            "Priority: %s\n" +
            "Due Date: %s\n" +
            "Days Overdue: %d\n\n" +
            "Please review this content immediately.",
            approval.getContent().getTitle(),
            approval.getSubmittedBy().getUsername(),
            approval.getPriority().getDisplayName(),
            approval.getDueDate(),
            java.time.temporal.ChronoUnit.DAYS.between(approval.getDueDate().toLocalDate(), 
                                                      java.time.LocalDate.now())
        );
    }
    
    private String buildTeamApprovalMessage(ContentApproval approval) {
        return String.format(
            "Content has been approved and is ready for publication:\n\n" +
            "Content: %s\n" +
            "Approved by: %s\n" +
            "Approved at: %s\n\n" +
            "The content is now available for publication.",
            approval.getContent().getTitle(),
            approval.getCurrentReviewer().getUsername(),
            approval.getReviewedAt()
        );
    }
}