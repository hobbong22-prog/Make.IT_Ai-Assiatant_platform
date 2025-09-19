package com.Human.Ai.D.makit.config;

import com.Human.Ai.D.makit.domain.Campaign;
import com.Human.Ai.D.makit.domain.Content;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.repository.CampaignRepository;
import com.Human.Ai.D.makit.repository.ContentRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // Create sample users with encrypted passwords
        User user1 = new User("demo_user", "demo@Human.Ai.D.com", passwordEncoder.encode("password123"), "Human.Ai.D", UserRole.ADMIN);
        user1 = userRepository.save(user1);
        
        User user2 = new User("marketer", "marketer@example.com", passwordEncoder.encode("password123"), "Example Corp", UserRole.MARKETING_MANAGER);
        user2 = userRepository.save(user2);
        
        // Create sample campaigns
        Campaign campaign1 = new Campaign("Spring Product Launch", 
                "새로운 제품 출시를 위한 마케팅 캠페인", 
                Campaign.CampaignType.EMAIL, user1);
        campaign1.setStatus(Campaign.CampaignStatus.ACTIVE);
        campaign1.setTargetAudience("기술에 관심있는 20-40대");
        campaign1.setBudget(50000.0);
        campaign1 = campaignRepository.save(campaign1);
        
        Campaign campaign2 = new Campaign("Social Media Engagement", 
                "소셜 미디어 참여도 증대 캠페인", 
                Campaign.CampaignType.SOCIAL_MEDIA, user1);
        campaign2.setStatus(Campaign.CampaignStatus.DRAFT);
        campaign2.setTargetAudience("소셜 미디어 활성 사용자");
        campaign2.setBudget(30000.0);
        campaign2 = campaignRepository.save(campaign2);
        
        // Create sample content
        Content content1 = new Content("AI 마케팅의 미래", Content.ContentType.BLOG_POST, user1);
        content1.setBody("AI 기술이 마케팅 분야에 가져올 혁신적인 변화에 대해 알아보겠습니다...");
        content1.setStatus(Content.ContentStatus.PUBLISHED);
        content1.setCampaign(campaign1);
        contentRepository.save(content1);
        
        Content content2 = new Content("제품 출시 이메일", Content.ContentType.EMAIL_TEMPLATE, user1);
        content2.setBody("안녕하세요! 새로운 제품 출시 소식을 전해드립니다...");
        content2.setStatus(Content.ContentStatus.APPROVED);
        content2.setCampaign(campaign1);
        contentRepository.save(content2);
        
        Content content3 = new Content("소셜 미디어 포스트", Content.ContentType.SOCIAL_MEDIA_POST, user1);
        content3.setBody("🚀 새로운 기능이 출시되었습니다! #MaKIT #AI #Marketing");
        content3.setStatus(Content.ContentStatus.GENERATED);
        content3.setCampaign(campaign2);
        contentRepository.save(content3);
        
        System.out.println("Sample data loaded successfully!");
        System.out.println("Demo user: demo@Human.Ai.D.com / password123");
        System.out.println("H2 Console: http://localhost:8083/h2-console");
    }
}