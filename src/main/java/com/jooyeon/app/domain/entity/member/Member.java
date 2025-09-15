package com.jooyeon.app.domain.entity.member;

import com.jooyeon.app.common.encryption.EncryptedConverter;
import com.jooyeon.app.common.encryption.EncryptedGenderConverter;
import com.jooyeon.app.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@Setter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Convert(converter = EncryptedConverter.class)
    @Column(name = "name", nullable = false)
    private String name;

    @Convert(converter = EncryptedConverter.class)
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Convert(converter = EncryptedGenderConverter.class)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Convert(converter = EncryptedConverter.class)
    @Column(name = "birth_date", nullable = false)
    private String birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false)
    private MemberStatus memberStatus = MemberStatus.ACTIVE;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Version
    private Long version;

}