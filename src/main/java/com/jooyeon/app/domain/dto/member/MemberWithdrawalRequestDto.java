package com.jooyeon.app.domain.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberWithdrawalRequestDto {

    @NotBlank(message = "Withdrawal reason is required")
    @Size(max = 500, message = "Withdrawal reason must not exceed 500 characters")
    private String withdrawalReason;

}