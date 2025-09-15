package com.jooyeon.app.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jooyeon.app.common.security.JwtUtil;
import com.jooyeon.app.domain.dto.common.ErrorResponseDto;
import com.jooyeon.app.service.member.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
@RequiredArgsConstructor
public class MemberStatusInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final MemberService memberService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        if (isPublicEndpoint(requestURI, method)) {
            return true;
        }

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[멤버_상태_인터셉터] 인증 헤더가 없거나 잘못됨: {}", requestURI);
                writeErrorResponse(response, "Authorization token is required", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
                return false;
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.validateAccessToken(token)) {
                log.warn("[멤버_상태_인터셉터] 잘못되었거나 만료된 토큰: {}", requestURI);
                writeErrorResponse(response, "Invalid or expired token", "INVALID_TOKEN", HttpStatus.UNAUTHORIZED);
                return false;
            }

            Long memberId = jwtUtil.getMemberIdFromToken(token);

            if (!memberService.isMemberActive(memberId)) {
                log.warn("[멤버_상태_인터셉터] 비활성 멤버: memberId={}, requestURI={}", memberId, requestURI);
                writeErrorResponse(response, "Member account is not active", "MEMBER_NOT_ACTIVE", HttpStatus.FORBIDDEN);
                return false;
            }
            request.setAttribute("memberId", memberId);

            return true;

        } catch (Exception e) {
            log.error("[멤버_상태_인터셉터] 멤버 상태 검사 중 오류: {}", requestURI, e);
            writeErrorResponse(response, "Internal server error", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    private boolean isPublicEndpoint(String requestURI, String method) {
        if ("POST".equals(method)) {
            if (requestURI.equals("/api/members/register") ||
                requestURI.equals("/api/auth/login") ||
                requestURI.equals("/api/auth/refresh") ||
                requestURI.equals("/api/auth/validate") ||
                requestURI.equals("/api/auth/logout")) {
                return true;
            }
        }

        if (requestURI.startsWith("/api/v1/payments/mock")) {
            return true;
        }

        if (requestURI.startsWith("/actuator") ||
            requestURI.equals("/health") ||
            requestURI.equals("/") ||
            requestURI.startsWith("/swagger") ||
            requestURI.startsWith("/v3/api-docs")) {
            return true;
        }

        return false;
    }

    private void writeErrorResponse(HttpServletResponse response, String message, String errorCode, HttpStatus status) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDto errorResponse = ErrorResponseDto.of(message, errorCode);
        String responseBody = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(responseBody);
    }
}