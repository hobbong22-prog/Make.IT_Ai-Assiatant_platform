# 프론트엔드 Nginx 서버 Dockerfile
FROM nginx:alpine

# 프론트엔드 파일들 복사
COPY frontend/ /usr/share/nginx/html/

# Nginx 설정 파일 생성
RUN echo 'server { \
    listen 80; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    \
    # API 요청을 백엔드로 프록시 \
    location /api/ { \
        proxy_pass http://backend:8080/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto $scheme; \
    } \
    \
    # 정적 파일 서빙 \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80