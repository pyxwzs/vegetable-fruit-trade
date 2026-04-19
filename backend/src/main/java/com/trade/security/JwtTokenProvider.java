package com.trade.security;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_REMEMBER = "rm";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${jwt.refresh-expiration-remember}")
    private long refreshExpirationRemember;

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return buildToken(userPrincipal.getUsername(), jwtExpiration, TYPE_ACCESS, null);
    }

    public String generateRefreshToken(String username, boolean rememberMe) {
        long ttl = Boolean.TRUE.equals(rememberMe) ? refreshExpirationRemember : refreshExpiration;
        return buildToken(username, ttl, TYPE_REFRESH, rememberMe);
    }

    private String buildToken(String subject, long ttlMillis, String type, Boolean rememberMe) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlMillis);
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, type);
        if (rememberMe != null) {
            claims.put(CLAIM_REMEMBER, rememberMe);
        }
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    /** 刷新令牌：校验 refresh JWT 后签发新的 access + refresh（滑动续期） */
    public Map<String, String> rotateTokens(String refreshToken) {
        Claims claims = parseClaims(refreshToken);
        String typ = claims.get(CLAIM_TYPE, String.class);
        if (!TYPE_REFRESH.equals(typ)) {
            throw new JwtException("非刷新令牌");
        }
        String username = claims.getSubject();
        Boolean remember = claims.get(CLAIM_REMEMBER, Boolean.class);
        boolean rememberMe = Boolean.TRUE.equals(remember);
        Map<String, String> out = new HashMap<>();
        out.put("token", buildToken(username, jwtExpiration, TYPE_ACCESS, null));
        out.put("refreshToken", generateRefreshToken(username, rememberMe));
        out.put("type", "Bearer");
        out.put("tokenType", "Bearer");
        return out;
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        }
        return false;
    }

    /** API 请求须使用访问令牌；刷新令牌仅用于 /auth/refresh */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String typ = claims.get(CLAIM_TYPE, String.class);
            return typ == null || TYPE_ACCESS.equals(typ);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }
}
