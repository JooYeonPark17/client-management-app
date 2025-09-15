package com.jooyeon.app.domain.dto.member;

import com.jooyeon.app.domain.entity.member.Gender;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponseDto {
    private Long memberId;
    private String userId;
    private String name;
    private String phoneNumber;
    private Gender gender;
    private LocalDate birthDate;
    private MemberStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime withdrawalRequestedAt;

    public MemberResponseDto (long memberId, LocalDateTime withdrawalRequestedAt) {
        this.memberId = memberId;
        this.withdrawalRequestedAt = withdrawalRequestedAt;
    }

    public static MemberResponseDto convertToResponseDto(Member member) {
        return new MemberResponseDto(
                member.getId(),
                member.getUserId(),
                member.getName(),
                member.getPhoneNumber(),
                member.getGender(),
                LocalDate.parse(member.getBirthDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                member.getMemberStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.getWithdrawnAt()
        );
    }

}