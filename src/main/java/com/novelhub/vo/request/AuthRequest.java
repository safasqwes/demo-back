package com.novelhub.vo.request;

import lombok.Data;

/**
 * Authentication Request
 */
@Data
public class AuthRequest {
    /**
     * Username or email
     */
    private String username;

    /**
     * Password
     */
    private String password;

    /**
     * Email (for registration)
     */
    private String email;

    /**
     * Refresh token (for token refresh)
     */
    private String refreshToken;

    /**
     * Old password (for password change)
     */
    private String oldPassword;

    /**
     * New password (for password change)
     */
    private String newPassword;

    /**
     * Browser fingerprint
     */
    private String fingerprint;
}

