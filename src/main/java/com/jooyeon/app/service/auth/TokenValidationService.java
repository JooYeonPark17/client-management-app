package com.jooyeon.app.service.auth;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.TokenException;
import com.jooyeon.app.common.security.JwtUtil;
import com.jooyeon.app.domain.dto.auth.TokenValidationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 토큰 검증 서비스
 * JWT 토큰의 유효성을 검증하고 토큰 정보를 추출하는 비즈니스 로직을 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final JwtUtil jwtUtil;

    public TokenValidationResponseDto validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new TokenException(ErrorCode.INVALID_TOKEN);
            }

            Long memberId = extractMemberIdSafely(token);
            String userId = extractUserIdSafely(token);
            String tokenType = extractTokenTypeSafely(token);

            boolean isValid = validateTokenByType(token, tokenType);

            TokenValidationResponseDto.TokenInfoDto tokenInfo = null;
            if (isValid) {
                tokenInfo = TokenValidationResponseDto.TokenInfoDto.builder()
                        .memberId(memberId)
                        .userId(userId)
                        .tokenType(tokenType)
                        .expiresAt(jwtUtil.getExpirationDateFromToken(token))
                        .build();
            }

            TokenValidationResponseDto response = new TokenValidationResponseDto(isValid, tokenInfo);

            return response;
        } catch (TokenException e) {
            log.warn("[TOKEN_VALIDATION] 토큰 검증 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[TOKEN_VALIDATION] 토큰 검증 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new TokenException(ErrorCode.TOKEN_VALIDATION_FAILED, e);
        }
    }

    private boolean validateTokenByType(String token, String tokenType) {
        try {
            if ("ACCESS".equals(tokenType)) {
                return jwtUtil.validateAccessToken(token);
            } else if ("REFRESH".equals(tokenType)) {
                return jwtUtil.validateRefreshToken(token);
            } else {
                log.warn("[TOKEN_VALIDATION] 알 수 없는 토큰 타입: {}", tokenType);
                return false;
            }
        } catch (Exception e) {
            log.warn("[TOKEN_VALIDATION] 토큰 유효성 검증 실패: tokenType={}, error={}", tokenType, e.getMessage());
            return false;
        }
    }

    private Long extractMemberIdSafely(String token) {
        try {
            return jwtUtil.getMemberIdFromToken(token);
        } catch (Exception e) {
            log.warn("[TOKEN_VALIDATION] 멤버 ID 추출 실패: {}", e.getMessage());
            throw new TokenException(ErrorCode.INVALID_TOKEN, e);
        }
    }

    private String extractUserIdSafely(String token) {
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("[TOKEN_VALIDATION] 사용자 ID 추출 실패: {}", e.getMessage());
            throw new TokenException(ErrorCode.INVALID_TOKEN, e);
        }
    }

    private String extractTokenTypeSafely(String token) {
        try {
            return jwtUtil.getTokenTypeFromToken(token);
        } catch (Exception e) {
            log.warn("[TOKEN_VALIDATION] 토큰 타입 추출 실패: {}", e.getMessage());
            throw new TokenException(ErrorCode.INVALID_TOKEN, e);
        }
    }


}