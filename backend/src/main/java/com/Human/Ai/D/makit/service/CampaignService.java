package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignMetrics;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CampaignService {
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    public Campaign createCampaign(String name, String description, Campaign.CampaignType type, User user) {
        Campaign campaign = new Campaign(name, description, type, user);
        return campaignRepository.save(campaign);
    }
    
    public Campaign updateCampaignStatus(Long campaignId, Campaign.CampaignStatus status) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        campaign.setStatus(status);
        return campaignRepository.save(campaign);
    }
    
    public List<Campaign> getActiveCampaigns(User user) {
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository.findActiveCampaignsByUser(user, now);
    }
    
    public List<Campaign> getCampaignsByStatus(User user, Campaign.CampaignStatus status) {
        return campaignRepository.findByUserAndStatus(user, status);
    }
    
    public List<Campaign> getCampaignsByType(User user, Campaign.CampaignType type) {
        return campaignRepository.findByUserAndType(user, type);
    }
    
    public CampaignMetrics saveCampaignMetrics(CampaignMetrics metrics) {
        // This would typically be handled by a separate repository
        // For now, we'll add it to the campaign's metrics list
        Campaign campaign = metrics.getCampaign();
        campaign.getMetrics().add(metrics);
        campaignRepository.save(campaign);
        return metrics;
    }
    
    public void deleteCampaign(Long campaignId) {
        campaignRepository.deleteById(campaignId);
    }
    
    // Analytics methods
    public Long getTotalCampaigns(User user) {
        return (long) campaignRepository.findByUser(user).size();
    }
    
    public Long getActiveCampaignsCount(User user) {
        return campaignRepository.countByUserAndStatus(user, Campaign.CampaignStatus.ACTIVE);
    }
    
    public Long getCompletedCampaignsCount(User user) {
        return campaignRepository.countByUserAndStatus(user, Campaign.CampaignStatus.COMPLETED);
    }
}