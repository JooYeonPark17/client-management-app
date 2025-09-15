package com.jooyeon.app.domain.dto.auth;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponseDto {
    private boolean valid;
    private TokenInfoDto tokenInfo;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenInfoDto {
        private Long memberId;
        private String userId;
        private String tokenType;
        private LocalDateTime expiresAt;
    }
}