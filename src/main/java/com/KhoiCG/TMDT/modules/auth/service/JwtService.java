package com.KhoiCG.TMDT.modules.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secretKey;

    @Getter
    @Value("${jwt.expiration:36000000}")
    private long jwtExpiration;

    @Getter
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    // Cached sau @PostConstruct — tránh decode Base64 mỗi request
    private SecretKey cachedKey;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("jwt.secret must be configured");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 256 bits (32 bytes) after base64 decoding");
        }
        cachedKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return generateToken(username, null);
    }

    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        if (role != null && !role.isBlank()) {
            claims.put("role", role);
        }
        claims.put(CLAIM_TYPE, TYPE_ACCESS);
        return buildToken(claims, username, jwtExpiration);
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TYPE_REFRESH);
        return buildToken(claims, username, refreshExpiration);
    }

    public boolean isAccessToken(String token) {
        try {
            String type = extractClaim(token, c -> c.get(CLAIM_TYPE, String.class));
            return TYPE_ACCESS.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String userName = extractUserName(token);
        return userName.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(cachedKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimResolver.apply(claims);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(cachedKey)
                .compact();
    }
}
