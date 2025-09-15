package com.jooyeon.app.common.lock;

import com.jooyeon.app.common.lock.exception.LockTimeoutException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 분산락 AOP Aspect
 * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
 */
@Aspect
@Component
@Slf4j
public class LockAspect {


    private final RedisLockService redisLockService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public LockAspect(RedisLockService redisLockService) {
        this.redisLockService = redisLockService;
    }

    /**
     * @Lock 어노테이션이 적용된 메서드에 대한 Around Advice
     * Redis 분산락의 표준 패턴을 구현:
     * 1. 락 획득 시도 (SET key value PX leaseTime NX)
     * 2. 비즈니스 로직 실행
     * 3. 락 해제 (DEL key if value matches)
     */
    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint joinPoint, Lock lock) throws Throwable {
        String lockKey = generateLockKey(joinPoint, lock.key());

        log.debug("[REDIS-락-AOP] 락이 적용된 메소드 처리: {}, 키: {}",
                    joinPoint.getSignature().toShortString(), lockKey);

        boolean lockAcquired = redisLockService.tryLock(
            lockKey,
            lock.waitTime(),
            lock.leaseTime(),
            lock.timeUnit()
        );

        if (!lockAcquired) {
            if (lock.throwExceptionOnFailure()) {
                throw new LockTimeoutException(lockKey, lock.waitTime());
            } else {
                log.warn("[REDIS-락-AOP] 락 획득 실패, 락 없이 진행: {}", lockKey);
                return joinPoint.proceed();
            }
        }

        try {
            log.debug("[REDIS-락-AOP] 락 획득, 비즈니스 로직 실행: {}", lockKey);
            return joinPoint.proceed();
        } finally {
            try {
                redisLockService.unlock(lockKey);
                log.debug("[REDIS-락-AOP] 락 해제 성공: {}", lockKey);
            } catch (Exception e) {
                log.error("[REDIS-락-AOP] 락 해제 오류: {}", lockKey, e);
            }
        }
    }

    /**
     * SpEL 표현식을 사용하여 동적 락 키 생성
     * Redis 키 명명 규칙을 따름: prefix:identifier:suffix
     */
    private String generateLockKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        try {
            Expression expression = expressionParser.parseExpression(keyExpression);
            EvaluationContext context = createEvaluationContext(joinPoint);

            String evaluatedKey = expression.getValue(context, String.class);

            return "lock:" + evaluatedKey;

        } catch (Exception e) {
            log.warn("[REDIS-락-AOP] SpEL 표현식 '{}' 평가 실패, 리터럴로 사용: {}",
                       keyExpression, e.getMessage());
            return "lock:" + keyExpression;
        }
    }

    /**
     * SpEL 평가 컨텍스트 생성
     * 메서드 파라미터와 값을 컨텍스트에 추가
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length && i < args.length; i++) {
            context.setVariable(parameters[i].getName(), args[i]);
            context.setVariable("p" + i, args[i]);
        }

        context.setVariable("methodName", method.getName());
        context.setVariable("className", method.getDeclaringClass().getSimpleName());

        return context;
    }
}