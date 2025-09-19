package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.model.ContentTemplate;
import com.Human.Ai.D.makit.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 콘텐츠 템플릿 리포지토리
 * Human.Ai.D MaKIT 플랫폼의 템플릿 데이터 액세스 계층
 */
@Repository
public interface ContentTemplateRepository extends JpaRepository<ContentTemplate, Long> {
    
    /**
     * 템플릿 ID로 활성 템플릿 조회
     */
    Optional<ContentTemplate> findByTemplateIdAndIsActiveTrue(String templateId);
    
    /**
     * 콘텐츠 타입별 활성 템플릿 조회
     */
    List<ContentTemplate> findByContentTypeAndIsActiveTrueOrderByUsageCountDesc(String contentType);
    
    /**
     * 카테고리별 활성 템플릿 조회
     */
    List<ContentTemplate> findByCategoryAndIsActiveTrueOrderByRatingDesc(String category);
    
    /**
     * 공개 템플릿 조회 (사용 횟수 순)
     */
    List<ContentTemplate> findByIsPublicTrueAndIsActiveTrueOrderByUsageCountDesc();
    
    /**
     * 사용자별 템플릿 조회 (생성일 역순)
     */
    List<ContentTemplate> findByCreatedByAndIsActiveTrueOrderByCreatedAtDesc(User createdBy);
    
    /**
     * 태그로 템플릿 검색
     */
    @Query("SELECT DISTINCT t FROM ContentTemplate t JOIN t.tags tag " +
           "WHERE tag IN :tags AND t.isActive = true " +
           "ORDER BY t.rating DESC")
    List<ContentTemplate> findByTagsInAndIsActiveTrue(@Param("tags") List<String> tags);
    
    /**
     * 이름 또는 설명으로 템플릿 검색
     */
    @Query("SELECT t FROM ContentTemplate t " +
           "WHERE (LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND t.isActive = true " +
           "ORDER BY t.rating DESC")
    List<ContentTemplate> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 인기 템플릿 조회 (평점 및 사용 횟수 기준)
     */
    @Query("SELECT t FROM ContentTemplate t " +
           "WHERE t.isActive = true AND t.isPublic = true " +
           "ORDER BY (t.rating * 0.7 + (t.usageCount / 100.0) * 0.3) DESC")
    List<ContentTemplate> findPopularTemplates();
    
    /**
     * 최근 생성된 템플릿 조회
     */
    List<ContentTemplate> findTop10ByIsActiveTrueOrderByCreatedAtDesc();
    
    /**
     * 콘텐츠 타입별 템플릿 수 조회
     */
    long countByContentTypeAndIsActiveTrue(String contentType);
    
    /**
     * 사용자별 템플릿 수 조회
     */
    long countByCreatedByAndIsActiveTrue(User createdBy);
    
    /**
     * 템플릿 ID 존재 여부 확인
     */
    boolean existsByTemplateIdAndIsActiveTrue(String templateId);
}