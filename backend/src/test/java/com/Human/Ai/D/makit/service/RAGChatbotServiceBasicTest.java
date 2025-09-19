package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RAGChatbotServiceBasicTest {
    
    @Test
    void testServiceInstantiation() {
        // This test verifies that the Spring context can be loaded
        // and all the services can be instantiated without errors
        assertTrue(true, "Spring context loaded successfully");
    }
    
    @Test
    void testUserCreation() {
        // Basic test to verify User entity works
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        
        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
    }
}