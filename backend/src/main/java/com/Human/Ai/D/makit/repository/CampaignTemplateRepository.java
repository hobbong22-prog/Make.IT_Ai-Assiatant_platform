package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignTemplate;
import com.Human.Ai.D.makit.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignTemplateRepository extends JpaRepository<CampaignTemplate, Long> {
    
    List<CampaignTemplate> findByCreatedBy(User user);
    
    List<CampaignTemplate> findByType(Campaign.CampaignType type);
    
    List<CampaignTemplate> findByCreatedByAndType(User user, Campaign.CampaignType type);
    
    List<CampaignTemplate> findByIsActiveTrue();
    
    List<CampaignTemplate> findByCreatedByAndIsActiveTrue(User user);
    
    @Query("SELECT ct FROM CampaignTemplate ct WHERE ct.isActive = true AND ct.type = :type")
    List<CampaignTemplate> findActiveTemplatesByType(@Param("type") Campaign.CampaignType type);
    
    @Query("SELECT ct FROM CampaignTemplate ct WHERE ct.name LIKE %:name% AND ct.isActive = true")
    List<CampaignTemplate> findActiveTemplatesByNameContaining(@Param("name") String name);
}