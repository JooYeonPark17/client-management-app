package com.jooyeon.app.common.idempotency;

import com.jooyeon.app.common.lock.Lock;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 멱등성 키 검증 서비스
 * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
 *
 * Redis 멱등성 패턴 구현:
 * - SET idempotency:key processing PX 300000 NX (5분 처리 시간 확보)
 * - 처리 완료 후 결과 저장: SET idempotency:key:result value PX 86400000 (24시간 보관)
 * - 동일 키 재요청시 기존 결과 반환
 */

@Service("memoryIdempotencyService")
@Slf4j
public class IdempotencyService {

    // Redis의 멱등성 키 저장소를 모방
    // Mimic Redis storage for idempotency keys
    private final ConcurrentHashMap<String, IdempotencyRecord> idempotencyStore = new ConcurrentHashMap<>();

    private static final String PROCESSING_STATUS = "PROCESSING";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final int PROCESSING_TIMEOUT_MINUTES = 5; // 5분
    private static final int RESULT_RETENTION_HOURS = 24; // 24시간

    @Getter
    @Setter
    static class IdempotencyRecord {
        private final String key;
        private volatile String status;
        private volatile Object result;
        private final LocalDateTime createdAt;
        private volatile LocalDateTime completedAt;

        public IdempotencyRecord(String key) {
            this.key = key;
            this.status = PROCESSING_STATUS;
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * 멱등성 키 검증 및 처리 상태 확인
     * Redis 분산락을 사용하여 동시성 제어
     */
    @Lock(key = "'idempotency:' + #idempotencyKey", waitTime = 10, leaseTime = 30, timeUnit = TimeUnit.SECONDS)
    public IdempotencyResult checkIdempotency(String idempotencyKey) {
        log.debug("[멱등성] 멱등성 키 확인: {}", idempotencyKey);

        IdempotencyRecord record = idempotencyStore.get(idempotencyKey);

        if (record == null) {
            // 새로운 요청 - 처리 상태로 기록
            record = new IdempotencyRecord(idempotencyKey);
            idempotencyStore.put(idempotencyKey, record);
            log.debug("[멱등성] 새로운 요청 등록: {}", idempotencyKey);
            return new IdempotencyResult(false, null, record);
        }

        if (PROCESSING_STATUS.equals(record.getStatus())) {
            // 처리 타임아웃 확인 (Redis TTL 모방)
            if (record.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES))) {
                log.warn("[멱등성] 처리 타임아웃 감지, 재시도 허용: {}", idempotencyKey);
                record.setStatus(PROCESSING_STATUS);
                record.setResult(null);
                return new IdempotencyResult(false, null, record);
            } else {
                log.debug("[멱등성] 이미 처리 중인 요청: {}", idempotencyKey);
                return new IdempotencyResult(true, null, record);
            }
        }

        if (COMPLETED_STATUS.equals(record.getStatus())) {
            // 결과 보관 시간 확인 (Redis TTL 모방)
            if (record.getCompletedAt() != null &&
                record.getCompletedAt().isBefore(LocalDateTime.now().minusHours(RESULT_RETENTION_HOURS))) {
                log.debug("[멱등성] 결과 만료, 새 처리 허용: {}", idempotencyKey);
                record.setStatus(PROCESSING_STATUS);
                record.setResult(null);
                return new IdempotencyResult(false, null, record);
            } else {
                log.debug("[멱등성] 캐시된 결과 반환: {}", idempotencyKey);
                return new IdempotencyResult(true, record.getResult(), record);
            }
        }

        // 알 수 없는 상태 - 새로 처리
        log.warn("[멱등성] 알 수 없는 상태 감지, 재설정: {}", idempotencyKey);
        record.setStatus(PROCESSING_STATUS);
        record.setResult(null);
        return new IdempotencyResult(false, null, record);
    }

    /**
     * 처리 완료 후 결과 저장
     * Redis SET 명령어와 TTL 설정을 모방
     */
    public void saveResult(String idempotencyKey, Object result) {
        log.debug("[멱등성] 키에 대한 결과 저장: {}", idempotencyKey);

        IdempotencyRecord record = idempotencyStore.get(idempotencyKey);
        if (record != null) {
            record.setStatus(COMPLETED_STATUS);
            record.setResult(result);
            record.setCompletedAt(LocalDateTime.now());
            log.debug("[멱등성] 결과 저장 성공: {}", idempotencyKey);
        } else {
            log.warn("[멱등성] 결과 저장 시 레코드를 찾을 수 없음: {}", idempotencyKey);
        }
    }

    /**
     * 처리 실패시 상태 초기화
     */
    public void markFailed(String idempotencyKey) {
        log.debug("[멱등성] 실패로 표시: {}", idempotencyKey);

        IdempotencyRecord record = idempotencyStore.get(idempotencyKey);
        if (record != null) {
            idempotencyStore.remove(idempotencyKey);
            log.debug("[멱등성] 실패로 인한 레코드 제거: {}", idempotencyKey);
        }
    }


    /**
     * 멱등성 검증 결과
     */
    public static class IdempotencyResult {
        private final boolean duplicate;
        private final Object existingResult;
        private final IdempotencyRecord record;

        public IdempotencyResult(boolean duplicate, Object existingResult, IdempotencyRecord record) {
            this.duplicate = duplicate;
            this.existingResult = existingResult;
            this.record = record;
        }

        public boolean isDuplicate() { return duplicate; }
        public Object getExistingResult() { return existingResult; }
        public IdempotencyRecord getRecord() { return record; }
    }
}