package com.jooyeon.app.common.security;

import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        Optional<Member> memberOpt = memberRepository.findByUserId(userId);

        if (memberOpt.isEmpty()) {
            throw new UsernameNotFoundException("Member not found with userId: " + userId);
        }

        return new MemberUserDetails(memberOpt.get());
    }
}