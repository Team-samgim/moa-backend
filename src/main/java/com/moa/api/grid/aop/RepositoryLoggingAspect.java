package com.moa.api.grid.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Repository 메서드 실행 시간 로깅
 */
@Slf4j
@Aspect
@Component
public class RepositoryLoggingAspect {

    /**
     * Repository 메서드 실행 시간 측정
     */
    @Around("execution(* com.moa.api.grid.repository..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;

            // 1초 이상 걸린 쿼리는 WARN
            if (executionTime > 1000) {
                log.warn("⚠️ SLOW QUERY: {} took {}ms", methodName, executionTime);
            } else if (executionTime > 500) {
                log.info("🐌 {} took {}ms", methodName, executionTime);
            } else {
                log.debug("✅ {} took {}ms", methodName, executionTime);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("❌ {} failed after {}ms: {}",
                    methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}