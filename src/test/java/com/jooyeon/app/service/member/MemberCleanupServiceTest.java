package com.jooyeon.app.service.member;

import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import com.jooyeon.app.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberCleanupService 테스트")
class MemberCleanupServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberCleanupService memberCleanupService;

    private Member expiredMember1;
    private Member expiredMember2;
    private Member recentWithdrawnMember;

    @BeforeEach
    void setUp() {
        // 30일 이상 지난 탈퇴 회원
        expiredMember1 = new Member();
        expiredMember1.setId(1L);
        expiredMember1.setUserId("expired_user1");
        expiredMember1.setMemberStatus(MemberStatus.WITHDRAWN);
        expiredMember1.setWithdrawnAt(LocalDateTime.now().minusDays(35));

        expiredMember2 = new Member();
        expiredMember2.setId(2L);
        expiredMember2.setUserId("expired_user2");
        expiredMember2.setMemberStatus(MemberStatus.WITHDRAWN);
        expiredMember2.setWithdrawnAt(LocalDateTime.now().minusDays(45));

        // 30일 이내의 최근 탈퇴 회원 (삭제 대상이 아님)
        recentWithdrawnMember = new Member();
        recentWithdrawnMember.setId(3L);
        recentWithdrawnMember.setUserId("recent_user");
        recentWithdrawnMember.setMemberStatus(MemberStatus.WITHDRAWN);
        recentWithdrawnMember.setWithdrawnAt(LocalDateTime.now().minusDays(10));
    }

    @Test
    @DisplayName("30일 이상 지난 탈퇴 회원들이 정상적으로 삭제된다")
    void cleanupExpiredWithdrawnMembers_Success() {
        // given
        List<Member> expiredMembers = Arrays.asList(expiredMember1, expiredMember2);
        when(memberRepository.countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(2L);
        when(memberRepository.findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(expiredMembers);

        // when
        memberCleanupService.cleanupExpiredWithdrawnMembers();

        // then
        verify(memberRepository).countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class));
        verify(memberRepository).findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class));
        verify(memberRepository, times(2)).delete(any(Member.class));
        verify(memberRepository).delete(expiredMember1);
        verify(memberRepository).delete(expiredMember2);
    }

    @Test
    @DisplayName("삭제 대상 회원이 없을 때 정상적으로 처리된다")
    void cleanupExpiredWithdrawnMembers_NoTargetsFound() {
        // given
        when(memberRepository.countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(0L);

        // when
        memberCleanupService.cleanupExpiredWithdrawnMembers();

        // then
        verify(memberRepository).countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class));
        verify(memberRepository, never()).findMembersWithdrawnBefore(any(), any());
        verify(memberRepository, never()).delete(any(Member.class));
    }

    @Test
    @DisplayName("일부 회원 삭제에 실패해도 다른 회원들은 정상적으로 삭제된다")
    void cleanupExpiredWithdrawnMembers_PartialFailure() {
        // given
        List<Member> expiredMembers = Arrays.asList(expiredMember1, expiredMember2);
        when(memberRepository.countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(2L);
        when(memberRepository.findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(expiredMembers);

        // 첫 번째 회원 삭제는 실패, 두 번째는 성공
        doThrow(new RuntimeException("삭제 실패")).when(memberRepository).delete(expiredMember1);
        doNothing().when(memberRepository).delete(expiredMember2);

        // when
        memberCleanupService.cleanupExpiredWithdrawnMembers();

        // then
        verify(memberRepository).delete(expiredMember1);
        verify(memberRepository).delete(expiredMember2);
    }

    @Test
    @DisplayName("수동 정리 작업이 정상적으로 수행된다")
    void manualCleanupExpiredWithdrawnMembers_Success() {
        // given
        List<Member> expiredMembers = Arrays.asList(expiredMember1, expiredMember2);
        when(memberRepository.findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(expiredMembers);

        // when
        int deletedCount = memberCleanupService.manualCleanupExpiredWithdrawnMembers();

        // then
        assertThat(deletedCount).isEqualTo(2);
        verify(memberRepository).findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class));
        verify(memberRepository, times(2)).delete(any(Member.class));
    }

    @Test
    @DisplayName("수동 정리 작업에서 삭제 대상이 없을 때 정상적으로 처리된다")
    void manualCleanupExpiredWithdrawnMembers_NoTargets() {
        // given
        when(memberRepository.findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // when
        int deletedCount = memberCleanupService.manualCleanupExpiredWithdrawnMembers();

        // then
        assertThat(deletedCount).isEqualTo(0);
        verify(memberRepository).findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class));
        verify(memberRepository, never()).delete(any(Member.class));
    }

    @Test
    @DisplayName("삭제 대상 회원 수 조회가 정상적으로 수행된다")
    void getExpiredWithdrawnMembersCount_Success() {
        // given
        long expectedCount = 5L;
        when(memberRepository.countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(expectedCount);

        // when
        long actualCount = memberCleanupService.getExpiredWithdrawnMembersCount();

        // then
        assertThat(actualCount).isEqualTo(expectedCount);
        verify(memberRepository).countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("삭제 대상 회원 수가 0일 때 정상적으로 반환된다")
    void getExpiredWithdrawnMembersCount_ZeroCount() {
        // given
        when(memberRepository.countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(0L);

        // when
        long actualCount = memberCleanupService.getExpiredWithdrawnMembersCount();

        // then
        assertThat(actualCount).isEqualTo(0L);
        verify(memberRepository).countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class));
    }
}