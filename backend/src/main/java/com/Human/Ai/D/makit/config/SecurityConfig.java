package com.Human.Ai.D.makit.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/", "/index.html", "/login.html", "/dashboard.html").permitAll()
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                
                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Content management endpoints - requires content management permission
                .requestMatchers("/api/content/**").hasAnyRole("ADMIN", "MARKETING_MANAGER", "CONTENT_CREATOR")
                .requestMatchers("/api/content-templates/**").hasAnyRole("ADMIN", "MARKETING_MANAGER", "CONTENT_CREATOR")
                
                // Campaign management endpoints - requires campaign management permission
                .requestMatchers("/api/campaigns/**").hasAnyRole("ADMIN", "MARKETING_MANAGER")
                
                // Analytics endpoints - requires analytics access
                .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "MARKETING_MANAGER", "ANALYST")
                .requestMatchers("/api/campaign-analytics/**").hasAnyRole("ADMIN", "MARKETING_MANAGER", "ANALYST")
                .requestMatchers("/api/optimization/**").hasAnyRole("ADMIN", "MARKETING_MANAGER", "ANALYST")
                .requestMatchers("/api/audience-segmentation/**").hasAnyRole("ADMIN", "MARKETING_MANAGER", "ANALYST")
                
                // Chatbot endpoints - accessible to all authenticated users
                .requestMatchers("/api/chatbot/**").authenticated()
                .requestMatchers("/api/knowledge-base/**").hasAnyRole("ADMIN", "MARKETING_MANAGER", "CONTENT_CREATOR")
                
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())); // H2 콘솔을 위해 필요
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}