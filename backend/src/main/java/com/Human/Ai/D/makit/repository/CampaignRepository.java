package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    
    List<Campaign> findByUser(User user);
    
    List<Campaign> findByUserAndStatus(User user, Campaign.CampaignStatus status);
    
    List<Campaign> findByUserAndType(User user, Campaign.CampaignType type);
    
    @Query("SELECT c FROM Campaign c WHERE c.user = :user AND c.startDate <= :now AND c.endDate >= :now")
    List<Campaign> findActiveCampaignsByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM Campaign c LEFT JOIN FETCH c.contents WHERE c.id = :id")
    Optional<Campaign> findByIdWithContents(@Param("id") Long id);
    
    @Query("SELECT c FROM Campaign c LEFT JOIN FETCH c.metrics WHERE c.id = :id")
    Optional<Campaign> findByIdWithMetrics(@Param("id") Long id);
    
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.user = :user AND c.status = :status")
    Long countByUserAndStatus(@Param("user") User user, @Param("status") Campaign.CampaignStatus status);
    
    // Dashboard-specific methods
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.user.id = :userId AND c.status = 'ACTIVE'")
    int countActiveCampaignsByUser(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Campaign c WHERE c.user.id = :userId AND c.createdAt >= :startDate AND c.createdAt <= :endDate")
    List<Campaign> findByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT c FROM Campaign c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    List<Campaign> findRecentByUser(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
    
    default List<Campaign> findRecentByUser(Long userId, int limit) {
        return findRecentByUser(userId, org.springframework.data.domain.PageRequest.of(0, limit));
    }
    
    // Additional methods for enhanced campaign management
    List<Campaign> findByStatus(Campaign.CampaignStatus status);
    
    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.endDate < :now")
    List<Campaign> findExpiredActiveCampaigns(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM Campaign c WHERE c.createdAt >= :date")
    List<Campaign> findCampaignsCreatedAfter(@Param("date") LocalDateTime date);
}