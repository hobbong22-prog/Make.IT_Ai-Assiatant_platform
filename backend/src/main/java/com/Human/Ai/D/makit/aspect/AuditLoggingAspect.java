package com.Human.Ai.D.makit.aspect;

import com.Human.Ai.D.makit.domain.AuditAction;
import com.Human.Ai.D.makit.service.AuditLoggingService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 서비스 메서드 호출에 대한 자동 감사 로깅을 제공하는 AOP Aspect
 * 
 * 이 Aspect는 @Auditable 어노테이션이 붙은 메서드나 특정 패턴의 메서드에 대해
 * 자동으로 감사 로그를 생성합니다.
 */
@Aspect
@Component
public class AuditLoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingAspect.class);
    
    @Autowired
    private AuditLoggingService auditLoggingService;
    
    /**
     * @Auditable 어노테이션이 붙은 메서드에 대한 포인트컷
     */
    @Pointcut("@annotation(com.Human.Ai.D.makit.annotation.Auditable)")
    public void auditableMethod() {}
    
    /**
     * 서비스 계층의 주요 비즈니스 메서드에 대한 포인트컷
     */
    @Pointcut("execution(* com.Human.Ai.D.makit.service.*Service.create*(..)) || " +
              "execution(* com.Human.Ai.D.makit.service.*Service.update*(..)) || " +
              "execution(* com.Human.Ai.D.makit.service.*Service.delete*(..)) || " +
              "execution(* com.Human.Ai.D.makit.service.*Service.approve*(..)) || " +
              "execution(* com.Human.Ai.D.makit.service.*Service.reject*(..)) || " +
              "execution(* com.Human.Ai.D.makit.service.*Service.publish*(..)) || " +
              "execution(* com.Human.Ai.D.makit.service.*Service.generate*(..))")
    public void businessMethods() {}
    
    /**
     * 인증 관련 메서드에 대한 포인트컷
     */
    @Pointcut("execution(* com.Human.Ai.D.makit.service.AuthService.login*(..)) || " +
              "execution(* com.Human.Ai.D.makit.service.AuthService.logout*(..)) || " +
              "execution(* com.Human.Ai.D.makit.service.AuthService.register*(..)) || " +
              "execution(* com.Human.Ai.D.makit.service.CognitoAuthenticationService.*(..))")
    public void authenticationMethods() {}
    
    /**
     * @Auditable 어노테이션이 붙은 메서드 실행 전후로 감사 로그를 기록합니다.
     */
    @Around("auditableMethod()")
    public Object auditAnnotatedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            // 메서드 실행 전 로깅
            logger.debug("감사 대상 메서드 시작: {}.{}", className, methodName);
            
            // @Auditable 어노테이션에서 정보 추출
            Method method = getMethod(joinPoint);
            com.Human.Ai.D.makit.annotation.Auditable auditable = method.getAnnotation(com.Human.Ai.D.makit.annotation.Auditable.class);
            
            Object result = joinPoint.proceed();
            
            // 성공적인 실행 후 감사 로그 기록
            long executionTime = System.currentTimeMillis() - startTime;
            recordAuditLog(auditable, joinPoint, result, executionTime, null);
            
            return result;
            
        } catch (Exception e) {
            // 실패한 실행에 대한 감사 로그 기록
            long executionTime = System.currentTimeMillis() - startTime;
            Method method = getMethod(joinPoint);
            com.Human.Ai.D.makit.annotation.Auditable auditable = method.getAnnotation(com.Human.Ai.D.makit.annotation.Auditable.class);
            recordAuditLog(auditable, joinPoint, null, executionTime, e);
            
            throw e;
        }
    }
    
    /**
     * 비즈니스 메서드 실행 후 감사 로그를 기록합니다.
     */
    @AfterReturning(pointcut = "businessMethods()", returning = "result")
    public void auditBusinessMethodSuccess(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            
            AuditAction action = determineActionFromMethodName(methodName);
            String entityType = extractEntityTypeFromClassName(className);
            String entityId = extractEntityIdFromResult(result);
            String description = String.format("%s.%s 메서드 실행 완료", className, methodName);
            
            Map<String, String> details = new HashMap<>();
            details.put("method", methodName);
            details.put("class", className);
            details.put("arguments", Arrays.toString(joinPoint.getArgs()));
            
            auditLoggingService.log(action, entityType, entityId, description, details);
            
        } catch (Exception e) {
            logger.error("비즈니스 메서드 감사 로깅 실패", e);
        }
    }
    
    /**
     * 비즈니스 메서드 실행 실패 시 감사 로그를 기록합니다.
     */
    @AfterThrowing(pointcut = "businessMethods()", throwing = "exception")
    public void auditBusinessMethodFailure(JoinPoint joinPoint, Throwable exception) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            
            AuditAction action = determineActionFromMethodName(methodName);
            String entityType = extractEntityTypeFromClassName(className);
            String description = String.format("%s.%s 메서드 실행 실패", className, methodName);
            
            auditLoggingService.logFailure(action, entityType, null, description, exception.getMessage());
            
        } catch (Exception e) {
            logger.error("비즈니스 메서드 실패 감사 로깅 실패", e);
        }
    }
    
    /**
     * 인증 메서드 실행 후 감사 로그를 기록합니다.
     */
    @AfterReturning(pointcut = "authenticationMethods()", returning = "result")
    public void auditAuthenticationSuccess(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            AuditAction action = determineAuthActionFromMethodName(methodName);
            String description = String.format("인증 작업 성공: %s", methodName);
            
            auditLoggingService.log(action, "USER", null, description);
            
        } catch (Exception e) {
            logger.error("인증 메서드 감사 로깅 실패", e);
        }
    }
    
    /**
     * 인증 메서드 실행 실패 시 감사 로그를 기록합니다.
     */
    @AfterThrowing(pointcut = "authenticationMethods()", throwing = "exception")
    public void auditAuthenticationFailure(JoinPoint joinPoint, Throwable exception) {
        try {
            String methodName = joinPoint.getSignature().getName();
            AuditAction action = determineAuthActionFromMethodName(methodName);
            String description = String.format("인증 작업 실패: %s", methodName);
            
            auditLoggingService.logFailure(action, "USER", null, description, exception.getMessage());
            
        } catch (Exception e) {
            logger.error("인증 메서드 실패 감사 로깅 실패", e);
        }
    }
    
    // 유틸리티 메서드들
    
    /**
     * JoinPoint에서 Method 객체를 추출합니다.
     */
    private Method getMethod(JoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Class<?>[] parameterTypes = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterTypes();
            return joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes);
        } catch (Exception e) {
            logger.warn("메서드 추출 실패", e);
            return null;
        }
    }
    
    /**
     * @Auditable 어노테이션 정보를 기반으로 감사 로그를 기록합니다.
     */
    private void recordAuditLog(com.Human.Ai.D.makit.annotation.Auditable auditable, 
                               JoinPoint joinPoint, Object result, long executionTime, Exception exception) {
        try {
            if (auditable == null) return;
            
            AuditAction action = auditable.action();
            String entityType = auditable.entityType().isEmpty() ? 
                               extractEntityTypeFromClassName(joinPoint.getTarget().getClass().getSimpleName()) : 
                               auditable.entityType();
            String entityId = extractEntityIdFromResult(result);
            String description = auditable.description().isEmpty() ? 
                               String.format("%s 실행", joinPoint.getSignature().getName()) : 
                               auditable.description();
            
            Map<String, String> details = new HashMap<>();
            details.put("executionTime", executionTime + "ms");
            details.put("method", joinPoint.getSignature().getName());
            details.put("class", joinPoint.getTarget().getClass().getSimpleName());
            
            if (exception != null) {
                auditLoggingService.logFailure(action, entityType, entityId, description, exception.getMessage());
            } else {
                auditLoggingService.log(action, entityType, entityId, description, details);
            }
            
        } catch (Exception e) {
            logger.error("어노테이션 기반 감사 로깅 실패", e);
        }
    }
    
    /**
     * 메서드 이름에서 AuditAction을 결정합니다.
     */
    private AuditAction determineActionFromMethodName(String methodName) {
        String lowerMethodName = methodName.toLowerCase();
        
        if (lowerMethodName.startsWith("create")) return AuditAction.CONTENT_CREATE;
        if (lowerMethodName.startsWith("update")) return AuditAction.CONTENT_UPDATE;
        if (lowerMethodName.startsWith("delete")) return AuditAction.CONTENT_DELETE;
        if (lowerMethodName.startsWith("approve")) return AuditAction.CONTENT_APPROVE;
        if (lowerMethodName.startsWith("reject")) return AuditAction.CONTENT_REJECT;
        if (lowerMethodName.startsWith("publish")) return AuditAction.CONTENT_PUBLISH;
        if (lowerMethodName.startsWith("generate")) return AuditAction.CONTENT_GENERATE;
        
        return AuditAction.UNKNOWN;
    }
    
    /**
     * 인증 메서드 이름에서 AuditAction을 결정합니다.
     */
    private AuditAction determineAuthActionFromMethodName(String methodName) {
        String lowerMethodName = methodName.toLowerCase();
        
        if (lowerMethodName.contains("login")) return AuditAction.USER_LOGIN;
        if (lowerMethodName.contains("logout")) return AuditAction.USER_LOGOUT;
        if (lowerMethodName.contains("register")) return AuditAction.USER_REGISTER;
        
        return AuditAction.UNKNOWN;
    }
    
    /**
     * 클래스 이름에서 엔티티 타입을 추출합니다.
     */
    private String extractEntityTypeFromClassName(String className) {
        if (className.contains("Content")) return "CONTENT";
        if (className.contains("Campaign")) return "CAMPAIGN";
        if (className.contains("User")) return "USER";
        if (className.contains("Template")) return "TEMPLATE";
        if (className.contains("Knowledge")) return "KNOWLEDGE";
        if (className.contains("Analytics")) return "ANALYTICS";
        
        return className.toUpperCase().replace("SERVICE", "");
    }
    
    /**
     * 결과 객체에서 엔티티 ID를 추출합니다.
     */
    private String extractEntityIdFromResult(Object result) {
        if (result == null) return null;
        
        try {
            // ID 필드가 있는 경우 추출 시도
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            // ID 추출 실패 시 null 반환
            return null;
        }
    }
}