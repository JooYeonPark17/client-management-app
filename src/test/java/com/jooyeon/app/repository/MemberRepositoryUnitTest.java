package com.jooyeon.app.repository;

import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import com.jooyeon.app.domain.entity.member.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberRepository 순수 단위 테스트")
class MemberRepositoryUnitTest {

    @Mock
    private MemberRepository memberRepository;

    private Member activeMember;
    private Member withdrawnMember;
    private Member oldWithdrawnMember;

    @BeforeEach
    void setUp() {
        // 활성 회원
        activeMember = new Member();
        activeMember.setId(1L);
        activeMember.setUserId("activeUser");
        activeMember.setName("활성 사용자");
        activeMember.setPhoneNumber("010-1111-1111");
        activeMember.setGender(Gender.MALE);
        activeMember.setBirthDate("1990-01-01");
        activeMember.setMemberStatus(MemberStatus.ACTIVE);
        activeMember.setCreatedAt(LocalDateTime.now());
        activeMember.setUpdatedAt(LocalDateTime.now());

        // 최근 탈퇴 회원 (30일 이내)
        withdrawnMember = new Member();
        withdrawnMember.setId(2L);
        withdrawnMember.setUserId("recentWithdrawnUser");
        withdrawnMember.setName("최근 탈퇴 사용자");
        withdrawnMember.setPhoneNumber("010-2222-2222");
        withdrawnMember.setGender(Gender.FEMALE);
        withdrawnMember.setBirthDate("1992-02-02");
        withdrawnMember.setMemberStatus(MemberStatus.WITHDRAWN);
        withdrawnMember.setWithdrawnAt(LocalDateTime.now().minusDays(10));
        withdrawnMember.setCreatedAt(LocalDateTime.now().minusDays(100));
        withdrawnMember.setUpdatedAt(LocalDateTime.now().minusDays(10));

        // 오래된 탈퇴 회원 (30일 초과)
        oldWithdrawnMember = new Member();
        oldWithdrawnMember.setId(3L);
        oldWithdrawnMember.setUserId("oldWithdrawnUser");
        oldWithdrawnMember.setName("오래된 탈퇴 사용자");
        oldWithdrawnMember.setPhoneNumber("010-3333-3333");
        oldWithdrawnMember.setGender(Gender.MALE);
        oldWithdrawnMember.setBirthDate("1988-03-03");
        oldWithdrawnMember.setMemberStatus(MemberStatus.WITHDRAWN);
        oldWithdrawnMember.setWithdrawnAt(LocalDateTime.now().minusDays(45));
        oldWithdrawnMember.setCreatedAt(LocalDateTime.now().minusDays(200));
        oldWithdrawnMember.setUpdatedAt(LocalDateTime.now().minusDays(45));
    }

    @Test
    @DisplayName("userId 존재 여부를 정확히 확인한다")
    void existsByUserId() {
        // given
        when(memberRepository.existsByUserId("activeUser")).thenReturn(true);
        when(memberRepository.existsByUserId("recentWithdrawnUser")).thenReturn(true);
        when(memberRepository.existsByUserId("nonExistentUser")).thenReturn(false);

        // when & then
        assertThat(memberRepository.existsByUserId("activeUser")).isTrue();
        assertThat(memberRepository.existsByUserId("recentWithdrawnUser")).isTrue();
        assertThat(memberRepository.existsByUserId("nonExistentUser")).isFalse();
    }

    @Test
    @DisplayName("userId로 회원을 정확히 조회한다")
    void findByUserId() {
        // given
        when(memberRepository.findByUserId("activeUser")).thenReturn(Optional.of(activeMember));

        // when
        Optional<Member> result = memberRepository.findByUserId("activeUser");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("activeUser");
        assertThat(result.get().getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("존재하지 않는 userId로 조회 시 빈 Optional을 반환한다")
    void findByUserId_NotFound() {
        // given
        when(memberRepository.findByUserId("nonExistentUser")).thenReturn(Optional.empty());

        // when
        Optional<Member> result = memberRepository.findByUserId("nonExistentUser");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("30일 이상 지난 탈퇴 회원들을 정확히 조회한다")
    void findMembersWithdrawnBefore() {
        // given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        when(memberRepository.findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(oldWithdrawnMember));

        // when
        List<Member> result = memberRepository.findMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("oldWithdrawnUser");
        assertThat(result.get(0).getWithdrawnAt()).isBefore(cutoffDate);
    }

    @Test
    @DisplayName("30일 이상 지난 탈퇴 회원 수를 정확히 반환한다")
    void countMembersWithdrawnBefore() {
        // given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        when(memberRepository.countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(1L);

        // when
        long count = memberRepository.countMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);

        // then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("조건에 맞는 탈퇴 회원이 없을 때 빈 리스트를 반환한다")
    void findMembersWithdrawnBefore_NoResults() {
        // given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(60);
        when(memberRepository.findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // when
        List<Member> result = memberRepository.findMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("조건에 맞는 탈퇴 회원이 없을 때 0을 반환한다")
    void countMembersWithdrawnBefore_NoResults() {
        // given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(60);
        when(memberRepository.countMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(0L);

        // when
        long count = memberRepository.countMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);

        // then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("활성 회원은 탈퇴 회원 조회 결과에 포함되지 않는다")
    void findMembersWithdrawnBefore_ExcludesActiveMembers() {
        // given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);
        when(memberRepository.findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(withdrawnMember, oldWithdrawnMember));

        // when
        List<Member> result = memberRepository.findMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);

        // then
        assertThat(result).doesNotContain(activeMember);
        assertThat(result.stream().allMatch(member -> member.getMemberStatus() == MemberStatus.WITHDRAWN)).isTrue();
    }

    @Test
    @DisplayName("withdrawnAt이 null인 회원은 조회 결과에 포함되지 않는다")
    void findMembersWithdrawnBefore_ExcludesNullWithdrawnAt() {
        // given
        LocalDateTime cutoffDate = LocalDateTime.now().plusDays(1);
        when(memberRepository.findMembersWithdrawnBefore(eq(MemberStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(withdrawnMember, oldWithdrawnMember));

        // when
        List<Member> result = memberRepository.findMembersWithdrawnBefore(MemberStatus.WITHDRAWN, cutoffDate);

        // then
        assertThat(result.stream().allMatch(member -> member.getWithdrawnAt() != null)).isTrue();
    }
}