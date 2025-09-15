package com.jooyeon.app.domain.dto.auth;

import com.jooyeon.app.domain.dto.member.MemberResponseDto;
import com.jooyeon.app.domain.dto.member.TokenResponseDto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private TokenResponseDto tokens;
    private MemberResponseDto member;
}