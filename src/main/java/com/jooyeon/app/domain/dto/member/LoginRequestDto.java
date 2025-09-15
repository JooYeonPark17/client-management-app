package com.jooyeon.app.domain.dto.member;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;
}