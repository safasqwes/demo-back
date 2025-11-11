package com.novelhub.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novelhub.entity.User;
import com.novelhub.enums.ErrorMessage;
import com.novelhub.mapper.UserMapper;
import com.novelhub.utils.EmailUtil;
import com.novelhub.utils.PasswordUtil;
import com.novelhub.vo.response.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * User Service (using MySQL)
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private EmailUtil emailUtil;

    @Value("${points.verification-token-expiry:86400}")
    private long verificationTokenExpiry;

    /**
     * Ensure user has a valid username, auto-generate if empty
     *
     * @param user User object
     */
    private void ensureUsernameExists(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            String generatedUsername = "user_" + user.getUserId();
            user.setUsername(generatedUsername);
            userMapper.updateById(user);
            log.info(ErrorMessage.AUTO_GENERATED_USERNAME.format(user.getUserId(), generatedUsername));
        }
    }

    /**
     * Register new user
     *
     * @param username Username
     * @param email Email
     * @param password Password (plain text)
     * @return User response
     */
    public UserResponse register(String username, String email, String password) {
        // Validate input
        if (!StringUtils.hasText(username) || !StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn(ErrorMessage.MISSING_REQUIRED_FIELDS.getMessage());
            return null;
        }

        // Check if username already exists
        if (usernameExists(username)) {
            log.warn("Registration failed: username exists - {}", username);
            throw new RuntimeException(ErrorMessage.USERNAME_EXISTS.getMessage());
        }

        // Check if email already exists
        if (emailExists(email)) {
            log.warn("Registration failed: email exists - {}", email);
            throw new RuntimeException(ErrorMessage.EMAIL_EXISTS.getMessage());
        }

        // Create user
        String token = java.util.UUID.randomUUID().toString().replace("-", "");

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordUtil.encode(password))
                .status(1)
                .emailVerified(0)
                .verificationToken(token)
                .tokenExpiry(java.time.LocalDateTime.now().plusSeconds(verificationTokenExpiry))
                .build();

        int result = userMapper.insert(user);
        if (result > 0) {
            // Ensure username is not empty
            ensureUsernameExists(user);
            
            log.info("User registration successful: {}", user.getUsername());
            try {
                emailUtil.sendVerificationEmail(email, user.getUsername(), token, "http://localhost:8080");
            } catch (Exception ignore) {}
            return convertToUserResponse(user);
        }

        log.error(ErrorMessage.USER_REGISTRATION_FAILED.getMessage());
        return null;
    }

    public boolean verifyEmail(String token) {
        if (!org.springframework.util.StringUtils.hasText(token)) return false;
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.eq(User::getVerificationToken, token).eq(User::getEmailVerified, 0);
        User user = userMapper.selectOne(qw);
        if (user == null) return false;
        if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(java.time.LocalDateTime.now())) return false;
        user.setEmailVerified(1);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        return userMapper.updateById(user) > 0;
    }

    /**
     * User authentication
     *
     * @param username Username
     * @param password Password (plain text)
     * @return User response
     */
    public UserResponse authenticate(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn(ErrorMessage.AUTH_MISSING_CREDENTIALS.getMessage());
            return null;
        }

        // Query user
        User user = getUserByUsername(username);
        if (user == null) {
            log.warn(ErrorMessage.AUTH_USER_NOT_FOUND.getMessage() + " - {}", username);
            return null;
        }

        // Check user status
        if (user.getStatus() != 1) {
            log.warn(ErrorMessage.AUTH_USER_DISABLED.getMessage() + " - {}", username);
            throw new RuntimeException(ErrorMessage.AUTH_USER_DISABLED.getMessage());
        }

        // Verify password
        if (!passwordUtil.matches(password, user.getPassword())) {
            log.warn(ErrorMessage.AUTH_WRONG_PASSWORD.getMessage() + " - {}", username);
            return null;
        }

        log.info(ErrorMessage.AUTH_SUCCESS.getMessage() + ": {}", username);
        return convertToUserResponse(user);
    }

    /**
     * Change password
     *
     * @param username Username
     * @param oldPassword Old password
     * @param newPassword New password
     * @return Success status
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            log.warn(ErrorMessage.CHANGE_PASSWORD_MISSING_FIELDS.getMessage());
            return false;
        }

        // Get user
        User user = getUserByUsername(username);
        if (user == null) {
            log.warn(ErrorMessage.CHANGE_PASSWORD_USER_NOT_FOUND.getMessage() + " - {}", username);
            return false;
        }

        // Verify old password
        if (!passwordUtil.matches(oldPassword, user.getPassword())) {
            log.warn(ErrorMessage.CHANGE_PASSWORD_WRONG_OLD_PASSWORD.getMessage() + " - {}", username);
            throw new RuntimeException(ErrorMessage.CHANGE_PASSWORD_WRONG_OLD_PASSWORD.getMessage());
        }

        // Update password
        user.setPassword(passwordUtil.encode(newPassword));
        int result = userMapper.updateById(user);

        if (result > 0) {
            log.info(ErrorMessage.CHANGE_PASSWORD_SUCCESS.getMessage() + ": {}", username);
            return true;
        }

        return false;
    }

    /**
     * Get user profile
     *
     * @param username Username
     * @return User response
     */
    public UserResponse getUserProfile(String username) {
        User user = getUserByUsername(username);
        if (user == null) {
            log.warn(ErrorMessage.GET_USER_PROFILE_NOT_FOUND.getMessage() + " - {}", username);
            return null;
        }
        return convertToUserResponse(user);
    }

    /**
     * Update user profile (JSONObject version)
     *
     * @param username Username
     * @param updates Update fields
     * @return User response
     */
    public UserResponse updateProfile(String username, JSONObject updates) {
        User user = getUserByUsername(username);
        if (user == null) {
            log.warn(ErrorMessage.UPDATE_PROFILE_USER_NOT_FOUND.getMessage() + " - {}", username);
            return null;
        }

        // Update fields
        if (updates.containsKey("nickname")) {
            user.setNickname(updates.getString("nickname"));
        }
        if (updates.containsKey("avatar")) {
            user.setAvatar(updates.getString("avatar"));
        }
        if (updates.containsKey("phone")) {
            user.setPhone(updates.getString("phone"));
        }
        if (updates.containsKey("gender")) {
            user.setGender(updates.getInteger("gender"));
        }
        if (updates.containsKey("introduction")) {
            user.setIntroduction(updates.getString("introduction"));
        }

        int result = userMapper.updateById(user);
        if (result > 0) {
            log.info(ErrorMessage.UPDATE_PROFILE_SUCCESS.getMessage() + ": {}", username);
            return convertToUserResponse(user);
        }

        return null;
    }

    /**
     * Update user profile (simple version)
     *
     * @param username Username
     * @param nickname Nickname
     * @param avatar Avatar
     * @return User response
     */
    public UserResponse updateProfile(String username, String nickname, String avatar) {
        User user = getUserByUsername(username);
        if (user == null) {
            log.warn(ErrorMessage.UPDATE_PROFILE_USER_NOT_FOUND.getMessage() + " - {}", username);
            return null;
        }

        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        }
        if (StringUtils.hasText(avatar)) {
            user.setAvatar(avatar);
        }

        int result = userMapper.updateById(user);
        if (result > 0) {
            log.info(ErrorMessage.UPDATE_PROFILE_SUCCESS.getMessage() + ": {}", username);
            return convertToUserResponse(user);
        }

        return null;
    }

    /**
     * Update last login information
     *
     * @param userId User ID
     * @param loginIp Login IP
     */
    public void updateLastLogin(Long userId, String loginIp) {
        userMapper.updateLastLogin(userId, loginIp);
    }

    /**
     * Get user by username
     *
     * @param username Username
     * @return User entity
     */
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * Get user by email
     *
     * @param email Email
     * @return User entity
     */
    public User getUserByEmail(String email) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * Get user by user ID
     *
     * @param userId User ID
     * @return User entity
     */
    public User getUserById(Long userId) {
        if (userId == null) {
            log.warn(ErrorMessage.GET_USER_FAILED_NULL_ID.getMessage());
            return null;
        }
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserId, userId);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * Check if username exists
     *
     * @param username Username
     * @return Exists
     */
    public boolean usernameExists(String username) {
        return getUserByUsername(username) != null;
    }

    /**
     * Check if email exists
     *
     * @param email Email
     * @return Exists
     */
    public boolean emailExists(String email) {
        return getUserByEmail(email) != null;
    }

    /**
     * Get user ID by username
     *
     * @param username Username
     * @return User ID, null if not exists
     */
    public Long getUserIdByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        
        User user = getUserByUsername(username);
        return user != null ? user.getUserId() : null;
    }

    /**
     * Convert to user response object
     *
     * @param user User entity
     * @return User response
     */
    private UserResponse convertToUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .objectId(user.getUserId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .createdAt(java.sql.Timestamp.valueOf(user.getCreatedAt()))
                .updatedAt(java.sql.Timestamp.valueOf(user.getUpdatedAt()))
                .build();
    }
}

