package com.Human.Ai.D.makit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {
    
    @GetMapping("/")
    @ResponseBody
    public String home() {
        return """
            <html>
            <head>
                <title>MaKIT Platform API</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    .container { max-width: 800px; margin: 0 auto; }
                    .header { text-align: center; margin-bottom: 40px; }
                    .api-section { margin-bottom: 30px; }
                    .endpoint { background: #f5f5f5; padding: 10px; margin: 5px 0; border-radius: 5px; }
                    .method { font-weight: bold; color: #007bff; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚀 MaKIT Platform API</h1>
                        <p>AI-powered Marketing Automation Platform by Human.Ai.D</p>
                    </div>
                    
                    <div class="api-section">
                        <h2>🔐 Authentication API</h2>
                        <div class="endpoint">
                            <span class="method">POST</span> /api/auth/login - 로그인
                        </div>
                        <div class="endpoint">
                            <span class="method">POST</span> /api/auth/register - 회원가입
                        </div>
                        <div class="endpoint">
                            <span class="method">GET</span> /api/auth/me - 현재 사용자 정보
                        </div>
                    </div>
                    
                    <div class="api-section">
                        <h2>📝 Content Generation API</h2>
                        <div class="endpoint">
                            <span class="method">POST</span> /api/content/generate/blog - 블로그 포스트 생성
                        </div>
                        <div class="endpoint">
                            <span class="method">POST</span> /api/content/generate/ad-copy - 광고 문구 생성
                        </div>
                        <div class="endpoint">
                            <span class="method">POST</span> /api/content/generate/social-media - 소셜 미디어 포스트 생성
                        </div>
                        <div class="endpoint">
                            <span class="method">POST</span> /api/content/generate/email - 이메일 템플릿 생성
                        </div>
                    </div>
                    
                    <div class="api-section">
                        <h2>📊 Campaign API</h2>
                        <div class="endpoint">
                            <span class="method">GET</span> /api/campaigns/user/{userId} - 사용자 캠페인 목록
                        </div>
                        <div class="endpoint">
                            <span class="method">POST</span> /api/campaigns - 캠페인 생성
                        </div>
                        <div class="endpoint">
                            <span class="method">GET</span> /api/campaigns/{id}/metrics - 캠페인 메트릭스
                        </div>
                    </div>
                    
                    <div class="api-section">
                        <h2>🤖 Chatbot API</h2>
                        <div class="endpoint">
                            <span class="method">POST</span> /api/chat - AI 챗봇 대화
                        </div>
                        <div class="endpoint">
                            <span class="method">WS</span> /ws - WebSocket 연결
                        </div>
                    </div>
                    
                    <div class="api-section">
                        <h2>🔧 Development Tools</h2>
                        <div class="endpoint">
                            <span class="method">GET</span> <a href="/h2-console" target="_blank">/h2-console</a> - H2 데이터베이스 콘솔
                        </div>
                    </div>
                    
                    <div class="api-section">
                        <h2>👤 Demo Accounts</h2>
                        <div class="endpoint">
                            <strong>관리자:</strong> demo@Human.Ai.D.com / password123
                        </div>
                        <div class="endpoint">
                            <strong>마케터:</strong> marketer@example.com / password123
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
    
    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
    
    @GetMapping("/api")
    @ResponseBody
    public String apiInfo() {
        return """
            {
                "service": "MaKIT Platform API",
                "version": "1.0.0",
                "status": "running",
                "endpoints": {
                    "auth": "/api/auth/*",
                    "content": "/api/content/*",
                    "campaigns": "/api/campaigns/*",
                    "chat": "/api/chat",
                    "websocket": "/ws"
                },
                "demo_accounts": {
                    "admin": "demo@Human.Ai.D.com / password123",
                    "marketer": "marketer@example.com / password123"
                }
            }
            """;
    }
}