package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.model.ContentTemplate;
import com.Human.Ai.D.makit.service.ContentTemplateManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 콘텐츠 템플릿 관리 REST API 컨트롤러
 * Human.Ai.D MaKIT 플랫폼의 템플릿 관리 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
public class ContentTemplateController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentTemplateController.class);
    
    @Autowired
    private ContentTemplateManager templateManager;
    
    /**
     * 새로운 콘텐츠 템플릿을 생성합니다.
     * 
     * @param template 생성할 템플릿
     * @param userId 생성자 사용자 ID
     * @return 생성된 템플릿
     */
    @PostMapping
    public ResponseEntity<?> createTemplate(@Valid @RequestBody ContentTemplate template,
                                          @RequestParam Long userId) {
        try {
            logger.info("템플릿 생성 요청 - 템플릿 ID: {}, 사용자 ID: {}", 
                       template.getTemplateId(), userId);
            
            ContentTemplate createdTemplate = templateManager.createTemplate(template, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTemplate);
            
        } catch (IllegalArgumentException e) {
            logger.warn("템플릿 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("템플릿 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 생성 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 템플릿 ID로 템플릿을 조회합니다.
     * 
     * @param templateId 템플릿 ID
     * @return 템플릿 정보
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<?> getTemplate(@PathVariable String templateId) {
        try {
            logger.debug("템플릿 조회 요청 - 템플릿 ID: {}", templateId);
            
            Optional<ContentTemplate> template = templateManager.getTemplateById(templateId);
            if (template.isPresent()) {
                return ResponseEntity.ok(template.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("템플릿 조회 중 오류 발생 - 템플릿 ID: {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 조회 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 콘텐츠 타입별 템플릿 목록을 조회합니다.
     * 
     * @param contentType 콘텐츠 타입
     * @return 템플릿 목록
     */
    @GetMapping("/by-content-type/{contentType}")
    public ResponseEntity<?> getTemplatesByContentType(@PathVariable String contentType) {
        try {
            logger.debug("콘텐츠 타입별 템플릿 조회 - 타입: {}", contentType);
            
            List<ContentTemplate> templates = templateManager.getTemplatesByContentType(contentType);
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            logger.error("콘텐츠 타입별 템플릿 조회 중 오류 발생 - 타입: {}", contentType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 조회 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 카테고리별 템플릿 목록을 조회합니다.
     * 
     * @param category 카테고리
     * @return 템플릿 목록
     */
    @GetMapping("/by-category/{category}")
    public ResponseEntity<?> getTemplatesByCategory(@PathVariable String category) {
        try {
            logger.debug("카테고리별 템플릿 조회 - 카테고리: {}", category);
            
            List<ContentTemplate> templates = templateManager.getTemplatesByCategory(category);
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            logger.error("카테고리별 템플릿 조회 중 오류 발생 - 카테고리: {}", category, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 조회 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 사용자별 템플릿 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 템플릿 목록
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getTemplatesByUser(@PathVariable Long userId) {
        try {
            logger.debug("사용자별 템플릿 조회 - 사용자 ID: {}", userId);
            
            List<ContentTemplate> templates = templateManager.getTemplatesByUser(userId);
            return ResponseEntity.ok(templates);
            
        } catch (IllegalArgumentException e) {
            logger.warn("사용자별 템플릿 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("사용자별 템플릿 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 조회 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 공개 템플릿 목록을 조회합니다.
     * 
     * @return 공개 템플릿 목록
     */
    @GetMapping("/public")
    public ResponseEntity<?> getPublicTemplates() {
        try {
            logger.debug("공개 템플릿 조회 요청");
            
            List<ContentTemplate> templates = templateManager.getPublicTemplates();
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            logger.error("공개 템플릿 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 조회 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 인기 템플릿 목록을 조회합니다.
     * 
     * @param limit 조회할 개수 (기본값: 10)
     * @return 인기 템플릿 목록
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularTemplates(@RequestParam(defaultValue = "10") int limit) {
        try {
            logger.debug("인기 템플릿 조회 요청 - 개수: {}", limit);
            
            List<ContentTemplate> templates = templateManager.getPopularTemplates(limit);
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            logger.error("인기 템플릿 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 조회 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 최근 생성된 템플릿 목록을 조회합니다.
     * 
     * @return 최근 템플릿 목록
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentTemplates() {
        try {
            logger.debug("최근 템플릿 조회 요청");
            
            List<ContentTemplate> templates = templateManager.getRecentTemplates();
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            logger.error("최근 템플릿 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 조회 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 키워드로 템플릿을 검색합니다.
     * 
     * @param keyword 검색 키워드
     * @return 검색된 템플릿 목록
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchTemplates(@RequestParam String keyword) {
        try {
            logger.debug("템플릿 검색 요청 - 키워드: {}", keyword);
            
            List<ContentTemplate> templates = templateManager.searchTemplates(keyword);
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            logger.error("템플릿 검색 중 오류 발생 - 키워드: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 검색 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 태그로 템플릿을 검색합니다.
     * 
     * @param tags 태그 목록 (쉼표로 구분)
     * @return 검색된 템플릿 목록
     */
    @GetMapping("/search-by-tags")
    public ResponseEntity<?> searchTemplatesByTags(@RequestParam String tags) {
        try {
            logger.debug("태그별 템플릿 검색 요청 - 태그: {}", tags);
            
            List<String> tagList = List.of(tags.split(","));
            List<ContentTemplate> templates = templateManager.searchTemplatesByTags(tagList);
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            logger.error("태그별 템플릿 검색 중 오류 발생 - 태그: {}", tags, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 검색 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 템플릿을 업데이트합니다.
     * 
     * @param templateId 템플릿 ID
     * @param template 업데이트할 템플릿 정보
     * @param userId 업데이트 요청 사용자 ID
     * @return 업데이트된 템플릿
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<?> updateTemplate(@PathVariable String templateId,
                                          @Valid @RequestBody ContentTemplate template,
                                          @RequestParam Long userId) {
        try {
            logger.info("템플릿 업데이트 요청 - 템플릿 ID: {}, 사용자 ID: {}", templateId, userId);
            
            ContentTemplate updatedTemplate = templateManager.updateTemplate(templateId, template, userId);
            return ResponseEntity.ok(updatedTemplate);
            
        } catch (IllegalArgumentException e) {
            logger.warn("템플릿 업데이트 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("템플릿 업데이트 중 오류 발생 - 템플릿 ID: {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 업데이트 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 템플릿을 삭제합니다.
     * 
     * @param templateId 템플릿 ID
     * @param userId 삭제 요청 사용자 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<?> deleteTemplate(@PathVariable String templateId,
                                          @RequestParam Long userId) {
        try {
            logger.info("템플릿 삭제 요청 - 템플릿 ID: {}, 사용자 ID: {}", templateId, userId);
            
            templateManager.deleteTemplate(templateId, userId);
            return ResponseEntity.ok(Map.of("message", "템플릿이 성공적으로 삭제되었습니다"));
            
        } catch (IllegalArgumentException e) {
            logger.warn("템플릿 삭제 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("템플릿 삭제 중 오류 발생 - 템플릿 ID: {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "템플릿 삭제 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 템플릿 사용 횟수를 증가시킵니다.
     * 
     * @param templateId 템플릿 ID
     * @return 업데이트 결과
     */
    @PostMapping("/{templateId}/use")
    public ResponseEntity<?> incrementUsageCount(@PathVariable String templateId) {
        try {
            logger.debug("템플릿 사용 횟수 증가 요청 - 템플릿 ID: {}", templateId);
            
            templateManager.incrementUsageCount(templateId);
            return ResponseEntity.ok(Map.of("message", "사용 횟수가 증가되었습니다"));
            
        } catch (Exception e) {
            logger.error("템플릿 사용 횟수 증가 중 오류 발생 - 템플릿 ID: {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "사용 횟수 업데이트 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 템플릿 평점을 업데이트합니다.
     * 
     * @param templateId 템플릿 ID
     * @param rating 새로운 평점
     * @param totalRatings 총 평점 수
     * @return 업데이트 결과
     */
    @PostMapping("/{templateId}/rate")
    public ResponseEntity<?> updateRating(@PathVariable String templateId,
                                        @RequestParam double rating,
                                        @RequestParam int totalRatings) {
        try {
            logger.debug("템플릿 평점 업데이트 요청 - 템플릿 ID: {}, 평점: {}", templateId, rating);
            
            if (rating < 0 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "평점은 0-5 사이의 값이어야 합니다"));
            }
            
            templateManager.updateTemplateRating(templateId, rating, totalRatings);
            return ResponseEntity.ok(Map.of("message", "평점이 업데이트되었습니다"));
            
        } catch (Exception e) {
            logger.error("템플릿 평점 업데이트 중 오류 발생 - 템플릿 ID: {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "평점 업데이트 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 콘텐츠 타입별 템플릿 통계를 조회합니다.
     * 
     * @return 콘텐츠 타입별 템플릿 수
     */
    @GetMapping("/stats/by-content-type")
    public ResponseEntity<?> getTemplateStatsByContentType() {
        try {
            logger.debug("콘텐츠 타입별 템플릿 통계 조회 요청");
            
            // 주요 콘텐츠 타입들에 대한 통계 수집
            String[] contentTypes = {"BLOG_POST", "EMAIL_TEMPLATE", "AD_COPY", "SOCIAL_MEDIA_POST"};
            Map<String, Long> stats = new java.util.HashMap<>();
            
            for (String contentType : contentTypes) {
                long count = templateManager.getTemplateCountByContentType(contentType);
                stats.put(contentType, count);
            }
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("템플릿 통계 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "통계 조회 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 사용자별 템플릿 수를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 사용자의 템플릿 수
     */
    @GetMapping("/stats/by-user/{userId}")
    public ResponseEntity<?> getTemplateCountByUser(@PathVariable Long userId) {
        try {
            logger.debug("사용자별 템플릿 수 조회 요청 - 사용자 ID: {}", userId);
            
            long count = templateManager.getTemplateCountByUser(userId);
            return ResponseEntity.ok(Map.of("userId", userId, "templateCount", count));
            
        } catch (Exception e) {
            logger.error("사용자별 템플릿 수 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "통계 조회 중 오류가 발생했습니다"));
        }
    }
}