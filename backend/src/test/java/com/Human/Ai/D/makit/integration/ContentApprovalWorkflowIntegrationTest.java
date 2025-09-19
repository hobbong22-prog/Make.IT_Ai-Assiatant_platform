package com.Human.Ai.D.makit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.dto.ApprovalActionRequest;
import com.Human.Ai.D.makit.dto.ContentApprovalRequest;
import com.Human.Ai.D.makit.repository.ContentApprovalRepository;
import com.Human.Ai.D.makit.repository.ContentRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class ContentApprovalWorkflowIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ContentApprovalRepository contentApprovalRepository;
    
    private User contentCreator;
    private User marketingManager;
    private User admin;
    private Content testContent;
    
    @BeforeEach
    void setUp() {
        // Create test users
        contentCreator = new User();
        contentCreator.setUsername("content_creator");
        contentCreator.setEmail("creator@test.com");
        contentCreator.setRole(UserRole.CONTENT_CREATOR);
        contentCreator.setActive(true);
        contentCreator = userRepository.save(contentCreator);
        
        marketingManager = new User();
        marketingManager.setUsername("marketing_manager");
        marketingManager.setEmail("manager@test.com");
        marketingManager.setRole(UserRole.MARKETING_MANAGER);
        marketingManager.setActive(true);
        marketingManager = userRepository.save(marketingManager);
        
        admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin = userRepository.save(admin);
        
        // Create test content
        testContent = new Content();
        testContent.setTitle("Test Marketing Content");
        testContent.setContent("This is test marketing content for approval workflow testing.");
        testContent.setType(Content.ContentType.BLOG_POST);
        testContent.setCreatedBy(contentCreator);
        testContent = contentRepository.save(testContent);
    }
    
    @Test
    @WithMockUser(username = "content_creator", roles = {"CONTENT_CREATOR"})
    void testCompleteApprovalWorkflow() throws Exception {
        // 1. Submit content for approval
        ContentApprovalRequest submitRequest = new ContentApprovalRequest(
            testContent.getId(), 
            "Please review this content for our upcoming campaign", 
            Priority.HIGH
        );
        
        String submitResponse = mockMvc.perform(post("/api/content-approvals/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentId", is(testContent.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.submissionNotes", is("Please review this content for our upcoming campaign")))
                .andReturn().getResponse().getContentAsString();
        
        // Extract approval ID from response
        Long approvalId = objectMapper.readTree(submitResponse).get("id").asLong();
        
        // 2. Start review (as marketing manager)
        mockMvc.perform(post("/api/content-approvals/{approvalId}/start-review", approvalId)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user("marketing_manager").roles("MARKETING_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_REVIEW")))
                .andExpect(jsonPath("$.currentReviewerUsername", is("marketing_manager")));
        
        // 3. Request revision
        ApprovalActionRequest revisionRequest = new ApprovalActionRequest(
            "Please add more details about the target audience and include call-to-action."
        );
        
        mockMvc.perform(post("/api/content-approvals/{approvalId}/request-revision", approvalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(revisionRequest))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user("marketing_manager").roles("MARKETING_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NEEDS_REVISION")))
                .andExpect(jsonPath("$.reviewerComments", 
                    is("Please add more details about the target audience and include call-to-action.")));
        
        // 4. Resubmit after revision (as content creator)
        mockMvc.perform(post("/api/content-approvals/{approvalId}/resubmit", approvalId)
                .contentType(MediaType.TEXT_PLAIN)
                .content("Added target audience details and call-to-action as requested")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user("content_creator").roles("CONTENT_CREATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")));
        
        // 5. Start review again
        mockMvc.perform(post("/api/content-approvals/{approvalId}/start-review", approvalId)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user("marketing_manager").roles("MARKETING_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_REVIEW")));
        
        // 6. Approve content
        ApprovalActionRequest approvalRequest = new ApprovalActionRequest(
            "Content looks great! Approved for publication."
        );
        
        mockMvc.perform(post("/api/content-approvals/{approvalId}/approve", approvalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user("marketing_manager").roles("MARKETING_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.reviewerComments", is("Content looks great! Approved for publication.")))
                .andExpect(jsonPath("$.history", hasSize(greaterThan(0))));
    }
    
    @Test
    @WithMockUser(username = "content_creator", roles = {"CONTENT_CREATOR"})
    void testContentRejectionWorkflow() throws Exception {
        // Submit content for approval
        ContentApprovalRequest submitRequest = new ContentApprovalRequest(
            testContent.getId(), 
            "Please review this content", 
            Priority.MEDIUM
        );
        
        String submitResponse = mockMvc.perform(post("/api/content-approvals/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        
        Long approvalId = objectMapper.readTree(submitResponse).get("id").asLong();
        
        // Start review
        mockMvc.perform(post("/api/content-approvals/{approvalId}/start-review", approvalId)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        
        // Reject content
        ApprovalActionRequest rejectionRequest = new ApprovalActionRequest(
            "Content does not meet our quality standards and brand guidelines."
        );
        
        mockMvc.perform(post("/api/content-approvals/{approvalId}/reject", approvalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectionRequest))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.reviewerComments", 
                    is("Content does not meet our quality standards and brand guidelines.")));
    }
    
    @Test
    @WithMockUser(username = "marketing_manager", roles = {"MARKETING_MANAGER"})
    void testGetMyReviews() throws Exception {
        // Create and submit content for approval
        ContentApproval approval = new ContentApproval(testContent, contentCreator, "Test submission", Priority.HIGH);
        approval.setCurrentReviewer(marketingManager);
        contentApprovalRepository.save(approval);
        
        // Get reviews for marketing manager
        mockMvc.perform(get("/api/content-approvals/my-reviews")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].currentReviewerUsername", is("marketing_manager")));
    }
    
    @Test
    @WithMockUser(username = "content_creator", roles = {"CONTENT_CREATOR"})
    void testGetMySubmissions() throws Exception {
        // Create and submit content for approval
        ContentApproval approval = new ContentApproval(testContent, contentCreator, "Test submission", Priority.MEDIUM);
        contentApprovalRepository.save(approval);
        
        // Get submissions for content creator
        mockMvc.perform(get("/api/content-approvals/my-submissions")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].submittedByUsername", is("content_creator")));
    }
    
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetOverdueApprovals() throws Exception {
        // Create overdue approval (set due date in the past)
        ContentApproval approval = new ContentApproval(testContent, contentCreator, "Test submission", Priority.HIGH);
        approval.setDueDate(java.time.LocalDateTime.now().minusDays(1));
        contentApprovalRepository.save(approval);
        
        // Get overdue approvals
        mockMvc.perform(get("/api/content-approvals/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].overdue", is(true)));
    }
    
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetApprovalStatistics() throws Exception {
        // Create various approvals with different statuses
        ContentApproval pendingApproval = new ContentApproval(testContent, contentCreator, "Pending", Priority.MEDIUM);
        contentApprovalRepository.save(pendingApproval);
        
        ContentApproval approvedApproval = new ContentApproval(testContent, contentCreator, "Approved", Priority.LOW);
        approvedApproval.transitionTo(ApprovalStatus.APPROVED, admin, "Approved");
        contentApprovalRepository.save(approvedApproval);
        
        // Get statistics
        mockMvc.perform(get("/api/content-approvals/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApprovals", greaterThan(0)))
                .andExpect(jsonPath("$.statusCounts", notNullValue()))
                .andExpect(jsonPath("$.priorityCounts", notNullValue()));
    }
    
    @Test
    @WithMockUser(username = "content_creator", roles = {"CONTENT_CREATOR"})
    void testCancelApproval() throws Exception {
        // Submit content for approval
        ContentApprovalRequest submitRequest = new ContentApprovalRequest(
            testContent.getId(), 
            "Test submission", 
            Priority.LOW
        );
        
        String submitResponse = mockMvc.perform(post("/api/content-approvals/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        
        Long approvalId = objectMapper.readTree(submitResponse).get("id").asLong();
        
        // Cancel approval
        mockMvc.perform(post("/api/content-approvals/{approvalId}/cancel", approvalId)
                .contentType(MediaType.TEXT_PLAIN)
                .content("No longer needed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.reviewerComments", is("No longer needed")));
    }
    
    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void testUnauthorizedAccess() throws Exception {
        // Try to submit content as viewer (should fail)
        ContentApprovalRequest submitRequest = new ContentApprovalRequest(
            testContent.getId(), 
            "Unauthorized submission", 
            Priority.MEDIUM
        );
        
        mockMvc.perform(post("/api/content-approvals/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isForbidden());
        
        // Try to access reviewer endpoints as viewer (should fail)
        mockMvc.perform(get("/api/content-approvals/my-reviews"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/api/content-approvals/overdue"))
                .andExpect(status().isForbidden());
    }
}