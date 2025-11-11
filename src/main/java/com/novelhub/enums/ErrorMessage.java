package com.novelhub.enums;

/**
 * Error Message Enum
 * Centralized management of all error messages
 */
public enum ErrorMessage {
    // Daily Claim Errors
    ALREADY_CLAIMED_TODAY("Already claimed points today"),
    ADD_POINTS_FAILED("Failed to add points"),
    CLAIM_FAILED("Failed to claim points"),
    GET_CLAIM_INFO_FAILED("Failed to get claim information"),
    
    // User Service Errors
    MISSING_REQUIRED_FIELDS("Missing required fields"),
    USERNAME_EXISTS("Username already exists"),
    EMAIL_EXISTS("Email already exists"),
    USER_REGISTRATION_FAILED("User registration failed: database insert failed"),
    AUTH_MISSING_CREDENTIALS("Authentication failed: missing credentials"),
    AUTH_USER_NOT_FOUND("Authentication failed: user not found"),
    AUTH_USER_DISABLED("User account is disabled"),
    AUTH_WRONG_PASSWORD("Authentication failed: incorrect password"),
    AUTH_SUCCESS("User authentication successful"),
    CHANGE_PASSWORD_MISSING_FIELDS("Change password failed: missing required fields"),
    CHANGE_PASSWORD_USER_NOT_FOUND("Change password failed: user not found"),
    CHANGE_PASSWORD_WRONG_OLD_PASSWORD("Old password is incorrect"),
    CHANGE_PASSWORD_SUCCESS("Password changed successfully"),
    GET_USER_PROFILE_NOT_FOUND("Get user profile failed: user not found"),
    UPDATE_PROFILE_USER_NOT_FOUND("Update profile failed: user not found"),
    UPDATE_PROFILE_SUCCESS("User profile updated successfully"),
    GET_USER_FAILED_NULL_ID("Get user failed: user ID is null"),
    AUTO_GENERATED_USERNAME("Auto-generated username for user %s: %s"),
    
    // Web3 Auth Errors
    INVALID_WALLET_ADDRESS("Invalid wallet address"),
    SIGNATURE_VERIFICATION_FAILED("Signature verification failed"),
    WEB3_AUTH_SUCCESS("Web3 user authentication successful"),
    WEB3_AUTH_FAILED("Authentication failed"),
    WEB3_USER_FOUND("Found existing Web3 user"),
    WEB3_USER_CREATED("Created new Web3 user"),
    UPDATE_WALLET_INFO_FAILED("Failed to update user wallet information"),
    
    // General Errors
    OPERATION_FAILED("Operation failed"),
    INTERNAL_ERROR("Internal server error");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }
}

