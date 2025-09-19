package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.CampaignMetrics;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.dto.CampaignCreateRequest;
import com.Human.Ai.D.makit.repository.CampaignRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.CampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {
    
    @Autowired
    private CampaignService campaignService;
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentUser(#userId) or @authService.canManageCampaigns(authentication.principal)")
    public ResponseEntity<List<Campaign>> getUserCampaigns(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Campaign> campaigns = campaignRepository.findByUser(user);
        return ResponseEntity.ok(campaigns);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'ANALYST', 'VIEWER')")
    public ResponseEntity<Campaign> getCampaign(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        return ResponseEntity.ok(campaign);
    }
    
    @GetMapping("/{id}/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'ANALYST')")
    public ResponseEntity<List<CampaignMetrics>> getCampaignMetrics(@PathVariable Long id) {
        Campaign campaign = campaignRepository.findByIdWithMetrics(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        return ResponseEntity.ok(campaign.getMetrics());
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<Campaign> createCampaign(@RequestBody CampaignCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Campaign campaign = new Campaign(
                request.getName(), 
                request.getDescription(), 
                request.getType(), 
                user
        );
        
        campaign.setTargetAudience(request.getTargetAudience());
        campaign.setBudget(request.getBudget());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        
        campaign = campaignRepository.save(campaign);
        return ResponseEntity.ok(campaign);
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<Campaign> updateCampaignStatus(
            @PathVariable Long id, 
            @RequestParam Campaign.CampaignStatus status) {
        
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        campaign.setStatus(status);
        campaign = campaignRepository.save(campaign);
        
        return ResponseEntity.ok(campaign);
    }
    
    @PostMapping("/{id}/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<CampaignMetrics> addCampaignMetrics(
            @PathVariable Long id, 
            @RequestBody CampaignMetrics metrics) {
        
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        metrics.setCampaign(campaign);
        metrics.calculateMetrics();
        
        CampaignMetrics savedMetrics = campaignService.saveCampaignMetrics(metrics);
        return ResponseEntity.ok(savedMetrics);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        campaignRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}