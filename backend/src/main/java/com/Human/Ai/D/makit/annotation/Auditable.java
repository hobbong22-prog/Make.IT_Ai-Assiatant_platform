package com.Human.Ai.D.makit.annotation;

import com.Human.Ai.D.makit.domain.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드에 감사 로깅을 적용하기 위한 어노테이션
 * 
 * 이 어노테이션이 붙은 메서드는 AuditLoggingAspect에 의해
 * 자동으로 감사 로그가 기록됩니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    
    /**
     * 감사 로그에 기록될 작업 유형
     */
    AuditAction action();
    
    /**
     * 작업 대상 엔티티 타입 (선택사항)
     * 지정하지 않으면 클래스 이름에서 자동 추출
     */
    String entityType() default "";
    
    /**
     * 감사 로그 설명 (선택사항)
     * 지정하지 않으면 메서드 이름 기반으로 자동 생성
     */
    String description() default "";
    
    /**
     * 실패 시에도 로그를 기록할지 여부
     */
    boolean logFailures() default true;
    
    /**
     * 성공 시에도 로그를 기록할지 여부
     */
    boolean logSuccess() default true;
}