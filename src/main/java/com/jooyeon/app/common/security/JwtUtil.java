package com.jooyeon.app.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    //24시간
    @Value("${jwt.expiration:86400000}")
    private long accessTokenExpiration;

    //7일
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, Long memberId) {
        return generateToken(userId, memberId, accessTokenExpiration, "ACCESS");
    }

    public String generateRefreshToken(String userId, Long memberId) {
        return generateToken(userId, memberId, refreshTokenExpiration, "REFRESH");
    }

    private String generateToken(String userId, Long memberId, long expiration, String tokenType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDateTime = now.plusSeconds(expiration / 1000);

        Date issuedAt = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date expiryDate = Date.from(expiryDateTime.atZone(ZoneId.systemDefault()).toInstant());

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);
        claims.put("tokenType", tokenType);

        return Jwts.builder()
                .claim("memberId", memberId)
                .claim("tokenType", tokenType)
                .subject(userId)
                .issuedAt(issuedAt)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public Long getMemberIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("memberId", Long.class);
    }

    public Long extractMemberId(String token) {
        return getMemberIdFromToken(token);
    }

    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tokenType", String.class);
    }

    public LocalDateTime getExpirationDateFromToken(String token) {
        Date expiration = getClaimsFromToken(token).getExpiration();
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaimsFromToken(token).getExpiration();
            LocalDateTime expirationDateTime = expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            return expirationDateTime.isBefore(LocalDateTime.now());
        } catch (JwtException e) {
            return true;
        }
    }

    public boolean validateToken(String token, String userId) {
        try {
            String tokenUserId = getUserIdFromToken(token);
            return tokenUserId.equals(userId) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            String tokenType = getTokenTypeFromToken(token);
            return "ACCESS".equals(tokenType) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            String tokenType = getTokenTypeFromToken(token);
            return "REFRESH".equals(tokenType) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}