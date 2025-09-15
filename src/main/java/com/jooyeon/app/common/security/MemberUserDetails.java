package com.jooyeon.app.common.security;

import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class MemberUserDetails implements UserDetails {

    private final Member member;

    public MemberUserDetails(Member member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        // 본  app에서는 password를 사용하지 않습니다.
        return "";
    }

    @Override
    public String getUsername() {
        return member.getPhoneNumber();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return member.getMemberStatus() != MemberStatus.WITHDRAWN;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return member.getMemberStatus() == MemberStatus.ACTIVE;
    }

    public Member getMember() {
        return member;
    }

    public Long getMemberId() {
        return member.getId();
    }
}