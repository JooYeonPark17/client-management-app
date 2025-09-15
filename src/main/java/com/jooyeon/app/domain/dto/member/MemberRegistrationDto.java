package com.jooyeon.app.domain.dto.member;

import com.jooyeon.app.common.validation.BirthDate;
import com.jooyeon.app.common.validation.PhoneNumber;
import com.jooyeon.app.domain.entity.member.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberRegistrationDto {

    @NotBlank(message = "User ID is required")
    @Size(min = 3, max = 50, message = "User ID must be between 3 and 50 characters")
    private String userId;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @PhoneNumber
    private String phoneNumber;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @BirthDate
    private String birthDate;

}