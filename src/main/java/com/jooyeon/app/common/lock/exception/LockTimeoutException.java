package com.jooyeon.app.common.lock.exception;

/**
 * 락 타임아웃 예외
 * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
 */
public class LockTimeoutException extends LockException {

    public LockTimeoutException(String lockKey, long waitTime) {
        super(String.format("Failed to acquire lock for key '%s' within %d seconds", lockKey, waitTime));
    }

    public LockTimeoutException(String message) {
        super(message);
    }
}