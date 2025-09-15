package com.jooyeon.app.common.lock.exception;

/**
 * 분산락 관련 예외
 * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
 */
public class LockException extends RuntimeException {

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }
}