package com.jooyeon.app.controller;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.MemberException;
import com.jooyeon.app.common.security.JwtUtil;
import com.jooyeon.app.domain.dto.auth.LoginResponseDto;
import com.jooyeon.app.domain.dto.auth.RefreshTokenRequestDto;
import com.jooyeon.app.domain.dto.auth.TokenValidationResponseDto;
import com.jooyeon.app.domain.dto.common.ApiResponse;
import com.jooyeon.app.domain.dto.member.LoginRequestDto;
import com.jooyeon.app.domain.dto.member.MemberResponseDto;
import com.jooyeon.app.domain.dto.member.TokenResponseDto;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.service.auth.TokenValidationService;
import com.jooyeon.app.service.member.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "로그인 권한과 관련된 API")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final TokenValidationService tokenValidationService;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "회원가입이 된 회원들이 로그인합니다")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        Member member = memberService.authenticateMember(loginRequest.getUserId());

        String accessToken = jwtUtil.generateAccessToken(member.getUserId(), member.getId());
        String refreshToken = jwtUtil.generateRefreshToken(member.getUserId(), member.getId());

        TokenResponseDto tokenResponse = new TokenResponseDto(accessToken, refreshToken);
        MemberResponseDto memberResponse = MemberResponseDto.convertToResponseDto(member);

        LoginResponseDto loginResponse = new LoginResponseDto(tokenResponse, memberResponse);
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", loginResponse));
    }

    @PostMapping("/refresh")
    @Operation(summary = "액세스 토큰 갱신", description = "액세스 토큰을 갱신합니다")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto refreshRequest) {
        String refreshToken = refreshRequest.getRefreshToken();

        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new MemberException(ErrorCode.INVALID_TOKEN);
        }

        String userId = jwtUtil.getUserIdFromToken(refreshToken);
        Long memberId = jwtUtil.getMemberIdFromToken(refreshToken);

        if (!memberService.isMemberActive(memberId)) {
            throw new MemberException(ErrorCode.MEMBER_INACTIVE);
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId, memberId);
        TokenResponseDto tokenResponse = new TokenResponseDto(newAccessToken, refreshToken);

        return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", tokenResponse));
    }

    @PostMapping("/validate")
    @Operation(summary = "토큰 검증", description = "테스트용 토큰 검증 API입니다")
    public ResponseEntity<ApiResponse<TokenValidationResponseDto>> validateToken(@RequestParam String token) {

        TokenValidationResponseDto response = tokenValidationService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success("토큰 검증이 완료되었습니다.", response));
    }

}