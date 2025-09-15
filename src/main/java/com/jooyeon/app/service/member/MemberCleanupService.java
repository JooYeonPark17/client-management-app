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

    /**
     * 매일 오전 2시에 탈퇴한지 30일 이상 지난 회원들을 하드 삭제
     * cron = "0 0 2 * * *" -> 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredWithdrawnMembers() {
        log.info("[MEMBER_CLEANUP] 탈퇴 회원 정리 작업 시작");

        try {
            // 30일 전 날짜 계산
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

            // 삭제 대상 회원 수 조회 (로깅용)
            long targetCount = memberRepository.countMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);

            if (targetCount == 0) {
                log.info("[MEMBER_CLEANUP] 삭제 대상 회원이 없습니다.");
                return;
            }

            log.info("[MEMBER_CLEANUP] 삭제 대상 회원 수: {}명 ({}일 이전 탈퇴)", targetCount, 30);

            // 삭제 대상 회원 조회
            List<Member> expiredMembers = memberRepository.findMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);

            // 하드 삭제 수행
            int deletedCount = 0;
            for (Member member : expiredMembers) {
                try {
                    log.debug("[MEMBER_CLEANUP] 회원 삭제: ID={}, userId={}, 탈퇴일={}",
                            member.getId(), member.getUserId(), member.getWithdrawnAt());

                    memberRepository.delete(member);
                    deletedCount++;

                } catch (Exception e) {
                    log.error("[MEMBER_CLEANUP] 회원 삭제 중 오류 발생: ID={}, userId={}, 오류={}",
                            member.getId(), member.getUserId(), e.getMessage(), e);
                }
            }

            log.info("[MEMBER_CLEANUP] 탈퇴 회원 정리 완료: {}명 삭제", deletedCount);

        } catch (Exception e) {
            log.error("[MEMBER_CLEANUP] 탈퇴 회원 정리 작업 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 수동으로 탈퇴 회원 정리 작업 실행 (관리자용)
     * @return 삭제된 회원 수
     */
    @Transactional
    public int manualCleanupExpiredWithdrawnMembers() {
        log.info("[MEMBER_CLEANUP] 수동 탈퇴 회원 정리 작업 시작");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        List<Member> expiredMembers = memberRepository.findMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);

        log.info("[MEMBER_CLEANUP] 삭제 대상 회원 수: {}명", expiredMembers.size());

        int deletedCount = 0;
        for (Member member : expiredMembers) {
            try {
                memberRepository.delete(member);
                deletedCount++;
                log.debug("[MEMBER_CLEANUP] 수동 삭제: ID={}, userId={}", member.getId(), member.getUserId());

            } catch (Exception e) {
                log.error("[MEMBER_CLEANUP] 수동 삭제 중 오류: ID={}, 오류={}", member.getId(), e.getMessage(), e);
            }
        }

        log.info("[MEMBER_CLEANUP] 수동 탈퇴 회원 정리 완료: {}명 삭제", deletedCount);
        return deletedCount;
    }

    /**
     * 탈퇴한지 30일 이상 지난 회원 수 조회
     * @return 삭제 대상 회원 수
     */
    @Transactional(readOnly = true)
    public long getExpiredWithdrawnMembersCount() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        return memberRepository.countMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);
    }
}