package com.novelhub.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT Utility for token generation and validation
 * Also includes general-purpose token generation utilities
 */
@Slf4j
@Component
public class JwtUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${jwt.secret:aBcDeFgHiJkLmNoPqRsTuVwXyZaBcDeFgHiJkLmNoPaBcDeFgHiJkLs}")
    private String jwtSecret;

    @Value("${jwt.expiration:604800}") // 7 days in seconds
    private long jwtExpiration;

    /**
     * Generate JWT token
     *
     * @param username username
     * @return JWT token
     */
    public String generateToken(String username) {
        return generateToken(username, new HashMap<>());
    }

    /**
     * Generate JWT token with additional claims
     *
     * @param username username
     * @param extraClaims additional claims
     * @return JWT token
     */
    public String generateToken(String username, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("sub", username);
        claims.put("roles", List.of("ROLE_USER"));

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Generate refresh token
     *
     * @param username username
     * @return refresh token
     */
    public String generateRefreshToken(String username) {
        long refreshTokenExpiration = jwtExpiration * 4; // 4x the access token expiration
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration * 1000);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Get username from token
     *
     * @param token JWT token
     * @return username
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Get claims from token
     *
     * @param token JWT token
     * @return claims
     */
    public Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Check if token is expired
        Date expirationDate = claims.getExpiration();
        if (expirationDate.before(new Date())) {
            log.error("JWT token is expired");
            throw new RuntimeException("Expired JWT token");
        }

        return claims;
    }

    /**
     * Validate token
     *
     * @param token JWT token
     * @return true if valid
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ============================================
    // General-purpose token generation methods
    // ============================================

    /**
     * Generate a new random token
     *
     * @return random token
     */
    public String generateNewToken() {
        return "token_" + System.currentTimeMillis() + "_" + generateRandomString(12);
    }

    /**
     * Generate a random string of specified length
     *
     * @param length length of the random string
     * @return random string
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Get username from request (non-throwing version)
     * Returns null if token is missing, invalid, or username cannot be extracted
     * This method does not throw exceptions, it just returns null on any error
     *
     * @param request HttpServletRequest
     * @return username or null if not available
     */
    public String getUsername(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        try {
            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                return null;
            }
            
            // Validate token first
            if (!validateToken(token)) {
                return null;
            }
            
            String username = getUsername(token);
            if (username == null || username.isEmpty()) {
                return null;
            }
            
            return username;
        } catch (Exception e) {
            // Log error but don't throw - just return null
            log.debug("Failed to extract username from request: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get current authenticated username from request attribute
     * This is a validation interceptor method - throws exception if token is invalid or missing
     *
     * Security Notes:
     * - This method should be called after JWT validation (handled by JwtValidationAspect)
     * - The 'currentUsername' attribute is ONLY set by JwtValidationAspect after successful token validation
     * - For @JwtRequired(strict=false), the attribute is explicitly cleared if validation fails
     * - This prevents request attribute pollution and authentication bypass attacks
     * 
     * IMPORTANT: This method throws RuntimeException if:
     * - Request is null
     * - Authorization header is missing or invalid
     * - Token cannot be extracted or validated
     * - Username cannot be extracted from token
     *
     * @param request HttpServletRequest
     * @return username (never null)
     * @throws RuntimeException if authentication fails
     */
    public String validUsername(HttpServletRequest request) {
        if (request == null) {
            log.error("getCurrentUsername called with null request");
            throw new RuntimeException("Authentication required: request is null");
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("getCurrentUsername: Missing or invalid Authorization header");
            throw new RuntimeException("Authentication required: missing or invalid Authorization header");
        }

        try {
            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                log.error("getCurrentUsername: Token is empty");
                throw new RuntimeException("Authentication required: token is empty");
            }
            
            // Validate token first
            if (!validateToken(token)) {
                log.error("getCurrentUsername: Token validation failed");
                throw new RuntimeException("Authentication required: token validation failed");
            }
            
            String username = getUsername(token);
            if (username == null || username.isEmpty()) {
                log.error("getCurrentUsername: Username extracted from token is null or empty");
                throw new RuntimeException("Authentication required: username cannot be extracted from token");
            }
            
            return username;
        } catch (RuntimeException e) {
            // Re-throw RuntimeException as-is
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract username from auth header: {}", e.getMessage());
            throw new RuntimeException("Authentication required: failed to extract username from token - " + e.getMessage(), e);
        }
    }

}

