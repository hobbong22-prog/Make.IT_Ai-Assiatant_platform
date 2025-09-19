package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.ApprovalStatus;
import com.Human.Ai.D.makit.domain.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApprovalStatistics {
    
    private Map<ApprovalStatus, Long> statusCounts;
    private Map<Priority, Long> priorityCounts;
    private long totalApprovals;
    private long activeApprovals;
    private long overdueApprovals;
    
    public ApprovalStatistics() {
        this.statusCounts = new HashMap<>();
        this.priorityCounts = new HashMap<>();
    }
    
    public ApprovalStatistics(List<Object[]> statusStats, List<Object[]> priorityStats) {
        this();
        
        // Process status statistics
        for (Object[] stat : statusStats) {
            ApprovalStatus status = (ApprovalStatus) stat[0];
            Long count = (Long) stat[1];
            statusCounts.put(status, count);
            totalApprovals += count;
            
            if (status.isActive()) {
                activeApprovals += count;
            }
        }
        
        // Process priority statistics
        for (Object[] stat : priorityStats) {
            Priority priority = (Priority) stat[0];
            Long count = (Long) stat[1];
            priorityCounts.put(priority, count);
        }
    }
    
    // Getters and Setters
    public Map<ApprovalStatus, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<ApprovalStatus, Long> statusCounts) { this.statusCounts = statusCounts; }
    
    public Map<Priority, Long> getPriorityCounts() { return priorityCounts; }
    public void setPriorityCounts(Map<Priority, Long> priorityCounts) { this.priorityCounts = priorityCounts; }
    
    public long getTotalApprovals() { return totalApprovals; }
    public void setTotalApprovals(long totalApprovals) { this.totalApprovals = totalApprovals; }
    
    public long getActiveApprovals() { return activeApprovals; }
    public void setActiveApprovals(long activeApprovals) { this.activeApprovals = activeApprovals; }
    
    public long getOverdueApprovals() { return overdueApprovals; }
    public void setOverdueApprovals(long overdueApprovals) { this.overdueApprovals = overdueApprovals; }
    
    // Convenience methods
    public long getApprovedCount() {
        return statusCounts.getOrDefault(ApprovalStatus.APPROVED, 0L);
    }
    
    public long getRejectedCount() {
        return statusCounts.getOrDefault(ApprovalStatus.REJECTED, 0L);
    }
    
    public long getPendingCount() {
        return statusCounts.getOrDefault(ApprovalStatus.PENDING, 0L);
    }
    
    public long getInReviewCount() {
        return statusCounts.getOrDefault(ApprovalStatus.IN_REVIEW, 0L);
    }
    
    public long getNeedsRevisionCount() {
        return statusCounts.getOrDefault(ApprovalStatus.NEEDS_REVISION, 0L);
    }
    
    public double getApprovalRate() {
        if (totalApprovals == 0) return 0.0;
        return (double) getApprovedCount() / totalApprovals * 100;
    }
    
    public double getRejectionRate() {
        if (totalApprovals == 0) return 0.0;
        return (double) getRejectedCount() / totalApprovals * 100;
    }
}