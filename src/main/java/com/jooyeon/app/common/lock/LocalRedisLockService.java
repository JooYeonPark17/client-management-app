package com.jooyeon.app.common.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.*;

/**
 * 로컬 메모리 기반 분산락 구현체
 * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
 *
 * Local in-memory distributed lock implementation
 * This is a local implementation for demonstration purposes
 * In distributed environment, Redis distributed lock should be used
 *
 * Redis 분산락의 주요 특징들을 모방:
 * - SET key value PX milliseconds NX (Redis 명령어 모방)
 * - Lua 스크립트를 통한 원자적 연산 (로컬에서는 synchronized 블록으로 구현)
 * - 락 만료 시간 관리
 * - 재시도 메커니즘
 */
@Service
@Slf4j
public class LocalRedisLockService implements RedisLockService {


    // Redis의 메모리 저장소를 모방하는 ConcurrentHashMap
    // In real Redis implementation, this would be Redis server memory
    private final ConcurrentHashMap<String, LockInfo> lockStore = new ConcurrentHashMap<>();

    // Redis의 키 만료 기능을 모방하는 스케줄러
    // In real Redis implementation, Redis handles TTL automatically
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    static class LockInfo {
        private final String threadId;
        private final long expirationTime;
        private volatile ScheduledFuture<?> expirationTask;

        public LockInfo(String threadId, long expirationTime) {
            this.threadId = threadId;
            this.expirationTime = expirationTime;
        }

        public String getThreadId() { return threadId; }
        public long getExpirationTime() { return expirationTime; }
        public ScheduledFuture<?> getExpirationTask() { return expirationTask; }
        public void setExpirationTask(ScheduledFuture<?> task) { this.expirationTask = task; }
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        // Redis의 SET key value PX milliseconds NX 명령어와 동일한 동작
        // Equivalent to Redis: SET lockKey threadId PX leaseTimeMs NX

        String currentThreadId = getCurrentThreadId();
        long waitTimeMs = timeUnit.toMillis(waitTime);
        long leaseTimeMs = timeUnit.toMillis(leaseTime);
        Instant startTime = Instant.now();

        log.debug("[REDIS-락] 락 획득 시도: key={}, thread={}, 대기시간={}ms, 임대시간={}ms",
                    lockKey, currentThreadId, waitTimeMs, leaseTimeMs);

        while (Instant.now().toEpochMilli() - startTime.toEpochMilli() < waitTimeMs) {
            if (acquireLock(lockKey, currentThreadId, leaseTimeMs)) {
                log.debug("[REDIS-락] 락 획득 성공: key={}, thread={}", lockKey, currentThreadId);
                return true;
            }

            try {
                // Redis 클라이언트의 재시도 간격을 모방
                // Mimic Redis client retry interval
                Thread.sleep(100); // 100ms retry interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[REDIS-락] 락 획득 중단됨: key={}, thread={}", lockKey, currentThreadId);
                return false;
            }
        }

        log.debug("[REDIS-락] 락 획득 타임아웃: key={}, thread={}, 대기시간={}ms",
                    lockKey, currentThreadId, waitTimeMs);
        return false;
    }

    /**
     * Redis의 원자적 연산을 모방하는 락 획득 메서드
     * Atomic lock acquisition mimicking Redis Lua script behavior
     */
    private synchronized boolean acquireLock(String lockKey, String threadId, long leaseTimeMs) {
        // Redis Lua 스크립트와 동일한 로직:
        // if redis.call('exists', lockKey) == 0 then
        //     redis.call('set', lockKey, threadId, 'px', leaseTimeMs)
        //     return 1
        // else
        //     return 0
        // end

        LockInfo existingLock = lockStore.get(lockKey);

        // 기존 락이 없거나 만료된 경우
        if (existingLock == null || isExpired(existingLock)) {
            if (existingLock != null && existingLock.getExpirationTask() != null) {
                existingLock.getExpirationTask().cancel(false);
            }

            LockInfo newLock = new LockInfo(threadId, Instant.now().toEpochMilli() + leaseTimeMs);
            lockStore.put(lockKey, newLock);

            // Redis의 TTL 기능을 모방하는 만료 스케줄링
            // Schedule expiration task to mimic Redis TTL
            ScheduledFuture<?> expirationTask = scheduler.schedule(
                () -> expireLock(lockKey, newLock),
                leaseTimeMs,
                TimeUnit.MILLISECONDS
            );
            newLock.setExpirationTask(expirationTask);

            return true;
        }

        // 같은 스레드가 이미 락을 보유한 경우 (재진입 락)
        if (threadId.equals(existingLock.getThreadId())) {
            log.debug("[REDIS-락] 재진입 락 감지: key={}, thread={}", lockKey, threadId);
            return true;
        }

        return false;
    }

    @Override
    public void unlock(String lockKey) {
        // Redis의 Lua 스크립트와 동일한 로직:
        // if redis.call('get', lockKey) == threadId then
        //     redis.call('del', lockKey)
        //     return 1
        // else
        //     return 0
        // end

        String currentThreadId = getCurrentThreadId();
        log.debug("[REDIS-락] 락 해제 시도: key={}, thread={}", lockKey, currentThreadId);

        synchronized (this) {
            LockInfo lockInfo = lockStore.get(lockKey);
            if (lockInfo != null && currentThreadId.equals(lockInfo.getThreadId())) {
                if (lockInfo.getExpirationTask() != null) {
                    lockInfo.getExpirationTask().cancel(false);
                }
                lockStore.remove(lockKey);
                log.debug("[REDIS-락] 락 해제 성공: key={}, thread={}", lockKey, currentThreadId);
            } else {
                log.warn("[REDIS-락] 현재 스레드가 소유하지 않은 락 해제 시도: key={}, thread={}",
                           lockKey, currentThreadId);
            }
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        // Redis의 EXISTS 명령어와 동일한 동작
        LockInfo lockInfo = lockStore.get(lockKey);
        return lockInfo != null && !isExpired(lockInfo);
    }

    @Override
    public void forceUnlock(String lockKey) {
        // Redis의 DEL 명령어와 동일한 동작 (관리자용)
        log.warn("[REDIS-락] 강제 락 해제: {}", lockKey);
        synchronized (this) {
            LockInfo lockInfo = lockStore.remove(lockKey);
            if (lockInfo != null && lockInfo.getExpirationTask() != null) {
                lockInfo.getExpirationTask().cancel(false);
            }
        }
    }


    private boolean isExpired(LockInfo lockInfo) {
        return Instant.now().toEpochMilli() > lockInfo.getExpirationTime();
    }

    private void expireLock(String lockKey, LockInfo lockInfo) {
        // Redis의 자동 만료 기능을 모방
        // Mimic Redis automatic expiration
        synchronized (this) {
            LockInfo currentLock = lockStore.get(lockKey);
            if (currentLock == lockInfo) { // 동일한 락인지 확인
                lockStore.remove(lockKey);
                log.debug("[REDIS-락] 락 만료되어 제거됨: key={}", lockKey);
            }
        }
    }

    private String getCurrentThreadId() {
        // Redis 분산락에서 사용하는 클라이언트 식별자를 모방
        // Mimic Redis distributed lock client identifier
        return Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
    }

}