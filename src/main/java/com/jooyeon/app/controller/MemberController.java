package com.jooyeon.app.controller;

import com.jooyeon.app.common.security.CurrentUser;
import com.jooyeon.app.domain.dto.common.ApiResponse;
import com.jooyeon.app.domain.dto.member.MemberRegistrationDto;
import com.jooyeon.app.domain.dto.member.MemberResponseDto;
import com.jooyeon.app.domain.dto.member.MemberWithdrawalRequestDto;
import com.jooyeon.app.domain.dto.member.WithDrawResponseDto;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.service.member.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Member", description = "회원 관리 API")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    @Operation(summary = "회원 등록", description = "회원 등록 합니다")
    public ResponseEntity<ApiResponse<MemberResponseDto>> registerMember(@Valid @RequestBody MemberRegistrationDto registrationDto) {
        MemberResponseDto memberResponse = memberService.registerMember(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", memberResponse));
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴 합니다. 30일 이내에 철회 가능합니다")
    public ResponseEntity<ApiResponse<WithDrawResponseDto>> requestWithdrawal(
            @Valid @RequestBody MemberWithdrawalRequestDto withdrawalRequest,
            @CurrentUser Member currentMember) {

        MemberResponseDto memberResponse = memberService.requestWithdrawal(currentMember.getId(), withdrawalRequest);

        WithDrawResponseDto responseDto = WithDrawResponseDto.builder()
                .memberId(memberResponse.getMemberId())
                .withdrawalRequestedAt(memberResponse.getWithdrawalRequestedAt())
                .cancellationDeadline(memberResponse.getWithdrawalRequestedAt().plusDays(30))
                .message("탈퇴한지 30일 이내에는 탈퇴를 철회할 수 있으며, 탈퇴 철회 시 즉시 서비스 이용이 가능합니다.")
                .build();

        return ResponseEntity.ok(ApiResponse.success("회원탈퇴 요청이 완료되었습니다.", responseDto));
    }

    @PostMapping("/withdraw/cancel")
    @Operation(summary = "회원 탈퇴 취소", description = "회원 탈퇴 요청을 취소합니다")
    public ResponseEntity<ApiResponse<MemberResponseDto>> cancelWithdrawal(@CurrentUser Member currentMember) {
        MemberResponseDto memberResponse = memberService.cancelWithdrawal(currentMember.getId());
        return ResponseEntity.ok(ApiResponse.success("탈퇴 요청이 취소되었습니다. 계정이 다시 활성화되었습니다.", memberResponse));
    }

}