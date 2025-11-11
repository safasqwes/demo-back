package com.novelhub.utils;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Password Utility
 * Provides password encoding and validation using PBKDF2 (Java standard library)
 * No external dependencies required
 */
@Component
public class PasswordUtil {

    // PBKDF2 参数配置
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16; // 盐值长度（字节）
    private static final int ITERATIONS = 100000; // 迭代次数
    private static final int KEY_LENGTH = 256; // 密钥长度（位）
    private static final String DELIMITER = ":";

    /**
     * Encode a plain text password
     * Format: salt:iterations:hash
     * 
     * @param plainPassword Plain text password
     * @return Encoded password (base64 encoded salt:iterations:hash)
     */
    public String encode(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            // 生成随机盐值
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // 使用 PBKDF2 生成哈希
            byte[] hash = generateHash(plainPassword, salt);

            // 编码格式：salt:iterations:hash (都使用 Base64)
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);
            
            return String.format("%s%s%d%s%s", 
                saltBase64, DELIMITER, ITERATIONS, DELIMITER, hashBase64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode password", e);
        }
    }

    /**
     * Check if a plain text password matches an encoded password
     * 
     * @param plainPassword Plain text password
     * @param encodedPassword Encoded password
     * @return true if password matches, false otherwise
     */
    public boolean matches(String plainPassword, String encodedPassword) {
        if (plainPassword == null || encodedPassword == null) {
            return false;
        }

        try {
            // 解析编码的密码：salt:iterations:hash
            String[] parts = encodedPassword.split(DELIMITER);
            if (parts.length != 3) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            int iterations = Integer.parseInt(parts[1]);
            byte[] storedHash = Base64.getDecoder().decode(parts[2]);

            // 使用相同的盐值和迭代次数生成哈希
            byte[] computedHash = generateHashWithIterations(plainPassword, salt, iterations);

            // 使用常量时间比较防止时序攻击
            return constantTimeEquals(computedHash, storedHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 使用 PBKDF2 生成哈希（默认迭代次数）
     */
    private byte[] generateHash(String password, byte[] salt) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return generateHashWithIterations(password, salt, ITERATIONS);
    }

    /**
     * 使用 PBKDF2 生成哈希（指定迭代次数）
     */
    private byte[] generateHashWithIterations(String password, byte[] salt, int iterations)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * 常量时间比较，防止时序攻击
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}

