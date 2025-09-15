package com.jooyeon.app.domain.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithDrawResponseDto {
    private Long memberId;
    private LocalDateTime withdrawalRequestedAt;
    private LocalDateTime cancellationDeadline;
    private String message;
}
