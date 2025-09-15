package com.jooyeon.app.controller;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.MemberException;
import com.jooyeon.app.domain.dto.common.ApiResponse;
import com.jooyeon.app.domain.dto.member.MemberRegistrationDto;
import com.jooyeon.app.domain.dto.member.MemberResponseDto;
import com.jooyeon.app.domain.dto.member.MemberWithdrawalRequestDto;
import com.jooyeon.app.domain.dto.member.WithDrawResponseDto;
import com.jooyeon.app.domain.entity.member.Gender;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import com.jooyeon.app.service.member.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberController 순수 단위 테스트")
class MemberControllerUnitTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    private MemberRegistrationDto validRegistrationDto;
    private MemberResponseDto memberResponseDto;
    private Member testMember;

    @BeforeEach
    void setUp() {
        validRegistrationDto = new MemberRegistrationDto();
        validRegistrationDto.setUserId("testUser123");
        validRegistrationDto.setName("테스트 사용자");
        validRegistrationDto.setPhoneNumber("010-1234-5678");
        validRegistrationDto.setGender(Gender.MALE);
        validRegistrationDto.setBirthDate("1990-01-01");

        testMember = new Member();
        testMember.setId(1L);
        testMember.setUserId("testUser123");
        testMember.setName("테스트 사용자");
        testMember.setPhoneNumber("010-1234-5678");
        testMember.setGender(Gender.MALE);
        testMember.setBirthDate("1990-01-01");
        testMember.setMemberStatus(MemberStatus.ACTIVE);
        testMember.setCreatedAt(LocalDateTime.now());
        testMember.setUpdatedAt(LocalDateTime.now());

        memberResponseDto = MemberResponseDto.convertToResponseDto(testMember);
    }

    @Test
    @DisplayName("회원가입 - 성공")
    void registerMember_Success() {
        // given
        when(memberService.registerMember(any(MemberRegistrationDto.class)))
                .thenReturn(memberResponseDto);

        // when
        ResponseEntity<ApiResponse<MemberResponseDto>> response =
                memberController.registerMember(validRegistrationDto);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("회원가입이 완료되었습니다.");
        assertThat(response.getBody().getData().getUserId()).isEqualTo("testUser123");
        assertThat(response.getBody().getData().getName()).isEqualTo("테스트 사용자");
    }

    @Test
    @DisplayName("회원가입 - 중복 userId로 실패")
    void registerMember_DuplicateUserId() {
        // given
        when(memberService.registerMember(any(MemberRegistrationDto.class)))
                .thenThrow(new MemberException(ErrorCode.MEMBER_ALREADY_EXISTS));

        // when & then
        assertThatThrownBy(() -> memberController.registerMember(validRegistrationDto))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("회원탈퇴 - 성공")
    void requestWithdrawal_Success() {
        // given
        MemberWithdrawalRequestDto withdrawalRequest = new MemberWithdrawalRequestDto();
        withdrawalRequest.setWithdrawalReason("개인 사정");

        MemberResponseDto withdrawnMemberDto = new MemberResponseDto(1L, LocalDateTime.now());

        when(memberService.requestWithdrawal(eq(1L), any(MemberWithdrawalRequestDto.class)))
                .thenReturn(withdrawnMemberDto);

        // when
        ResponseEntity<ApiResponse<WithDrawResponseDto>> response =
                memberController.requestWithdrawal(withdrawalRequest, testMember);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("회원탈퇴 요청이 완료되었습니다.");
        assertThat(response.getBody().getData().getMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("회원탈퇴 취소 - 성공")
    void cancelWithdrawal_Success() {
        // given
        when(memberService.cancelWithdrawal(eq(1L)))
                .thenReturn(memberResponseDto);

        // when
        ResponseEntity<ApiResponse<MemberResponseDto>> response =
                memberController.cancelWithdrawal(testMember);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("탈퇴 요청이 취소되었습니다. 계정이 다시 활성화되었습니다.");
        assertThat(response.getBody().getData().getUserId()).isEqualTo("testUser123");
    }

    @Test
    @DisplayName("회원탈퇴 - 이미 탈퇴 처리된 회원")
    void requestWithdrawal_AlreadyWithdrawn() {
        // given
        MemberWithdrawalRequestDto withdrawalRequest = new MemberWithdrawalRequestDto();
        withdrawalRequest.setWithdrawalReason("개인 사정");

        when(memberService.requestWithdrawal(eq(1L), any(MemberWithdrawalRequestDto.class)))
                .thenThrow(new MemberException(ErrorCode.WITHDRAWAL_ALREADY_PENDING));

        // when & then
        assertThatThrownBy(() -> memberController.requestWithdrawal(withdrawalRequest, testMember))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WITHDRAWAL_ALREADY_PENDING);
    }

    @Test
    @DisplayName("회원탈퇴 취소 - 탈퇴 요청이 없는 경우")
    void cancelWithdrawal_NoPendingWithdrawal() {
        // given
        when(memberService.cancelWithdrawal(eq(1L)))
                .thenThrow(new MemberException(ErrorCode.NO_PENDING_WITHDRAWAL));

        // when & then
        assertThatThrownBy(() -> memberController.cancelWithdrawal(testMember))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_PENDING_WITHDRAWAL);
    }

}