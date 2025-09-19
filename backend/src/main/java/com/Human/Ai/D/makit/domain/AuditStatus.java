package com.Human.Ai.D.makit.domain;

/**
 * 감사 로그 항목의 상태를 나타내는 열거형
 */
public enum AuditStatus {
    /**
     * 작업이 성공적으로 완료됨
     */
    SUCCESS("성공"),
    
    /**
     * 작업이 실패함
     */
    FAILURE("실패"),
    
    /**
     * 작업이 부분적으로 성공함 (일부 오류 발생)
     */
    PARTIAL_SUCCESS("부분 성공"),
    
    /**
     * 작업이 진행 중임
     */
    IN_PROGRESS("진행 중"),
    
    /**
     * 작업이 취소됨
     */
    CANCELLED("취소됨"),
    
    /**
     * 작업이 시간 초과됨
     */
    TIMEOUT("시간 초과"),
    
    /**
     * 권한 부족으로 작업이 거부됨
     */
    ACCESS_DENIED("접근 거부"),
    
    /**
     * 잘못된 요청으로 작업이 거부됨
     */
    INVALID_REQUEST("잘못된 요청");
    
    private final String description;
    
    AuditStatus(String description) {
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