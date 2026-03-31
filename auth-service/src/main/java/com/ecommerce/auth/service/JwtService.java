package com.ecommerce.auth.service;

import com.ecommerce.auth.model.User;
import com.ecommerce.common.security.JwtConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return buildToken(user, JwtConstants.ACCESS_TOKEN_EXPIRATION_MS);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, JwtConstants.REFRESH_TOKEN_EXPIRATION_MS);
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).get(JwtConstants.CLAIM_USER_ID, String.class);
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get(JwtConstants.CLAIM_EMAIL, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractAllClaims(token).get(JwtConstants.CLAIM_ROLES, List.class);
    }

    private String buildToken(User user, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .toList();

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(JwtConstants.CLAIM_USER_ID, user.getId().toString())
                .claim(JwtConstants.CLAIM_EMAIL, user.getEmail())
                .claim(JwtConstants.CLAIM_ROLES, roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
