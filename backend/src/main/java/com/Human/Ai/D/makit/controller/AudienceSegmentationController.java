package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.AudienceSegment;
import com.Human.Ai.D.makit.service.AudienceSegmentationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/audience-segments")
@CrossOrigin(origins = "*")
public class AudienceSegmentationController {
    
    @Autowired
    private AudienceSegmentationService segmentationService;
    
    /**
     * Create AI-generated audience segment
     */
    @PostMapping("/users/{userId}/ai-generated")
    public CompletableFuture<ResponseEntity<AudienceSegment>> createAIGeneratedSegment(
            @PathVariable Long userId,
            @RequestParam String segmentName,
            @RequestBody Map<String, String> criteria) {
        
        return segmentationService.createAIGeneratedSegment(userId, segmentName, criteria)
                .thenApply(segment -> ResponseEntity.ok(segment));
    }
    
    /**
     * Create custom audience segment
     */
    @PostMapping("/users/{userId}/custom")
    public ResponseEntity<AudienceSegment> createCustomSegment(
            @PathVariable Long userId,
            @RequestParam String segmentName,
            @RequestParam String segmentType,
            @RequestBody Map<String, String> criteria) {
        
        AudienceSegment.SegmentType type = AudienceSegment.SegmentType.valueOf(segmentType.toUpperCase());
        AudienceSegment segment = segmentationService.createCustomSegment(userId, segmentName, criteria, type);
        
        return ResponseEntity.ok(segment);
    }
    
    /**
     * Get all segments for a user
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<AudienceSegment>> getUserSegments(@PathVariable Long userId) {
        List<AudienceSegment> segments = segmentationService.getUserSegments(userId);
        return ResponseEntity.ok(segments);
    }
    
    /**
     * Get active segments for a user
     */
    @GetMapping("/users/{userId}/active")
    public ResponseEntity<List<AudienceSegment>> getActiveSegments(@PathVariable Long userId) {
        List<AudienceSegment> segments = segmentationService.getActiveSegments(userId);
        return ResponseEntity.ok(segments);
    }
    
    /**
     * Get high-performing segments
     */
    @GetMapping("/users/{userId}/high-performing")
    public ResponseEntity<List<AudienceSegment>> getHighPerformingSegments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "70.0") Double minScore) {
        
        List<AudienceSegment> segments = segmentationService.getHighPerformingSegments(userId, minScore);
        return ResponseEntity.ok(segments);
    }
    
    /**
     * Analyze segment performance
     */
    @PostMapping("/{segmentId}/analyze")
    public CompletableFuture<ResponseEntity<String>> analyzeSegmentPerformance(@PathVariable Long segmentId) {
        return segmentationService.analyzeSegmentPerformance(segmentId)
                .thenApply(result -> ResponseEntity.ok("Analysis initiated for segment " + segmentId));
    }
    
    /**
     * Generate lookalike segments
     */
    @PostMapping("/users/{userId}/lookalike/{sourceSegmentId}")
    public CompletableFuture<ResponseEntity<List<AudienceSegment>>> generateLookalikeSegments(
            @PathVariable Long userId,
            @PathVariable Long sourceSegmentId) {
        
        return segmentationService.generateLookalikeSegments(userId, sourceSegmentId)
                .thenApply(segments -> ResponseEntity.ok(segments));
    }
    
    /**
     * Update segment status
     */
    @PutMapping("/{segmentId}/status")
    public ResponseEntity<AudienceSegment> updateSegmentStatus(
            @PathVariable Long segmentId,
            @RequestParam String status) {
        
        try {
            AudienceSegment.SegmentStatus segmentStatus = 
                    AudienceSegment.SegmentStatus.valueOf(status.toUpperCase());
            
            AudienceSegment segment = segmentationService.updateSegmentStatus(segmentId, segmentStatus);
            return ResponseEntity.ok(segment);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get segment statistics
     */
    @GetMapping("/users/{userId}/statistics")
    public ResponseEntity<Map<String, Object>> getSegmentStatistics(@PathVariable Long userId) {
        Map<String, Object> stats = segmentationService.getSegmentStatistics(userId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Delete segment
     */
    @DeleteMapping("/{segmentId}")
    public ResponseEntity<String> deleteSegment(@PathVariable Long segmentId) {
        segmentationService.deleteSegment(segmentId);
        return ResponseEntity.ok("Segment deleted successfully");
    }
    
    /**
     * Get segment by ID
     */
    @GetMapping("/{segmentId}")
    public ResponseEntity<AudienceSegment> getSegment(@PathVariable Long segmentId) {
        // This would require a method in the service to get by ID
        // For now, return a simple response
        return ResponseEntity.notFound().build();
    }
}