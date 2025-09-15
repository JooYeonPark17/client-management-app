package com.jooyeon.app.common.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 분산락 어노테이션
 * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lock {

    /**
     * 락 키 (SpEL 표현식 지원)
     */
    String key();

    /**
     * 락 대기 시간 (기본: 10초)
     */
    long waitTime() default 10;

    /**
     * 락 보유 시간 (기본: 30초)
     */
    long leaseTime() default 30;

    /**
     * 시간 단위 (기본: 초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 재시도 간격 (밀리초)
     */
    long retryInterval() default 100;

    /**
     * 락 획득 실패시 예외 발생 여부
     */
    boolean throwExceptionOnFailure() default true;
}