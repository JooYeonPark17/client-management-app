package com.jooyeon.app.service.member;

import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import com.jooyeon.app.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원 데이터 정리 서비스
 * - 탈퇴한지 30일 이상 지난 회원들을 하드 삭제
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberCleanupService {

    private final MemberRepository memberRepository;

    private static final int WITHDRAWAL_RETENTION_DAYS = 30;

    /**
     * 매일 오전 2시에 탈퇴한지 30일 이상 지난 회원들을 하드 삭제
     * cron = "0 0 2 * * *" -> 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredWithdrawnMembers() {
        log.info("[MEMBER_CLEANUP] 탈퇴 회원 정리 작업 시작");

        try {
            LocalDateTime cutoffDate = calculateCutoffDate();

            long targetCount = getTargetMembersCount(cutoffDate);
            if (targetCount == 0) {
                log.info("[MEMBER_CLEANUP] 삭제 대상 회원이 없습니다.");
                return;
            }

            log.info("[MEMBER_CLEANUP] 삭제 대상 회원 수: {}명 ({}일 이전 탈퇴)", targetCount, WITHDRAWAL_RETENTION_DAYS);

            List<Member> expiredMembers = getExpiredMembers(cutoffDate);
            int deletedCount = deleteMembers(expiredMembers, true);

            log.info("[MEMBER_CLEANUP] 탈퇴 회원 정리 완료: {}명 삭제", deletedCount);

        } catch (Exception e) {
            log.error("[MEMBER_CLEANUP] 탈퇴 회원 정리 작업 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 탈퇴한지 30일 이상 지난 회원 수 조회
     * @return 삭제 대상 회원 수
     */
    @Transactional(readOnly = true)
    public long getExpiredWithdrawnMembersCount() {
        LocalDateTime cutoffDate = calculateCutoffDate();
        return getTargetMembersCount(cutoffDate);
    }

    private LocalDateTime calculateCutoffDate() {
        return LocalDateTime.now().minusDays(WITHDRAWAL_RETENTION_DAYS);
    }

    private long getTargetMembersCount(LocalDateTime cutoffDate) {
        return memberRepository.countMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);
    }

    private List<Member> getExpiredMembers(LocalDateTime cutoffDate) {
        return memberRepository.findMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);
    }

    private int deleteMembers(List<Member> expiredMembers, boolean isScheduledJob) {
        int deletedCount = 0;
        String logPrefix = isScheduledJob ? "[MEMBER_CLEANUP]" : "[MEMBER_CLEANUP]";

        for (Member member : expiredMembers) {
            try {
                if (isScheduledJob) {
                    log.debug("{} 회원 삭제: ID={}, userId={}, 탈퇴일={}",
                            logPrefix, member.getId(), member.getUserId(), member.getWithdrawnAt());
                } else {
                    log.debug("{} 수동 삭제: ID={}, userId={}",
                            logPrefix, member.getId(), member.getUserId());
                }

                memberRepository.delete(member);
                deletedCount++;

            } catch (Exception e) {
                if (isScheduledJob) {
                    log.error("{} 회원 삭제 중 오류 발생: ID={}, userId={}, 오류={}",
                            logPrefix, member.getId(), member.getUserId(), e.getMessage(), e);
                } else {
                    log.error("{} 수동 삭제 중 오류: ID={}, 오류={}",
                            logPrefix, member.getId(), e.getMessage(), e);
                }
            }
        }

        return deletedCount;
    }
}