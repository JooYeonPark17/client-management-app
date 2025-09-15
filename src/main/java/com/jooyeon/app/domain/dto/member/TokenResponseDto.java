package com.jooyeon.app.domain.dto.member;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    public TokenResponseDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

}