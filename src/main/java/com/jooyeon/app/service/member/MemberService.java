package com.jooyeon.app.service.member;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.MemberException;
import com.jooyeon.app.domain.dto.member.MemberRegistrationDto;
import com.jooyeon.app.domain.dto.member.MemberResponseDto;
import com.jooyeon.app.domain.dto.member.MemberWithdrawalRequestDto;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import com.jooyeon.app.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private static final int WITHDRAWAL_CANCELLATION_DAYS = 30;
    private final MemberRepository memberRepository;

    @Transactional
    public MemberResponseDto registerMember(MemberRegistrationDto registrationDto) {

        if (memberRepository.existsByUserId(registrationDto.getUserId())) {
            throw new MemberException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        try {
            Member member = new Member();
            member.setUserId(registrationDto.getUserId());
            member.setName(registrationDto.getName());
            member.setPhoneNumber(registrationDto.getPhoneNumber());
            member.setGender(registrationDto.getGender());
            member.setBirthDate(registrationDto.getBirthDate());
            member.setMemberStatus(MemberStatus.ACTIVE);
            member.setCreatedAt(LocalDateTime.now());
            member.setUpdatedAt(LocalDateTime.now());

            Member savedMember = memberRepository.save(member);
            return MemberResponseDto.convertToResponseDto(savedMember);

        } catch (Exception e) {
            log.error("[MEMBER] 회원가입 실패. {}", registrationDto, e);
            throw new MemberException(ErrorCode.MEMBER_REGISTRATION_FAILED, e);
        }
    }

    @Transactional
    public MemberResponseDto requestWithdrawal(Long memberId, MemberWithdrawalRequestDto withdrawalRequest) {
        Member member = findActiveMemberById(memberId);

        if (member.getMemberStatus() == MemberStatus.PENDING_WITHDRAWAL) {
            throw new MemberException(ErrorCode.WITHDRAWAL_ALREADY_PENDING);
        }

        try {
            member.setMemberStatus(MemberStatus.PENDING_WITHDRAWAL);
            member.setWithdrawnAt(LocalDateTime.now());

            Member updatedMember = memberRepository.save(member);
            return MemberResponseDto.convertToResponseDto(updatedMember);

        } catch (Exception e) {
            log.error("[MEMBER] 탈퇴 실패. memberId={}", memberId, e);
            throw new MemberException(ErrorCode.WITHDRAWAL_REQUEST_FAILED, e);
        }
    }

    @Transactional
    public MemberResponseDto cancelWithdrawal(Long memberId) {
        Member member = findMemberById(memberId);

        if (member.getMemberStatus() != MemberStatus.PENDING_WITHDRAWAL) {
            throw new MemberException(ErrorCode.NO_PENDING_WITHDRAWAL);
        }

        LocalDateTime withdrawalRequestedAt = member.getWithdrawnAt();
        if (withdrawalRequestedAt == null ||
            withdrawalRequestedAt.isBefore(LocalDateTime.now().minusDays(WITHDRAWAL_CANCELLATION_DAYS))) {
            throw new MemberException(ErrorCode.CANCELLATION_PERIOD_EXPIRED);
        }

        try {
            member.setMemberStatus(MemberStatus.ACTIVE);
            member.setWithdrawnAt(null);

            Member updatedMember = memberRepository.save(member);
            return MemberResponseDto.convertToResponseDto(updatedMember);

        } catch (Exception e) {
            log.error("[MEMBER] 회원 탈퇴 취소 실패. memberId = {}", memberId, e);
            throw new MemberException(ErrorCode.WITHDRAWAL_CANCELLATION_FAILED, e);
        }
    }

    public MemberResponseDto getMemberById(Long memberId) {
        Member member = findMemberById(memberId);
        return MemberResponseDto.convertToResponseDto(member);
    }


    public Member authenticateMember(String userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new MemberException(ErrorCode.INVALID_CREDENTIALS));

        if (member.getMemberStatus() != MemberStatus.ACTIVE) {
            throw new MemberException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        return member;
    }

    public boolean isMemberActive(Long memberId) {
        try {
            Member member = findMemberById(memberId);
            return member.getMemberStatus() == MemberStatus.ACTIVE;
        } catch (MemberException e) {
            return false;
        }
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Member findActiveMemberById(Long memberId) {
        Member member = findMemberById(memberId);
        if (member.getMemberStatus() != MemberStatus.ACTIVE) {
            throw new MemberException(ErrorCode.MEMBER_NOT_ACTIVE);
        }
        return member;
    }

    public Member findMemberEntityById(Long memberId) {
        return findMemberById(memberId);
    }
}