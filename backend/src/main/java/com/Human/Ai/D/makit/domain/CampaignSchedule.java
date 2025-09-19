package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "campaign_schedules")
public class CampaignSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType;

    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;

    // For recurring campaigns
    @ElementCollection(targetClass = DayOfWeek.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "campaign_schedule_days", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> daysOfWeek;

    private LocalTime dailyStartTime;
    private LocalTime dailyEndTime;

    @Enumerated(EnumType.STRING)
    private RecurrencePattern recurrencePattern;

    private Integer recurrenceInterval; // e.g., every 2 weeks
    private LocalDateTime recurrenceEndDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum ScheduleType {
        IMMEDIATE, SCHEDULED, RECURRING
    }

    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    public enum RecurrencePattern {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }

    // Constructors
    public CampaignSchedule() {}

    public CampaignSchedule(Campaign campaign, ScheduleType scheduleType) {
        this.campaign = campaign;
        this.scheduleType = scheduleType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public LocalDateTime getScheduledStartTime() { return scheduledStartTime; }
    public void setScheduledStartTime(LocalDateTime scheduledStartTime) { this.scheduledStartTime = scheduledStartTime; }

    public LocalDateTime getScheduledEndTime() { return scheduledEndTime; }
    public void setScheduledEndTime(LocalDateTime scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }

    public Set<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public LocalTime getDailyStartTime() { return dailyStartTime; }
    public void setDailyStartTime(LocalTime dailyStartTime) { this.dailyStartTime = dailyStartTime; }

    public LocalTime getDailyEndTime() { return dailyEndTime; }
    public void setDailyEndTime(LocalTime dailyEndTime) { this.dailyEndTime = dailyEndTime; }

    public RecurrencePattern getRecurrencePattern() { return recurrencePattern; }
    public void setRecurrencePattern(RecurrencePattern recurrencePattern) { this.recurrencePattern = recurrencePattern; }

    public Integer getRecurrenceInterval() { return recurrenceInterval; }
    public void setRecurrenceInterval(Integer recurrenceInterval) { this.recurrenceInterval = recurrenceInterval; }

    public LocalDateTime getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(LocalDateTime recurrenceEndDate) { this.recurrenceEndDate = recurrenceEndDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isScheduledForNow() {
        LocalDateTime now = LocalDateTime.now();
        
        if (scheduleType == ScheduleType.IMMEDIATE) {
            return true;
        }
        
        if (scheduleType == ScheduleType.SCHEDULED) {
            return scheduledStartTime != null && 
                   now.isAfter(scheduledStartTime) && 
                   (scheduledEndTime == null || now.isBefore(scheduledEndTime));
        }
        
        if (scheduleType == ScheduleType.RECURRING) {
            return isInRecurrenceWindow(now);
        }
        
        return false;
    }

    private boolean isInRecurrenceWindow(LocalDateTime now) {
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            DayOfWeek currentDay = DayOfWeek.valueOf(now.getDayOfWeek().name());
            if (!daysOfWeek.contains(currentDay)) {
                return false;
            }
        }
        
        if (dailyStartTime != null && dailyEndTime != null) {
            LocalTime currentTime = now.toLocalTime();
            return currentTime.isAfter(dailyStartTime) && currentTime.isBefore(dailyEndTime);
        }
        
        return true;
    }
}