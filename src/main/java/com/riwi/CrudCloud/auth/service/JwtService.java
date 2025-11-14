package com.riwi.CrudCloud.auth.service;

import com.riwi.CrudCloud.common.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for JWT token generation and validation
 */
@Service
public class JwtService {

    @Value("${jwt.secret:your-secret-key-change-in-production-must-be-at-least-256-bits}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationInMs;

    /**
     * Generate JWT token for user
     *
     * @param user the user entity
     * @return JWT token string
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("email", user.getEmail());
        claims.put("userType", user.getUserType().toString());

        return createToken(claims, user.getEmail());
    }

    /**
     * Create JWT token
     *
     * @param claims token claims
     * @param subject token subject (usually email)
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Get email from token
     *
     * @param token JWT token
     * @return email from token
     */
    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Get user ID from token
     *
     * @param token JWT token
     * @return user ID from token
     */
    public Integer getUserIdFromToken(String token) {
        return (Integer) getClaimsFromToken(token).get("userId");
    }

    /**
     * Check if token is expired
     *
     * @param token JWT token
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        return getClaimsFromToken(token).getExpiration().before(new Date());
    }

    /**
     * Validate token
     *
     * @param token JWT token
     * @param email user email to match
     * @return true if token is valid
     */
    public boolean validateToken(String token, String email) {
        try {
            String tokenEmail = getEmailFromToken(token);
            return (tokenEmail.equals(email) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract all claims from token
     *
     * @param token JWT token
     * @return claims from token
     */
    private Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
