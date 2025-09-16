package com.jooyeon.app.domain.dto.member;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithDrawResponseDto {
    private Long memberId;
    private LocalDateTime withdrawalRequestedAt;
    private LocalDateTime cancellationDeadline;
    private String message;
}
