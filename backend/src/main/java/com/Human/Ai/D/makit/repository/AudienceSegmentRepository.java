package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.AudienceSegment;
import com.Human.Ai.D.makit.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AudienceSegmentRepository extends JpaRepository<AudienceSegment, Long> {
    
    List<AudienceSegment> findByUser(User user);
    
    List<AudienceSegment> findByUserAndStatus(User user, AudienceSegment.SegmentStatus status);
    
    List<AudienceSegment> findByUserAndSegmentType(User user, AudienceSegment.SegmentType segmentType);
    
    @Query("SELECT s FROM AudienceSegment s WHERE s.user = :user AND s.status = 'ACTIVE' " +
           "ORDER BY s.performanceScore DESC")
    List<AudienceSegment> findActiveByUserOrderByPerformance(@Param("user") User user);
    
    @Query("SELECT s FROM AudienceSegment s WHERE s.user.id = :userId " +
           "AND s.performanceScore >= :minScore ORDER BY s.performanceScore DESC")
    List<AudienceSegment> findHighPerformingSegments(@Param("userId") Long userId,
                                                     @Param("minScore") Double minScore);
    
    @Query("SELECT s FROM AudienceSegment s WHERE s.user.id = :userId " +
           "AND s.sizeEstimate >= :minSize ORDER BY s.sizeEstimate DESC")
    List<AudienceSegment> findLargeSegments(@Param("userId") Long userId,
                                           @Param("minSize") Long minSize);
    
    @Query("SELECT s FROM AudienceSegment s WHERE s.lastAnalyzedAt < :cutoffDate " +
           "AND s.status = 'ACTIVE'")
    List<AudienceSegment> findSegmentsNeedingAnalysis(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(s) FROM AudienceSegment s WHERE s.user = :user AND s.status = :status")
    Long countByUserAndStatus(@Param("user") User user, 
                             @Param("status") AudienceSegment.SegmentStatus status);
    
    @Query("SELECT AVG(s.performanceScore) FROM AudienceSegment s WHERE s.user = :user " +
           "AND s.status = 'ACTIVE'")
    Double getAveragePerformanceScore(@Param("user") User user);
    
    @Query("SELECT SUM(s.sizeEstimate) FROM AudienceSegment s WHERE s.user = :user " +
           "AND s.status = 'ACTIVE'")
    Long getTotalActiveAudienceSize(@Param("user") User user);
    
    @Query("SELECT s.segmentType, COUNT(s) FROM AudienceSegment s WHERE s.user = :user " +
           "GROUP BY s.segmentType ORDER BY COUNT(s) DESC")
    List<Object[]> getSegmentTypeDistribution(@Param("user") User user);
    
    @Query("SELECT s FROM AudienceSegment s WHERE s.user = :user " +
           "AND s.createdAt >= :since ORDER BY s.createdAt DESC")
    List<AudienceSegment> findRecentSegments(@Param("user") User user,
                                            @Param("since") LocalDateTime since);
    
    Optional<AudienceSegment> findByUserAndName(User user, String name);
    
    @Query("SELECT s FROM AudienceSegment s WHERE s.user = :user " +
           "AND s.engagementRate >= :minEngagement ORDER BY s.engagementRate DESC")
    List<AudienceSegment> findHighEngagementSegments(@Param("user") User user,
                                                     @Param("minEngagement") Double minEngagement);
}