package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.OptimizationRecommendation;
import com.Human.Ai.D.makit.service.OptimizationRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class OptimizationRecommendationController {
    
    @Autowired
    private OptimizationRecommendationService recommendationService;
    
    /**
     * Generate recommendations for a campaign
     */
    @PostMapping("/campaigns/{campaignId}/generate")
    public CompletableFuture<ResponseEntity<List<OptimizationRecommendation>>> generateRecommendations(
            @PathVariable Long campaignId) {
        
        return recommendationService.generateRecommendations(campaignId)
                .thenApply(recommendations -> ResponseEntity.ok(recommendations));
    }
    
    /**
     * Get active recommendations for a campaign
     */
    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<List<OptimizationRecommendation>> getCampaignRecommendations(
            @PathVariable Long campaignId) {
        
        List<OptimizationRecommendation> recommendations = 
                recommendationService.getActiveRecommendations(campaignId);
        return ResponseEntity.ok(recommendations);
    }
    
    /**
     * Get recommendations by user and status
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<OptimizationRecommendation>> getUserRecommendations(
            @PathVariable Long userId,
            @RequestParam(required = false) String status) {
        
        List<OptimizationRecommendation> recommendations;
        
        if (status != null) {
            OptimizationRecommendation.RecommendationStatus recommendationStatus = 
                    OptimizationRecommendation.RecommendationStatus.valueOf(status.toUpperCase());
            recommendations = recommendationService.getRecommendationsByUserAndStatus(userId, recommendationStatus);
        } else {
            recommendations = recommendationService.getRecommendationsByUserAndStatus(
                    userId, OptimizationRecommendation.RecommendationStatus.PENDING);
        }
        
        return ResponseEntity.ok(recommendations);
    }
    
    /**
     * Get high priority recommendations for a user
     */
    @GetMapping("/users/{userId}/high-priority")
    public ResponseEntity<List<OptimizationRecommendation>> getHighPriorityRecommendations(
            @PathVariable Long userId) {
        
        List<OptimizationRecommendation> recommendations = 
                recommendationService.getHighPriorityRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }
    
    /**
     * Implement a recommendation
     */
    @PutMapping("/{recommendationId}/implement")
    public ResponseEntity<OptimizationRecommendation> implementRecommendation(
            @PathVariable Long recommendationId) {
        
        OptimizationRecommendation recommendation = 
                recommendationService.implementRecommendation(recommendationId);
        return ResponseEntity.ok(recommendation);
    }
    
    /**
     * Dismiss a recommendation
     */
    @PutMapping("/{recommendationId}/dismiss")
    public ResponseEntity<OptimizationRecommendation> dismissRecommendation(
            @PathVariable Long recommendationId) {
        
        OptimizationRecommendation recommendation = 
                recommendationService.dismissRecommendation(recommendationId);
        return ResponseEntity.ok(recommendation);
    }
    
    /**
     * Get recommendation statistics for a user
     */
    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getRecommendationStats(@PathVariable Long userId) {
        Map<String, Object> stats = recommendationService.getRecommendationStats(userId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get recommendation by ID
     */
    @GetMapping("/{recommendationId}")
    public ResponseEntity<OptimizationRecommendation> getRecommendation(@PathVariable Long recommendationId) {
        // This would require a method in the service to get by ID
        // For now, return a simple response
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Update recommendation status
     */
    @PutMapping("/{recommendationId}/status")
    public ResponseEntity<String> updateRecommendationStatus(
            @PathVariable Long recommendationId,
            @RequestParam String status) {
        
        try {
            OptimizationRecommendation.RecommendationStatus newStatus = 
                    OptimizationRecommendation.RecommendationStatus.valueOf(status.toUpperCase());
            
            if (newStatus == OptimizationRecommendation.RecommendationStatus.IMPLEMENTED) {
                recommendationService.implementRecommendation(recommendationId);
            } else if (newStatus == OptimizationRecommendation.RecommendationStatus.DISMISSED) {
                recommendationService.dismissRecommendation(recommendationId);
            }
            
            return ResponseEntity.ok("Status updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status: " + status);
        }
    }
}