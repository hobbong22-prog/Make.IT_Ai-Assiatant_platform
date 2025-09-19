package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.Content;
import com.Human.Ai.D.makit.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    
    List<Content> findByUser(User user);
    
    List<Content> findByCampaign(Campaign campaign);
    
    List<Content> findByUserAndType(User user, Content.ContentType type);
    
    List<Content> findByUserAndStatus(User user, Content.ContentStatus status);
    
    @Query("SELECT c FROM Content c WHERE c.user = :user AND c.type = :type AND c.status = :status")
    List<Content> findByUserAndTypeAndStatus(
        @Param("user") User user, 
        @Param("type") Content.ContentType type, 
        @Param("status") Content.ContentStatus status
    );
    
    @Query("SELECT COUNT(c) FROM Content c WHERE c.user = :user AND c.status = :status")
    Long countByUserAndStatus(@Param("user") User user, @Param("status") Content.ContentStatus status);
    
    @Query("SELECT c FROM Content c WHERE c.aiModel IS NOT NULL ORDER BY c.createdAt DESC")
    List<Content> findAiGeneratedContent();
    
    // Dashboard-specific methods
    @Query("SELECT COUNT(c) FROM Content c WHERE c.user.id = :userId AND DATE(c.createdAt) = :date")
    int countByUserAndCreatedDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query("SELECT c FROM Content c WHERE c.user.id = :userId AND c.createdAt BETWEEN :startDate AND :endDate")
    List<Content> findByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT c FROM Content c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    List<Content> findRecentByUser(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
    
    default List<Content> findRecentByUser(Long userId, int limit) {
        return findRecentByUser(userId, org.springframework.data.domain.PageRequest.of(0, limit));
    }
}