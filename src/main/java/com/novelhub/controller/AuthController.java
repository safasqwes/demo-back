package com.novelhub.controller;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import com.novelhub.service.GoogleAuthService;
import com.novelhub.service.UserService;
import com.novelhub.utils.JwtUtil;
import com.novelhub.vo.request.AuthRequest;
import com.novelhub.vo.response.AjaxResult;
import com.novelhub.vo.response.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller
 * Handles user registration, login, password change, and profile management
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * User Registration
     *
     * @param request registration request
     * @return response with user info and tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AjaxResult> register(@RequestBody AuthRequest request) {
        log.info("Registration request for username: {}", request.getUsername());

        // Validate input
        if (!StringUtils.hasText(request.getUsername()) ||
                !StringUtils.hasText(request.getEmail()) ||
                !StringUtils.hasText(request.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(AjaxResult.error("Username, email, and password are required"));
        }

        try {
            // Register user
            UserResponse user = userService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );

            if (user == null) {
                return ResponseEntity.internalServerError()
                        .body(AjaxResult.error("Registration failed"));
            }

            // Generate tokens
            String accessToken = jwtUtil.generateToken(user.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            AjaxResult result = AjaxResult.success("Registration successful")
                    .put("user", user)
                    .put("accessToken", accessToken)
                    .put("refreshToken", refreshToken);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Registration error", e);
            return ResponseEntity.badRequest()
                    .body(AjaxResult.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Registration failed: " + e.getMessage()));
        }
    }

    /**
     * Email Verification
     */
    @GetMapping("/verify-email")
    public ResponseEntity<AjaxResult> verifyEmail(@RequestParam String token) {
        try {
            boolean ok = userService.verifyEmail(token);
            return ok ? ResponseEntity.ok(AjaxResult.success("Email verified"))
                    : ResponseEntity.badRequest().body(AjaxResult.error("Invalid or expired token"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(AjaxResult.error("Verify failed: " + e.getMessage()));
        }
    }

    /**
     * User Login
     * 需要fingerprint校验的敏感操作
     *
     * @param request login request
     * @return response with user info and tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AjaxResult> login(@RequestBody AuthRequest request) {
        log.info("Login request for username: {}", request.getUsername());

        // Validate input
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(AjaxResult.error("Username and password are required"));
        }

        try {
            // Authenticate user
            UserResponse user = userService.authenticate(request.getUsername(), request.getPassword());

            if (user == null) {
                return ResponseEntity.status(401)
                        .body(AjaxResult.error(401, "Invalid username or password"));
            }

            // Generate tokens
            String accessToken = jwtUtil.generateToken(user.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            AjaxResult result = AjaxResult.success("Login successful")
                    .put("user", user)
                    .put("accessToken", accessToken)
                    .put("refreshToken", refreshToken);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Login failed: " + e.getMessage()));
        }
    }

    /**
     * Refresh Access Token
     *
     * @param request refresh token request
     * @return response with new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AjaxResult> refreshToken(@RequestBody AuthRequest request) {
        log.info("Token refresh request");

        if (!StringUtils.hasText(request.getRefreshToken())) {
            return ResponseEntity.badRequest()
                    .body(AjaxResult.error("Refresh token is required"));
        }

        try {
            String username = jwtUtil.getUsername(request.getRefreshToken());
            String newAccessToken = jwtUtil.generateToken(username);
            String newRefreshToken = jwtUtil.generateRefreshToken(username);

            AjaxResult result = AjaxResult.success("Token refreshed")
                    .put("accessToken", newAccessToken)
                    .put("refreshToken", newRefreshToken);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.status(401)
                    .body(AjaxResult.error(401, "Invalid refresh token"));
        }
    }

    /**
     * Change Password
     * 需要fingerprint校验的敏感操作
     *
     * @param request password change request
     * @return response
     */
    @PostMapping("/change-password")
    public ResponseEntity<AjaxResult> changePassword(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        // Get authenticated user
        String username = jwtUtil.validUsername(httpRequest);

        log.info("Password change request for user: {}", username);

        // Validate input
        if (!StringUtils.hasText(request.getOldPassword()) || !StringUtils.hasText(request.getNewPassword())) {
            return ResponseEntity.badRequest()
                    .body(AjaxResult.error("Old password and new password are required"));
        }

        try {
            boolean success = userService.changePassword(
                    username,
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            if (!success) {
                return ResponseEntity.internalServerError()
                        .body(AjaxResult.error("Password change failed"));
            }

            return ResponseEntity.ok(AjaxResult.success("Password changed successfully"));
        } catch (RuntimeException e) {
            log.error("Password change error", e);
            return ResponseEntity.badRequest()
                    .body(AjaxResult.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Password change error", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Password change failed: " + e.getMessage()));
        }
    }

    /**
     * Get User Profile
     *
     * @return user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<AjaxResult> getProfile(HttpServletRequest httpRequest) {
        // Get authenticated user
        String username = jwtUtil.validUsername(httpRequest);

        log.info("Profile request for user: {}", username);

        try {
            UserResponse user = userService.getUserProfile(username);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(AjaxResult.success(user));
        } catch (Exception e) {
            log.error("Get profile error", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Failed to get profile: " + e.getMessage()));
        }
    }

    /**
     * Update User Profile
     * 需要fingerprint校验的敏感操作
     *
     * @param updates fields to update
     * @return updated user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<AjaxResult> updateProfile(@RequestBody JSONObject updates, HttpServletRequest httpRequest) {
        // Get authenticated user
        String username = jwtUtil.validUsername(httpRequest);

        log.info("Profile update request for user: {}", username);

        try {
            UserResponse user = userService.updateProfile(username, updates);

            if (user == null) {
                return ResponseEntity.internalServerError()
                        .body(AjaxResult.error("Profile update failed"));
            }

            return ResponseEntity.ok(AjaxResult.success("Profile updated successfully", user));
        } catch (Exception e) {
            log.error("Update profile error", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Failed to update profile: " + e.getMessage()));
        }
    }

    /**
     * Google OAuth Login (Backend-verified with client-secret)
     * 后端使用client-secret验证Google credential，更安全
     *
     * @param request Google login request with credential
     * @return response with user info and tokens
     */
    @PostMapping("/google/login")
    public ResponseEntity<AjaxResult> googleLogin(@RequestBody Map<String, String> request) {
        log.info("Google login request (backend-verified)");
        log.info("Request body: {}", request);

        String credential = request.get("credential");
        log.info("Credential from request: {}", credential);
        
        if (!StringUtils.hasText(credential)) {
            log.warn("Google credential is missing or empty");
            return ResponseEntity.badRequest()
                    .body(AjaxResult.error("Google credential is required"));
        }

        try {
            // 使用client-secret验证Google credential
            var payload = googleAuthService.verifyGoogleCredential(credential);
            if (payload == null) {
                return ResponseEntity.badRequest()
                        .body(AjaxResult.error("Invalid Google credential"));
            }

            // 创建或获取用户
            Map<String, Object> result = googleAuthService.getOrCreateUserFromGoogle(payload);
            if (!(Boolean) result.get("success")) {
                return ResponseEntity.badRequest()
                        .body(AjaxResult.error((String) result.get("message")));
            }

            return ResponseEntity.ok(AjaxResult.success("Google login successful", result));
        } catch (Exception e) {
            log.error("Google login error", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Google login failed: " + e.getMessage()));
        }
    }

    /**
     * User Logout
     *
     * @return response
     */
    @PostMapping("/logout")
    public ResponseEntity<AjaxResult> logout() {
        log.info("User logout request");
        
        try {
            // 在JWT-based系统中，登出通常由前端处理（删除token）
            // 如果需要服务端登出，可以在这里添加token黑名单逻辑
            
            return ResponseEntity.ok(AjaxResult.success("Logout successful"));
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Logout failed: " + e.getMessage()));
        }
    }


}

