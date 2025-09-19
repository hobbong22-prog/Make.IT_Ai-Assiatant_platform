package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignAlertRepository extends JpaRepository<CampaignAlert, Long> {
    
    List<CampaignAlert> findByCampaign(Campaign campaign);
    
    List<CampaignAlert> findByStatus(CampaignAlert.AlertStatus status);
    
    List<CampaignAlert> findByCampaignAndStatus(Campaign campaign, CampaignAlert.AlertStatus status);
    
    List<CampaignAlert> findByAlertType(CampaignAlert.AlertType alertType);
    
    List<CampaignAlert> findBySeverity(CampaignAlert.AlertSeverity severity);
    
    @Query("SELECT ca FROM CampaignAlert ca WHERE ca.status = 'ACTIVE' " +
           "ORDER BY ca.severity DESC, ca.triggeredAt DESC")
    List<CampaignAlert> findActiveAlertsOrderedBySeverity();
    
    @Query("SELECT ca FROM CampaignAlert ca WHERE ca.campaign = :campaign " +
           "AND ca.status = 'ACTIVE' ORDER BY ca.severity DESC, ca.triggeredAt DESC")
    List<CampaignAlert> findActiveCampaignAlertsOrderedBySeverity(@Param("campaign") Campaign campaign);
    
    @Query("SELECT ca FROM CampaignAlert ca WHERE ca.triggeredAt BETWEEN :start AND :end " +
           "ORDER BY ca.triggeredAt DESC")
    List<CampaignAlert> findAlertsBetween(@Param("start") LocalDateTime start, 
                                         @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(ca) FROM CampaignAlert ca WHERE ca.campaign = :campaign AND ca.status = 'ACTIVE'")
    Long countActiveCampaignAlerts(@Param("campaign") Campaign campaign);
    
    @Query("SELECT COUNT(ca) FROM CampaignAlert ca WHERE ca.status = 'ACTIVE' AND ca.severity = :severity")
    Long countActiveAlertsBySeverity(@Param("severity") CampaignAlert.AlertSeverity severity);
}