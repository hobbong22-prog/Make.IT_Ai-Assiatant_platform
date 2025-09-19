package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignScheduleRepository extends JpaRepository<CampaignSchedule, Long> {
    
    Optional<CampaignSchedule> findByCampaign(Campaign campaign);
    
    List<CampaignSchedule> findByScheduleType(CampaignSchedule.ScheduleType scheduleType);
    
    List<CampaignSchedule> findByIsActiveTrue();
    
    @Query("SELECT cs FROM CampaignSchedule cs WHERE cs.scheduleType = 'SCHEDULED' " +
           "AND cs.scheduledStartTime <= :now AND cs.isActive = true " +
           "AND (cs.scheduledEndTime IS NULL OR cs.scheduledEndTime >= :now)")
    List<CampaignSchedule> findScheduledCampaignsForExecution(@Param("now") LocalDateTime now);
    
    @Query("SELECT cs FROM CampaignSchedule cs WHERE cs.scheduleType = 'RECURRING' " +
           "AND cs.isActive = true " +
           "AND (cs.recurrenceEndDate IS NULL OR cs.recurrenceEndDate >= :now)")
    List<CampaignSchedule> findRecurringCampaignsForExecution(@Param("now") LocalDateTime now);
    
    @Query("SELECT cs FROM CampaignSchedule cs WHERE cs.scheduledStartTime BETWEEN :start AND :end " +
           "AND cs.isActive = true")
    List<CampaignSchedule> findScheduledCampaignsBetween(@Param("start") LocalDateTime start, 
                                                        @Param("end") LocalDateTime end);
}