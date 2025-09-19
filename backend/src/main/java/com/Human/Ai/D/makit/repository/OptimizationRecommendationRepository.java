package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.OptimizationRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OptimizationRecommendationRepository extends JpaRepository<OptimizationRecommendation, Long> {
    
    List<OptimizationRecommendation> findByCampaign(Campaign campaign);
    
    List<OptimizationRecommendation> findByCampaignAndStatus(Campaign campaign, 
                                                           OptimizationRecommendation.RecommendationStatus status);
    
    List<OptimizationRecommendation> findByCampaignAndPriority(Campaign campaign, 
                                                              OptimizationRecommendation.RecommendationPriority priority);
    
    List<OptimizationRecommendation> findByCampaignAndCategory(Campaign campaign, 
                                                              OptimizationRecommendation.RecommendationCategory category);
    
    @Query("SELECT r FROM OptimizationRecommendation r WHERE r.campaign.user.id = :userId " +
           "AND r.status = :status ORDER BY r.priority DESC, r.expectedImpact DESC")
    List<OptimizationRecommendation> findByUserAndStatus(@Param("userId") Long userId,
                                                         @Param("status") OptimizationRecommendation.RecommendationStatus status);
    
    @Query("SELECT r FROM OptimizationRecommendation r WHERE r.campaign.user.id = :userId " +
           "AND r.priority IN :priorities ORDER BY r.priority DESC, r.expectedImpact DESC")
    List<OptimizationRecommendation> findByUserAndPriorities(@Param("userId") Long userId,
                                                             @Param("priorities") List<OptimizationRecommendation.RecommendationPriority> priorities);
    
    @Query("SELECT r FROM OptimizationRecommendation r WHERE r.expiresAt < :now AND r.status = 'PENDING'")
    List<OptimizationRecommendation> findExpiredRecommendations(@Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM OptimizationRecommendation r WHERE r.campaign = :campaign " +
           "AND r.status = 'PENDING' ORDER BY r.priority DESC, r.expectedImpact DESC")
    List<OptimizationRecommendation> findActiveByCampaign(@Param("campaign") Campaign campaign);
    
    @Query("SELECT COUNT(r) FROM OptimizationRecommendation r WHERE r.campaign.user.id = :userId " +
           "AND r.status = :status")
    Long countByUserAndStatus(@Param("userId") Long userId,
                             @Param("status") OptimizationRecommendation.RecommendationStatus status);
    
    @Query("SELECT r FROM OptimizationRecommendation r WHERE r.campaign.user.id = :userId " +
           "AND r.generatedAt >= :since ORDER BY r.generatedAt DESC")
    List<OptimizationRecommendation> findRecentByUser(@Param("userId") Long userId,
                                                      @Param("since") LocalDateTime since);
    
    @Query("SELECT r FROM OptimizationRecommendation r WHERE r.campaign = :campaign " +
           "AND r.recommendationType = :type AND r.status != 'DISMISSED' " +
           "ORDER BY r.generatedAt DESC LIMIT 1")
    OptimizationRecommendation findLatestByTypeAndCampaign(@Param("campaign") Campaign campaign,
                                                          @Param("type") String type);
    
    @Query("SELECT AVG(r.expectedImpact) FROM OptimizationRecommendation r WHERE r.campaign.user.id = :userId " +
           "AND r.status = 'IMPLEMENTED'")
    Double getAverageImplementedImpact(@Param("userId") Long userId);
    
    @Query("SELECT r.category, COUNT(r) FROM OptimizationRecommendation r WHERE r.campaign.user.id = :userId " +
           "GROUP BY r.category ORDER BY COUNT(r) DESC")
    List<Object[]> getRecommendationCategoryStats(@Param("userId") Long userId);
    
    // Dashboard-specific methods
    @Query("SELECT COUNT(r) FROM OptimizationRecommendation r WHERE r.campaign.user.id = :userId AND r.status = 'PENDING'")
    int countPendingByUser(@Param("userId") Long userId);
    
    @Query("SELECT r FROM OptimizationRecommendation r WHERE r.campaign.user.id = :userId " +
           "ORDER BY r.priority DESC, r.generatedAt DESC")
    List<OptimizationRecommendation> findTopByUserOrderByPriorityAndGeneratedAt(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
    
    default List<OptimizationRecommendation> findTopByUserOrderByPriorityAndGeneratedAt(Long userId, int limit) {
        return findTopByUserOrderByPriorityAndGeneratedAt(userId, org.springframework.data.domain.PageRequest.of(0, limit));
    }
}