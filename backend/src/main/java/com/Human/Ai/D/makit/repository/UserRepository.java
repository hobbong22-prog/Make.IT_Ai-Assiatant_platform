package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.campaigns WHERE u.id = :id")
    Optional<User> findByIdWithCampaigns(@Param("id") Long id);
    
    Optional<User> findByCognitoUserId(String cognitoUserId);
    
    List<User> findByUserRoleIn(List<UserRole> roles);
}