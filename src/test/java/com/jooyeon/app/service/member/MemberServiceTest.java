package com.jooyeon.app.service.member;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.MemberException;
import com.jooyeon.app.domain.dto.member.MemberRegistrationDto;
import com.jooyeon.app.domain.dto.member.MemberResponseDto;
import com.jooyeon.app.domain.dto.member.MemberWithdrawalRequestDto;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import com.jooyeon.app.domain.entity.member.Gender;
import com.jooyeon.app.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    private MemberRegistrationDto registrationDto;
    private Member activeMember;
    private Member withdrawnMember;

    @BeforeEach
    void setUp() {
        registrationDto = new MemberRegistrationDto();
        registrationDto.setUserId("testUser");
        registrationDto.setName("테스트 사용자");
        registrationDto.setPhoneNumber("010-1234-5678");
        registrationDto.setGender(Gender.MALE);
        registrationDto.setBirthDate("1990-01-01");

        activeMember = new Member();
        activeMember.setId(1L);
        activeMember.setUserId("testUser");
        activeMember.setName("테스트 사용자");
        activeMember.setPhoneNumber("010-1234-5678");
        activeMember.setGender(Gender.MALE);
        activeMember.setBirthDate("1990-01-01");
        activeMember.setMemberStatus(MemberStatus.ACTIVE);
        activeMember.setCreatedAt(LocalDateTime.now());
        activeMember.setUpdatedAt(LocalDateTime.now());

        withdrawnMember = new Member();
        withdrawnMember.setId(2L);
        withdrawnMember.setUserId("withdrawnUser");
        withdrawnMember.setMemberStatus(MemberStatus.WITHDRAWN);
        withdrawnMember.setWithdrawnAt(LocalDateTime.now().minusDays(5));
    }

    @Test
    @DisplayName("회원가입이 정상적으로 처리된다")
    void registerMember_Success() {
        // given
        when(memberRepository.existsByUserId(registrationDto.getUserId())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(activeMember);

        // when
        MemberResponseDto result = memberService.registerMember(registrationDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(registrationDto.getUserId());
        assertThat(result.getName()).isEqualTo(registrationDto.getName());
        verify(memberRepository).existsByUserId(registrationDto.getUserId());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("중복된 userId로 회원가입 시 예외가 발생한다")
    void registerMember_DuplicateUserId() {
        // given
        when(memberRepository.existsByUserId(registrationDto.getUserId())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.registerMember(registrationDto))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_ALREADY_EXISTS);

        verify(memberRepository).existsByUserId(registrationDto.getUserId());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("userId로 회원 인증이 정상적으로 처리된다")
    void authenticateMember_Success() {
        // given
        when(memberRepository.findByUserId("testUser")).thenReturn(Optional.of(activeMember));

        // when
        Member result = memberService.authenticateMember("testUser");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("testUser");
        assertThat(result.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(memberRepository).findByUserId("testUser");
    }

    @Test
    @DisplayName("존재하지 않는 userId로 인증 시 예외가 발생한다")
    void authenticateMember_UserNotFound() {
        // given
        when(memberRepository.findByUserId("nonExistentUser")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.authenticateMember("nonExistentUser"))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);

        verify(memberRepository).findByUserId("nonExistentUser");
    }

    @Test
    @DisplayName("탈퇴한 회원으로 인증 시 예외가 발생한다")
    void authenticateMember_InactiveMember() {
        // given
        withdrawnMember.setUserId("testUser");
        when(memberRepository.findByUserId("testUser")).thenReturn(Optional.of(withdrawnMember));

        // when & then
        assertThatThrownBy(() -> memberService.authenticateMember("testUser"))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_ACTIVE);

        verify(memberRepository).findByUserId("testUser");
    }

    @Test
    @DisplayName("회원 탈퇴 요청이 정상적으로 처리된다")
    void requestWithdrawal_Success() {
        // given
        Long memberId = 1L;
        MemberWithdrawalRequestDto withdrawalRequest = new MemberWithdrawalRequestDto();
        withdrawalRequest.setWithdrawalReason("개인 사정");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(activeMember));
        when(memberRepository.save(any(Member.class))).thenReturn(activeMember);

        // when
        MemberResponseDto result = memberService.requestWithdrawal(memberId, withdrawalRequest);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).findById(memberId);
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원의 탈퇴 요청 시 예외가 발생한다")
    void requestWithdrawal_MemberNotFound() {
        // given
        Long memberId = 999L;
        MemberWithdrawalRequestDto withdrawalRequest = new MemberWithdrawalRequestDto();
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.requestWithdrawal(memberId, withdrawalRequest))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        verify(memberRepository).findById(memberId);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원 탈퇴 취소가 정상적으로 처리된다")
    void cancelWithdrawal_Success() {
        // given
        Long memberId = 2L;
        Member pendingWithdrawalMember = new Member();
        pendingWithdrawalMember.setId(memberId);
        pendingWithdrawalMember.setUserId("testUser");
        pendingWithdrawalMember.setName("Test User");
        pendingWithdrawalMember.setPhoneNumber("010-1234-5678");
        pendingWithdrawalMember.setGender(Gender.MALE);
        pendingWithdrawalMember.setBirthDate("1990-01-01");
        pendingWithdrawalMember.setMemberStatus(MemberStatus.PENDING_WITHDRAWAL);
        pendingWithdrawalMember.setWithdrawnAt(LocalDateTime.now().minusDays(5)); // 5일 전
        pendingWithdrawalMember.setCreatedAt(LocalDateTime.now().minusDays(10));
        pendingWithdrawalMember.setUpdatedAt(LocalDateTime.now());

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(pendingWithdrawalMember));
        when(memberRepository.save(any(Member.class))).thenReturn(pendingWithdrawalMember);

        // when
        MemberResponseDto result = memberService.cancelWithdrawal(memberId);

        // then
        assertThat(result).isNotNull();
        verify(memberRepository).findById(memberId);
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원 상태 확인이 정상적으로 처리된다")
    void isMemberActive_Success() {
        // given
        Long memberId = 1L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(activeMember));

        // when
        boolean result = memberService.isMemberActive(memberId);

        // then
        assertThat(result).isTrue();
        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("탈퇴한 회원의 상태 확인 시 false를 반환한다")
    void isMemberActive_InactiveMember() {
        // given
        Long memberId = 2L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(withdrawnMember));

        // when
        boolean result = memberService.isMemberActive(memberId);

        // then
        assertThat(result).isFalse();
        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 상태 확인 시 false를 반환한다")
    void isMemberActive_MemberNotFound() {
        // given
        Long memberId = 999L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when
        boolean result = memberService.isMemberActive(memberId);

        // then
        assertThat(result).isFalse();
        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("회원 조회가 정상적으로 처리된다")
    void getMemberById_Success() {
        // given
        Long memberId = 1L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(activeMember));

        // when
        MemberResponseDto result = memberService.getMemberById(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(memberId);
        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 시 예외가 발생한다")
    void getMemberById_MemberNotFound() {
        // given
        Long memberId = 999L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMemberById(memberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        verify(memberRepository).findById(memberId);
    }
}