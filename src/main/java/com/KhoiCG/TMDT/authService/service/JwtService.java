package com.KhoiCG.TMDT.authService.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

    // 1. Lấy Secret Key từ file cấu hình (Không Hardcode trong Java)
    @Value("${jwt.secret}")
    private String secretKey;

    // 2. Lấy thời gian hết hạn từ config (Mặc định 10 tiếng nếu thiếu)
    @Value("${jwt.expiration:36000000}")
    private long jwtExpiration;

    // --- GENERATE TOKEN ---

    public String generateToken(String username) {
        return createToken(new HashMap<>(), username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims) // Set các claims tùy chỉnh (nếu có)
                .subject(subject) // Set email/username
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Thời gian hết hạn linh động
                .signWith(getKey()) // JJWT 0.12+ tự động chọn thuật toán dựa trên độ mạnh của Key
                .compact();
    }

    // --- VALIDATE & EXTRACT ---

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    // ⭐ QUAN TRỌNG: Cú pháp mới cho JJWT 0.12.x / 0.13.x
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey()) // Thay cho setSigningKey() cũ
                .build()
                .parseSignedClaims(token) // Thay cho parseClaimsJws() cũ
                .getPayload(); // Thay cho getBody() cũ
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}