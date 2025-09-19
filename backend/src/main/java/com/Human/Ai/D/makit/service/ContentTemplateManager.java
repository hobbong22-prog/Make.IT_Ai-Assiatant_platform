package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.model.ContentTemplate;
import com.Human.Ai.D.makit.repository.ContentTemplateRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 콘텐츠 템플릿 관리 서비스
 * Human.Ai.D MaKIT 플랫폼의 템플릿 CRUD 작업 및 관리 기능을 제공합니다.
 */
@Service
@Transactional
public class ContentTemplateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentTemplateManager.class);
    
    @Autowired
    private ContentTemplateRepository templateRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 새로운 콘텐츠 템플릿을 생성합니다.
     * 
     * @param template 생성할 템플릿
     * @param userId 생성자 사용자 ID
     * @return 생성된 템플릿
     */
    public ContentTemplate createTemplate(ContentTemplate template, Long userId) {
        logger.info("새 템플릿 생성 시작 - 템플릿 ID: {}, 사용자 ID: {}", 
                   template.getTemplateId(), userId);
        
        try {
            // 사용자 조회
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
            }
            User user = userOpt.get();
            
            // 템플릿 ID 중복 확인
            if (templateRepository.existsByTemplateIdAndIsActiveTrue(template.getTemplateId())) {
                throw new IllegalArgumentException("이미 존재하는 템플릿 ID입니다: " + template.getTemplateId());
            }
            
            // 템플릿 설정
            template.setCreatedBy(user);
            template.setCreatedAt(LocalDateTime.now());
            template.setUpdatedAt(LocalDateTime.now());
            template.setActive(true);
            
            ContentTemplate savedTemplate = templateRepository.save(template);
            logger.info("템플릿 생성 완료 - ID: {}", savedTemplate.getId());
            
            return savedTemplate;
            
        } catch (Exception e) {
            logger.error("템플릿 생성 실패 - 템플릿 ID: {}", template.getTemplateId(), e);
            throw e;
        }
    }
    
    /**
     * 템플릿 ID로 활성 템플릿을 조회합니다.
     * 
     * @param templateId 템플릿 ID
     * @return 템플릿 (Optional)
     */
    public Optional<ContentTemplate> getTemplateById(String templateId) {
        logger.debug("템플릿 조회 - 템플릿 ID: {}", templateId);
        return templateRepository.findByTemplateIdAndIsActiveTrue(templateId);
    }
    
    /**
     * 콘텐츠 타입별 활성 템플릿 목록을 조회합니다.
     * 
     * @param contentType 콘텐츠 타입
     * @return 템플릿 목록 (사용 횟수 순)
     */
    public List<ContentTemplate> getTemplatesByContentType(String contentType) {
        logger.debug("콘텐츠 타입별 템플릿 조회 - 타입: {}", contentType);
        return templateRepository.findByContentTypeAndIsActiveTrueOrderByUsageCountDesc(contentType);
    }
    
    /**
     * 카테고리별 활성 템플릿 목록을 조회합니다.
     * 
     * @param category 카테고리
     * @return 템플릿 목록 (평점 순)
     */
    public List<ContentTemplate> getTemplatesByCategory(String category) {
        logger.debug("카테고리별 템플릿 조회 - 카테고리: {}", category);
        return templateRepository.findByCategoryAndIsActiveTrueOrderByRatingDesc(category);
    }
    
    /**
     * 사용자별 템플릿 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 템플릿 목록 (생성일 역순)
     */
    public List<ContentTemplate> getTemplatesByUser(Long userId) {
        logger.debug("사용자별 템플릿 조회 - 사용자 ID: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
        }
        
        return templateRepository.findByCreatedByAndIsActiveTrueOrderByCreatedAtDesc(userOpt.get());
    }
    
    /**
     * 공개 템플릿 목록을 조회합니다.
     * 
     * @return 공개 템플릿 목록 (사용 횟수 순)
     */
    public List<ContentTemplate> getPublicTemplates() {
        logger.debug("공개 템플릿 조회");
        return templateRepository.findByIsPublicTrueAndIsActiveTrueOrderByUsageCountDesc();
    }
    
    /**
     * 인기 템플릿 목록을 조회합니다.
     * 
     * @param limit 조회할 개수
     * @return 인기 템플릿 목록
     */
    public List<ContentTemplate> getPopularTemplates(int limit) {
        logger.debug("인기 템플릿 조회 - 개수: {}", limit);
        
        List<ContentTemplate> popularTemplates = templateRepository.findPopularTemplates();
        return popularTemplates.stream()
                .limit(limit)
                .toList();
    }
    
    /**
     * 최근 생성된 템플릿 목록을 조회합니다.
     * 
     * @return 최근 템플릿 목록 (최대 10개)
     */
    public List<ContentTemplate> getRecentTemplates() {
        logger.debug("최근 템플릿 조회");
        return templateRepository.findTop10ByIsActiveTrueOrderByCreatedAtDesc();
    }
    
    /**
     * 키워드로 템플릿을 검색합니다.
     * 
     * @param keyword 검색 키워드
     * @return 검색된 템플릿 목록
     */
    public List<ContentTemplate> searchTemplates(String keyword) {
        logger.debug("템플릿 검색 - 키워드: {}", keyword);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return templateRepository.searchByKeyword(keyword.trim());
    }
    
    /**
     * 태그로 템플릿을 검색합니다.
     * 
     * @param tags 태그 목록
     * @return 검색된 템플릿 목록
     */
    public List<ContentTemplate> searchTemplatesByTags(List<String> tags) {
        logger.debug("태그별 템플릿 검색 - 태그: {}", tags);
        
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        
        return templateRepository.findByTagsInAndIsActiveTrue(tags);
    }
    
    /**
     * 템플릿을 업데이트합니다.
     * 
     * @param templateId 템플릿 ID
     * @param updatedTemplate 업데이트할 템플릿 정보
     * @param userId 업데이트 요청 사용자 ID
     * @return 업데이트된 템플릿
     */
    public ContentTemplate updateTemplate(String templateId, ContentTemplate updatedTemplate, Long userId) {
        logger.info("템플릿 업데이트 시작 - 템플릿 ID: {}, 사용자 ID: {}", templateId, userId);
        
        try {
            // 기존 템플릿 조회
            Optional<ContentTemplate> existingOpt = templateRepository.findByTemplateIdAndIsActiveTrue(templateId);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + templateId);
            }
            
            ContentTemplate existing = existingOpt.get();
            
            // 권한 확인 (생성자만 수정 가능)
            if (!existing.getCreatedBy().getId().equals(userId)) {
                throw new IllegalArgumentException("템플릿 수정 권한이 없습니다");
            }
            
            // 업데이트 가능한 필드만 수정
            existing.setName(updatedTemplate.getName());
            existing.setDescription(updatedTemplate.getDescription());
            existing.setPromptTemplate(updatedTemplate.getPromptTemplate());
            existing.setCategory(updatedTemplate.getCategory());
            existing.setDefaultParameters(updatedTemplate.getDefaultParameters());
            existing.setTags(updatedTemplate.getTags());
            existing.setPublic(updatedTemplate.isPublic());
            existing.setUpdatedAt(LocalDateTime.now());
            
            ContentTemplate savedTemplate = templateRepository.save(existing);
            logger.info("템플릿 업데이트 완료 - ID: {}", savedTemplate.getId());
            
            return savedTemplate;
            
        } catch (Exception e) {
            logger.error("템플릿 업데이트 실패 - 템플릿 ID: {}", templateId, e);
            throw e;
        }
    }
    
    /**
     * 템플릿을 비활성화합니다 (소프트 삭제).
     * 
     * @param templateId 템플릿 ID
     * @param userId 삭제 요청 사용자 ID
     */
    public void deleteTemplate(String templateId, Long userId) {
        logger.info("템플릿 삭제 시작 - 템플릿 ID: {}, 사용자 ID: {}", templateId, userId);
        
        try {
            // 기존 템플릿 조회
            Optional<ContentTemplate> existingOpt = templateRepository.findByTemplateIdAndIsActiveTrue(templateId);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + templateId);
            }
            
            ContentTemplate existing = existingOpt.get();
            
            // 권한 확인 (생성자만 삭제 가능)
            if (!existing.getCreatedBy().getId().equals(userId)) {
                throw new IllegalArgumentException("템플릿 삭제 권한이 없습니다");
            }
            
            // 소프트 삭제
            existing.setActive(false);
            existing.setUpdatedAt(LocalDateTime.now());
            
            templateRepository.save(existing);
            logger.info("템플릿 삭제 완료 - 템플릿 ID: {}", templateId);
            
        } catch (Exception e) {
            logger.error("템플릿 삭제 실패 - 템플릿 ID: {}", templateId, e);
            throw e;
        }
    }
    
    /**
     * 템플릿 사용 횟수를 증가시킵니다.
     * 
     * @param templateId 템플릿 ID
     */
    public void incrementUsageCount(String templateId) {
        logger.debug("템플릿 사용 횟수 증가 - 템플릿 ID: {}", templateId);
        
        Optional<ContentTemplate> templateOpt = templateRepository.findByTemplateIdAndIsActiveTrue(templateId);
        if (templateOpt.isPresent()) {
            ContentTemplate template = templateOpt.get();
            template.incrementUsageCount();
            templateRepository.save(template);
        }
    }
    
    /**
     * 템플릿 평점을 업데이트합니다.
     * 
     * @param templateId 템플릿 ID
     * @param rating 새로운 평점
     * @param totalRatings 총 평점 수
     */
    public void updateTemplateRating(String templateId, double rating, int totalRatings) {
        logger.debug("템플릿 평점 업데이트 - 템플릿 ID: {}, 평점: {}", templateId, rating);
        
        Optional<ContentTemplate> templateOpt = templateRepository.findByTemplateIdAndIsActiveTrue(templateId);
        if (templateOpt.isPresent()) {
            ContentTemplate template = templateOpt.get();
            template.updateRating(rating, totalRatings);
            templateRepository.save(template);
        }
    }
    
    /**
     * 콘텐츠 타입별 템플릿 수를 조회합니다.
     * 
     * @param contentType 콘텐츠 타입
     * @return 템플릿 수
     */
    public long getTemplateCountByContentType(String contentType) {
        return templateRepository.countByContentTypeAndIsActiveTrue(contentType);
    }
    
    /**
     * 사용자별 템플릿 수를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 템플릿 수
     */
    public long getTemplateCountByUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return 0;
        }
        
        return templateRepository.countByCreatedByAndIsActiveTrue(userOpt.get());
    }
}