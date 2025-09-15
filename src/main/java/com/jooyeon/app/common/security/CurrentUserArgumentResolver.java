package com.jooyeon.app.common.security;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.MemberException;
import com.jooyeon.app.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new MemberException(ErrorCode.ACCESS_DENIED);
        }

        String token = getTokenFromRequest(webRequest);
        if (token == null) {
            throw new MemberException(ErrorCode.INVALID_TOKEN);
        }

        try {
            Long memberId = jwtUtil.extractMemberId(token);
            return memberRepository.findById(memberId)
                    .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
        } catch (Exception e) {
            log.error("토큰 파싱 오류", e);
            throw new MemberException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String getTokenFromRequest(NativeWebRequest webRequest) {
        String bearerToken = webRequest.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}