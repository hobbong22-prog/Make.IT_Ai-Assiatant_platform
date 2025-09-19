package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.ContentApproval;
import com.Human.Ai.D.makit.domain.ApprovalStatus;
import com.Human.Ai.D.makit.domain.Priority;
import com.Human.Ai.D.makit.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContentApprovalRepository extends JpaRepository<ContentApproval, Long> {
    
    // Find by status
    List<ContentApproval> findByStatus(ApprovalStatus status);
    Page<ContentApproval> findByStatus(ApprovalStatus status, Pageable pageable);
    
    // Find by current reviewer
    List<ContentApproval> findByCurrentReviewer(User reviewer);
    Page<ContentApproval> findByCurrentReviewer(User reviewer, Pageable pageable);
    
    // Find by submitter
    List<ContentApproval> findBySubmittedBy(User submittedBy);
    Page<ContentApproval> findBySubmittedBy(User submittedBy, Pageable pageable);
    
    // Find by content ID
    Optional<ContentApproval> findByContentId(Long contentId);
    
    // Find by priority
    List<ContentApproval> findByPriority(Priority priority);
    
    // Find overdue approvals
    @Query("SELECT ca FROM ContentApproval ca WHERE ca.dueDate < :now AND ca.status IN :activeStatuses")
    List<ContentApproval> findOverdueApprovals(@Param("now") LocalDateTime now, 
                                              @Param("activeStatuses") List<ApprovalStatus> activeStatuses);
    
    // Find approvals requiring attention (overdue or high priority pending)
    @Query("SELECT ca FROM ContentApproval ca WHERE " +
           "(ca.dueDate < :now AND ca.status IN :activeStatuses) OR " +
           "(ca.priority = :highPriority AND ca.status = :pendingStatus)")
    List<ContentApproval> findApprovalsRequiringAttention(@Param("now") LocalDateTime now,
                                                         @Param("activeStatuses") List<ApprovalStatus> activeStatuses,
                                                         @Param("highPriority") Priority highPriority,
                                                         @Param("pendingStatus") ApprovalStatus pendingStatus);
    
    // Count by status
    long countByStatus(ApprovalStatus status);
    
    // Count by reviewer and status
    long countByCurrentReviewerAndStatus(User reviewer, ApprovalStatus status);
    
    // Find approvals submitted within date range
    @Query("SELECT ca FROM ContentApproval ca WHERE ca.submittedAt BETWEEN :startDate AND :endDate")
    List<ContentApproval> findBySubmittedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
    
    // Find approvals by multiple statuses
    List<ContentApproval> findByStatusIn(List<ApprovalStatus> statuses);
    Page<ContentApproval> findByStatusIn(List<ApprovalStatus> statuses, Pageable pageable);
    
    // Find approvals by reviewer and statuses
    List<ContentApproval> findByCurrentReviewerAndStatusIn(User reviewer, List<ApprovalStatus> statuses);
    Page<ContentApproval> findByCurrentReviewerAndStatusIn(User reviewer, List<ApprovalStatus> statuses, Pageable pageable);
    
    // Statistics queries
    @Query("SELECT ca.status, COUNT(ca) FROM ContentApproval ca GROUP BY ca.status")
    List<Object[]> getApprovalStatusStatistics();
    
    @Query("SELECT ca.priority, COUNT(ca) FROM ContentApproval ca WHERE ca.status IN :activeStatuses GROUP BY ca.priority")
    List<Object[]> getActivePriorityStatistics(@Param("activeStatuses") List<ApprovalStatus> activeStatuses);
}