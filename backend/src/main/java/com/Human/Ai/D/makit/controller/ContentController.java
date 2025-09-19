package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.Content;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.dto.ContentGenerationRequest;
import com.Human.Ai.D.makit.repository.ContentRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.ContentGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/content")
@CrossOrigin(origins = "*")
public class ContentController {
    
    @Autowired
    private ContentGenerationService contentGenerationService;
    
    @Autowired
    private com.Human.Ai.D.makit.service.EnhancedContentGenerationService enhancedContentGenerationService;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentUser(#userId) or @authService.canManageContent(authentication.principal)")
    public ResponseEntity<List<Content>> getUserContents(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Content> contents = contentRepository.findByUser(user);
        return ResponseEntity.ok(contents);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR', 'ANALYST', 'VIEWER')")
    public ResponseEntity<Content> getContent(@PathVariable Long id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        return ResponseEntity.ok(content);
    }
    
    @PostMapping("/generate/blog")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<CompletableFuture<Content>> generateBlogPost(
            @RequestBody ContentGenerationRequest request) {
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CompletableFuture<Content> future = contentGenerationService.generateBlogPost(
                request.getTopic(), 
                request.getTargetAudience(), 
                user
        );
        
        return ResponseEntity.ok(future);
    }
    
    @PostMapping("/generate/ad-copy")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<CompletableFuture<Content>> generateAdCopy(
            @RequestBody ContentGenerationRequest request) {
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CompletableFuture<Content> future = contentGenerationService.generateAdCopy(
                request.getProduct(), 
                request.getTargetAudience(), 
                request.getPlatform(), 
                user
        );
        
        return ResponseEntity.ok(future);
    }
    
    @PostMapping("/generate/social-media")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<CompletableFuture<Content>> generateSocialMediaPost(
            @RequestBody ContentGenerationRequest request) {
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CompletableFuture<Content> future = contentGenerationService.generateSocialMediaPost(
                request.getTopic(), 
                request.getPlatform(), 
                request.getTone(), 
                user
        );
        
        return ResponseEntity.ok(future);
    }
    
    @PostMapping("/generate/email")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<CompletableFuture<Content>> generateEmailTemplate(
            @RequestBody ContentGenerationRequest request) {
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CompletableFuture<Content> future = contentGenerationService.generateEmailTemplate(
                request.getSubject(), 
                request.getPurpose(), 
                request.getTargetAudience(), 
                user
        );
        
        return ResponseEntity.ok(future);
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<Content> updateContentStatus(
            @PathVariable Long id, 
            @RequestParam Content.ContentStatus status) {
        
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        
        content.setStatus(status);
        content = contentRepository.save(content);
        
        return ResponseEntity.ok(content);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id) {
        contentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 향상된 콘텐츠 생성 (전략 패턴 + 품질 분석)
     */
    @PostMapping("/generate/enhanced")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<?> generateEnhancedContent(@RequestBody ContentGenerationRequest request) {
        try {
            CompletableFuture<Content> future = enhancedContentGenerationService.generateContent(request);
            return ResponseEntity.ok(future);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("콘텐츠 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 콘텐츠 품질 분석
     */
    @PostMapping("/{id}/analyze-quality")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR', 'ANALYST')")
    public ResponseEntity<?> analyzeContentQuality(@PathVariable Long id) {
        try {
            CompletableFuture<com.Human.Ai.D.makit.service.ContentQualityAnalyzer.QualityAnalysisResult> future = 
                enhancedContentGenerationService.analyzeContentQuality(id);
            return ResponseEntity.ok(future);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("품질 분석 실패: " + e.getMessage());
        }
    }
    
    /**
     * 사용 가능한 콘텐츠 생성 전략 조회
     */
    @GetMapping("/strategies")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<?> getAvailableStrategies() {
        try {
            var strategies = enhancedContentGenerationService.getAvailableStrategies();
            return ResponseEntity.ok(strategies.stream()
                .map(strategy -> java.util.Map.of(
                    "modelId", strategy.getModelId(),
                    "priority", strategy.getPriority(),
                    "supportsMultimodal", strategy.supportsMultimodal()
                ))
                .toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("전략 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 특정 콘텐츠 타입을 지원하는 전략 조회
     */
    @GetMapping("/strategies/{contentType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'CONTENT_CREATOR')")
    public ResponseEntity<?> getStrategiesForContentType(@PathVariable String contentType) {
        try {
            Content.ContentType type = Content.ContentType.valueOf(contentType.toUpperCase());
            var strategies = enhancedContentGenerationService.getStrategiesForContentType(type);
            return ResponseEntity.ok(strategies.stream()
                .map(strategy -> java.util.Map.of(
                    "modelId", strategy.getModelId(),
                    "priority", strategy.getPriority(),
                    "supportsMultimodal", strategy.supportsMultimodal()
                ))
                .toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 콘텐츠 타입: " + contentType);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("전략 조회 실패: " + e.getMessage());
        }
    }}
