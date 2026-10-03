package com.appointflow.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, null, null);
    }

    public String generateToken(UserDetails userDetails, Long tenantId) {
        return generateToken(userDetails, tenantId, null);
    }

    public String generateToken(UserDetails userDetails, Long tenantId, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        if (tenantId != null) {
            claims.put("tenant_id", tenantId);
        }
        if (userId != null) {
            claims.put("user_id", userId);
        }
        // Role bilgisini ekle
        if (userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
            claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        }
        claims.put("type", "access");

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(String email, Long tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        if (tenantId != null) {
            claims.put("tenant_id", tenantId);
        }

        return Jwts.builder()
                .subject(email)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getKey())
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public Long extractTenantId(String token) {
        Object tenantId = getClaims(token).get("tenant_id");
        if (tenantId instanceof Number) {
            return ((Number) tenantId).longValue();
        }
        return null;
    }

    public Long extractUserId(String token) {
        Object userId = getClaims(token).get("user_id");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    public String extractTokenType(String token) {
        Object type = getClaims(token).get("type");
        return type != null ? type.toString() : "access";
    }

    public String extractRole(String token) {
        Object role = getClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractEmail(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }
}
