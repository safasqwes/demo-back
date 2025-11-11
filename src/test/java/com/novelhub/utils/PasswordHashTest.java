package com.novelhub.utils;

import org.junit.jupiter.api.Test;

/**
 * 密码哈希测试工具
 */
public class PasswordHashTest {

    private final PasswordUtil passwordUtil = new PasswordUtil();

    @Test
    public void generatePasswordHash() {
        // 生成测试密码的PBKDF2哈希
        String password1 = "password123";
        String password2 = "admin123";

        String hash1 = passwordUtil.encode(password1);
        String hash2 = passwordUtil.encode(password2);

        System.out.println("=== 密码哈希生成 ===");
        System.out.println("password123 -> " + hash1);
        System.out.println("admin123 -> " + hash2);
        System.out.println();

        // 验证新生成的哈希
        System.out.println("=== 验证新生成的哈希 ===");
        boolean matches1 = passwordUtil.matches("password123", hash1);
        boolean matches2 = passwordUtil.matches("admin123", hash2);
        System.out.println("password123验证: " + matches1);
        System.out.println("admin123验证: " + matches2);
        
        // 验证错误密码
        boolean wrongMatch = passwordUtil.matches("wrongpassword", hash1);
        System.out.println("错误密码验证: " + wrongMatch + " (应为 false)");
    }

    @Test
    public void verifyPasswordHashes() {
        System.out.println("=== 测试密码哈希功能 ===");
        
        // 生成哈希并验证
        String password = "test123";
        String hash = passwordUtil.encode(password);
        
        System.out.println("原始密码: " + password);
        System.out.println("生成哈希: " + hash);
        System.out.println("验证结果: " + passwordUtil.matches(password, hash));
    }
}

