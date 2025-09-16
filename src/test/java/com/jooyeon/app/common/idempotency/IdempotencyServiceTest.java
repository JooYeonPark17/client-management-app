package com.jooyeon.app.common.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService 단위 테스트")
class IdempotencyServiceTest {

    private IdempotencyService idempotencyService;
    private ConcurrentHashMap<String, IdempotencyService.InMemoryIdempotencyRecord> idempotencyStore;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService();

        // private 필드에 접근하여 테스트용 store 가져오기
        idempotencyStore = (ConcurrentHashMap<String, IdempotencyService.InMemoryIdempotencyRecord>)
                ReflectionTestUtils.getField(idempotencyService, "idempotencyStore");
    }

    @Test
    @DisplayName("새로운 멱등성 키 - 처리 상태로 기록")
    void checkIdempotency_NewKey_CreatesProcessingRecord() {
        // given
        String idempotencyKey = "new-key-123";

        // when
        IdempotencyService.IdempotencyResult result = idempotencyService.checkIdempotency(idempotencyKey);

        // then
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getExistingResult()).isNull();
        assertThat(result.getRecord()).isNotNull();
        assertThat(result.getRecord().getKey()).isEqualTo(idempotencyKey);
        assertThat(result.getRecord().getStatus()).isEqualTo("PROCESSING");

        // 스토어에 저장되었는지 확인
        assertThat(idempotencyStore).containsKey(idempotencyKey);
    }

    @Test
    @DisplayName("처리 중 상태의 멱등성 키 - 중복 요청으로 판단")
    void checkIdempotency_ProcessingKey_ReturnsDuplicate() {
        // given
        String idempotencyKey = "processing-key-123";
        IdempotencyService.InMemoryIdempotencyRecord record =
                new IdempotencyService.InMemoryIdempotencyRecord(idempotencyKey);
        idempotencyStore.put(idempotencyKey, record);

        // when
        IdempotencyService.IdempotencyResult result = idempotencyService.checkIdempotency(idempotencyKey);

        // then
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getExistingResult()).isNull();
        assertThat(result.getRecord()).isEqualTo(record);
    }

    @Test
    @DisplayName("완료 상태의 멱등성 키 - 기존 결과 반환")
    void checkIdempotency_CompletedKey_ReturnsExistingResult() {
        // given
        String idempotencyKey = "completed-key-123";
        String expectedResult = "Previous processing result";

        IdempotencyService.InMemoryIdempotencyRecord record =
                new IdempotencyService.InMemoryIdempotencyRecord(idempotencyKey);
        record.setStatus("COMPLETED");
        record.setResult(expectedResult);
        record.setCompletedAt(LocalDateTime.now());
        idempotencyStore.put(idempotencyKey, record);

        // when
        IdempotencyService.IdempotencyResult result = idempotencyService.checkIdempotency(idempotencyKey);

        // then
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getExistingResult()).isEqualTo(expectedResult);
        assertThat(result.getRecord()).isEqualTo(record);
    }

    @Test
    @DisplayName("처리 타임아웃된 키 - 재시도 허용")
    void checkIdempotency_TimeoutKey_AllowsRetry() {
        // given
        String idempotencyKey = "timeout-key-123";
        IdempotencyService.InMemoryIdempotencyRecord record =
                new IdempotencyService.InMemoryIdempotencyRecord(idempotencyKey);

        // 6분 전에 생성된 것으로 설정 (5분 타임아웃 초과)
        ReflectionTestUtils.setField(record, "createdAt", LocalDateTime.now().minusMinutes(6));
        idempotencyStore.put(idempotencyKey, record);

        // when
        IdempotencyService.IdempotencyResult result = idempotencyService.checkIdempotency(idempotencyKey);

        // then
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getExistingResult()).isNull();
        assertThat(result.getRecord()).isEqualTo(record);
        assertThat(record.getStatus()).isEqualTo("PROCESSING");
        assertThat(record.getResult()).isNull();
    }

    @Test
    @DisplayName("만료된 결과 키 - 새 처리 허용")
    void checkIdempotency_ExpiredResultKey_AllowsNewProcessing() {
        // given
        String idempotencyKey = "expired-result-key-123";
        IdempotencyService.InMemoryIdempotencyRecord record =
                new IdempotencyService.InMemoryIdempotencyRecord(idempotencyKey);
        record.setStatus("COMPLETED");
        record.setResult("Old result");
        record.setCompletedAt(LocalDateTime.now().minusHours(25)); // 24시간 + 1시간 초과
        idempotencyStore.put(idempotencyKey, record);

        // when
        IdempotencyService.IdempotencyResult result = idempotencyService.checkIdempotency(idempotencyKey);

        // then
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getExistingResult()).isNull();
        assertThat(result.getRecord()).isEqualTo(record);
        assertThat(record.getStatus()).isEqualTo("PROCESSING");
        assertThat(record.getResult()).isNull();
    }

    @Test
    @DisplayName("알 수 없는 상태의 키 - 처리 상태로 재설정")
    void checkIdempotency_UnknownStatusKey_ResetToProcessing() {
        // given
        String idempotencyKey = "unknown-status-key-123";
        IdempotencyService.InMemoryIdempotencyRecord record =
                new IdempotencyService.InMemoryIdempotencyRecord(idempotencyKey);
        record.setStatus("UNKNOWN_STATUS");
        record.setResult("Some old data");
        idempotencyStore.put(idempotencyKey, record);

        // when
        IdempotencyService.IdempotencyResult result = idempotencyService.checkIdempotency(idempotencyKey);

        // then
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getExistingResult()).isNull();
        assertThat(result.getRecord()).isEqualTo(record);
        assertThat(record.getStatus()).isEqualTo("PROCESSING");
        assertThat(record.getResult()).isNull();
    }

    @Test
    @DisplayName("결과 저장 - 완료 상태로 변경")
    void saveResult_ValidKey_UpdatesToCompleted() {
        // given
        String idempotencyKey = "save-result-key-123";
        String result = "Processing completed successfully";

        IdempotencyService.InMemoryIdempotencyRecord record =
                new IdempotencyService.InMemoryIdempotencyRecord(idempotencyKey);
        idempotencyStore.put(idempotencyKey, record);

        // when
        idempotencyService.saveResult(idempotencyKey, result);

        // then
        assertThat(record.getStatus()).isEqualTo("COMPLETED");
        assertThat(record.getResult()).isEqualTo(result);
        assertThat(record.getCompletedAt()).isNotNull();
        assertThat(record.getCompletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("존재하지 않는 키에 결과 저장 - 경고 로그만 출력")
    void saveResult_NonExistentKey_LogsWarning() {
        // given
        String nonExistentKey = "non-existent-key";
        String result = "Some result";

        // when
        idempotencyService.saveResult(nonExistentKey, result);

        // then
        // 예외가 발생하지 않아야 함
        assertThat(idempotencyStore).doesNotContainKey(nonExistentKey);
    }

    @Test
    @DisplayName("실패 처리 - 레코드 제거")
    void markFailed_ExistingKey_RemovesRecord() {
        // given
        String idempotencyKey = "failed-key-123";
        IdempotencyService.InMemoryIdempotencyRecord record =
                new IdempotencyService.InMemoryIdempotencyRecord(idempotencyKey);
        idempotencyStore.put(idempotencyKey, record);

        // when
        idempotencyService.markFailed(idempotencyKey);

        // then
        assertThat(idempotencyStore).doesNotContainKey(idempotencyKey);
    }

    @Test
    @DisplayName("존재하지 않는 키 실패 처리 - 안전하게 처리")
    void markFailed_NonExistentKey_HandlesSafely() {
        // given
        String nonExistentKey = "non-existent-failed-key";

        // when & then - 예외 발생하지 않음
        idempotencyService.markFailed(nonExistentKey);
        assertThat(idempotencyStore).doesNotContainKey(nonExistentKey);
    }

    @Test
    @DisplayName("동시 요청 시나리오 - 첫 번째 요청만 처리")
    void checkIdempotency_ConcurrentRequests_OnlyFirstProcesses() {
        // given
        String idempotencyKey = "concurrent-key-123";

        // when - 첫 번째 요청
        IdempotencyService.IdempotencyResult firstResult = idempotencyService.checkIdempotency(idempotencyKey);

        // when - 두 번째 요청 (동시)
        IdempotencyService.IdempotencyResult secondResult = idempotencyService.checkIdempotency(idempotencyKey);

        // then
        assertThat(firstResult.isDuplicate()).isFalse(); // 첫 번째는 새 요청
        assertThat(secondResult.isDuplicate()).isTrue();  // 두 번째는 중복 요청

        assertThat(firstResult.getRecord()).isEqualTo(secondResult.getRecord()); // 같은 레코드
    }

    @Test
    @DisplayName("결과 저장 후 재요청 - 저장된 결과 반환")
    void checkIdempotency_AfterSaveResult_ReturnsStoredResult() {
        // given
        String idempotencyKey = "stored-result-key-123";
        String storedResult = "Stored processing result";

        // 첫 번째 요청
        IdempotencyService.IdempotencyResult firstResult = idempotencyService.checkIdempotency(idempotencyKey);
        assertThat(firstResult.isDuplicate()).isFalse();

        // 결과 저장
        idempotencyService.saveResult(idempotencyKey, storedResult);

        // when - 두 번째 요청 (결과 저장 후)
        IdempotencyService.IdempotencyResult secondResult = idempotencyService.checkIdempotency(idempotencyKey);

        // then
        assertThat(secondResult.isDuplicate()).isTrue();
        assertThat(secondResult.getExistingResult()).isEqualTo(storedResult);
    }

    @Test
    @DisplayName("실패 후 재요청 - 새로운 처리 허용")
    void checkIdempotency_AfterMarkFailed_AllowsNewProcessing() {
        // given
        String idempotencyKey = "failed-then-retry-key-123";

        // 첫 번째 요청
        IdempotencyService.IdempotencyResult firstResult = idempotencyService.checkIdempotency(idempotencyKey);
        assertThat(firstResult.isDuplicate()).isFalse();

        // 실패 처리
        idempotencyService.markFailed(idempotencyKey);

        // when - 두 번째 요청 (실패 후)
        IdempotencyService.IdempotencyResult secondResult = idempotencyService.checkIdempotency(idempotencyKey);

        // then
        assertThat(secondResult.isDuplicate()).isFalse(); // 새로운 처리 허용
        assertThat(secondResult.getRecord()).isNotNull();
        assertThat(secondResult.getRecord().getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("복합 시나리오 - 처리->완료->만료->재처리")
    void checkIdempotency_ComplexScenario_HandlesAllStates() {
        // given
        String idempotencyKey = "complex-scenario-key";
        String result = "Complex scenario result";

        // 1. 첫 번째 요청 - 새로운 처리
        IdempotencyService.IdempotencyResult firstCheck = idempotencyService.checkIdempotency(idempotencyKey);
        assertThat(firstCheck.isDuplicate()).isFalse();

        // 2. 처리 중 재요청 - 중복 감지
        IdempotencyService.IdempotencyResult secondCheck = idempotencyService.checkIdempotency(idempotencyKey);
        assertThat(secondCheck.isDuplicate()).isTrue();
        assertThat(secondCheck.getExistingResult()).isNull();

        // 3. 결과 저장
        idempotencyService.saveResult(idempotencyKey, result);

        // 4. 완료 후 재요청 - 저장된 결과 반환
        IdempotencyService.IdempotencyResult thirdCheck = idempotencyService.checkIdempotency(idempotencyKey);
        assertThat(thirdCheck.isDuplicate()).isTrue();
        assertThat(thirdCheck.getExistingResult()).isEqualTo(result);

        // 5. 결과 만료 시뮬레이션
        IdempotencyService.InMemoryIdempotencyRecord record = idempotencyStore.get(idempotencyKey);
        record.setCompletedAt(LocalDateTime.now().minusHours(25)); // 만료

        // 6. 만료 후 재요청 - 새로운 처리 허용
        IdempotencyService.IdempotencyResult fourthCheck = idempotencyService.checkIdempotency(idempotencyKey);
        assertThat(fourthCheck.isDuplicate()).isFalse();
        assertThat(record.getStatus()).isEqualTo("PROCESSING");
        assertThat(record.getResult()).isNull();
    }
}