package com.jooyeon.app.common.lock;

import java.util.concurrent.TimeUnit;

/**
 * Redis 분산락 서비스 인터페이스
 * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
 */
public interface RedisLockService {

    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit);

    void unlock(String lockKey);

    boolean isLocked(String lockKey);

    void forceUnlock(String lockKey);
}