package com.Human.Ai.D.makit.domain;

/**
 * 감사 로그에서 추적되는 사용자 작업 유형을 정의하는 열거형
 */
public enum AuditAction {
    // 사용자 관리 작업
    USER_LOGIN("사용자 로그인"),
    USER_LOGOUT("사용자 로그아웃"),
    USER_REGISTER("사용자 등록"),
    USER_UPDATE("사용자 정보 수정"),
    USER_DELETE("사용자 삭제"),
    USER_PASSWORD_CHANGE("비밀번호 변경"),
    USER_ROLE_CHANGE("사용자 역할 변경"),
    
    // 콘텐츠 관리 작업
    CONTENT_CREATE("콘텐츠 생성"),
    CONTENT_UPDATE("콘텐츠 수정"),
    CONTENT_DELETE("콘텐츠 삭제"),
    CONTENT_PUBLISH("콘텐츠 게시"),
    CONTENT_UNPUBLISH("콘텐츠 게시 취소"),
    CONTENT_APPROVE("콘텐츠 승인"),
    CONTENT_REJECT("콘텐츠 거부"),
    CONTENT_GENERATE("AI 콘텐츠 생성"),
    
    // 캠페인 관리 작업
    CAMPAIGN_CREATE("캠페인 생성"),
    CAMPAIGN_UPDATE("캠페인 수정"),
    CAMPAIGN_DELETE("캠페인 삭제"),
    CAMPAIGN_START("캠페인 시작"),
    CAMPAIGN_STOP("캠페인 중지"),
    CAMPAIGN_PAUSE("캠페인 일시정지"),
    CAMPAIGN_RESUME("캠페인 재개"),
    
    // 템플릿 관리 작업
    TEMPLATE_CREATE("템플릿 생성"),
    TEMPLATE_UPDATE("템플릿 수정"),
    TEMPLATE_DELETE("템플릿 삭제"),
    
    // 챗봇 및 지식베이스 작업
    CHATBOT_QUERY("챗봇 질의"),
    KNOWLEDGE_BASE_UPDATE("지식베이스 업데이트"),
    KNOWLEDGE_DOCUMENT_ADD("지식 문서 추가"),
    KNOWLEDGE_DOCUMENT_DELETE("지식 문서 삭제"),
    
    // 분석 및 보고서 작업
    ANALYTICS_VIEW("분석 조회"),
    REPORT_GENERATE("보고서 생성"),
    REPORT_EXPORT("보고서 내보내기"),
    
    // 시스템 관리 작업
    SYSTEM_CONFIG_CHANGE("시스템 설정 변경"),
    CACHE_CLEAR("캐시 초기화"),
    BACKUP_CREATE("백업 생성"),
    BACKUP_RESTORE("백업 복원"),
    
    // 보안 관련 작업
    SECURITY_VIOLATION("보안 위반"),
    ACCESS_DENIED("접근 거부"),
    PERMISSION_GRANT("권한 부여"),
    PERMISSION_REVOKE("권한 취소"),
    
    // API 및 외부 서비스 작업
    API_CALL("API 호출"),
    EXTERNAL_SERVICE_CALL("외부 서비스 호출"),
    FILE_UPLOAD("파일 업로드"),
    FILE_DOWNLOAD("파일 다운로드"),
    
    // 기타 작업
    DATA_EXPORT("데이터 내보내기"),
    DATA_IMPORT("데이터 가져오기"),
    BULK_OPERATION("대량 작업"),
    UNKNOWN("알 수 없는 작업");
    
    private final String description;
    
    AuditAction(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}