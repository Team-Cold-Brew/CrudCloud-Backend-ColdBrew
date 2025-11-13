package com.riwi.CrudCloud.auth.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.riwi.CrudCloud.auth.model.User;
import com.riwi.CrudCloud.auth.repository.UserRepository;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.AuthException;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.ResourceNotFoundException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Service for generating, validating, and managing JWT tokens
 */
@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationAndValidationPurposesOnly123456}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    @Autowired
    private UserRepository userRepository;

    /**
     * Generate JWT token for a user
     *
     * @param user the user entity
     * @return JWT token string
     */
    public String generateToken(User user) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
            return Jwts.builder()
                    .subject(user.getUserId().toString())
                    .claim("email", user.getEmail())
                    .claim("username", user.getUsername())
                    .claim("userType", user.getUserType().toString())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(key)
                    .compact();
        } catch (Exception e) {
            logger.error("Error generating JWT token", e);
            throw new AuthException("Failed to generate authentication token");
        }
    }

    /**
     * Validate JWT token and extract user ID
     *
     * @param token the JWT token
     * @return user ID
     * @throws AuthException if token is invalid or expired
     */
    public Integer validateAndGetUserId(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Integer.valueOf(claims.getSubject());
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            throw new AuthException("Invalid or expired token");
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty");
            throw new AuthException("Token is malformed");
        }
    }

    /**
     * Validate JWT token without extracting data (just check validity)
     *
     * @param token the JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract claims from JWT token
     *
     * @param token the JWT token
     * @return Claims object
     */
    public Claims extractClaims(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.warn("Failed to extract claims from token");
            throw new AuthException("Invalid token");
        }
    }

    /**
     * Refresh token - validates old token and generates new one
     *
     * @param oldToken the old JWT token
     * @return new JWT token
     */
    public String refreshToken(String oldToken) {
        try {
            Integer userId = validateAndGetUserId(oldToken);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
            return generateToken(user);
        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage());
            throw new AuthException("Failed to refresh token");
        }
    }

    /**
     * Extract user ID from token string (convenience method)
     *
     * @param token the JWT token
     * @return user ID
     */
    public Integer extractUserIdFromToken(String token) {
        return validateAndGetUserId(token);
    }
}
