package com.novelhub.enums;

/**
 * Response Code Enum
 * Standardized response codes for API responses
 */
public enum ResponseCode {
    
    // Success (200)
    SUCCESS(200, "success", "Operation successful"),
    
    // Client Errors (400-499)
    BAD_REQUEST(400, "bad_request", "Bad request"),
    UNAUTHORIZED(401, "unauthorized", "Unauthorized"),
    FORBIDDEN(403, "forbidden", "Forbidden"),
    NOT_FOUND(404, "not_found", "Resource not found"),
    
    // Business Logic Errors (1000-1999)
    FUNCTION_CONFIG_NOT_FOUND(1001, "function_config_not_found", "Function configuration not found"),
    
    // Points/Quota Errors (2000-2999)
    INSUFFICIENT_POINTS(2001, "insufficient_points", "Insufficient points"),
    EXCEED_FREE_QUOTA(2002, "exceed_free_quota", "Exceeded free quota"),
    EXCEED_DAILY_LIMIT(2003, "exceed_daily_limit", "Exceeded daily limit"),
    QUOTA_EXHAUSTED(2004, "quota_exhausted", "Quota exhausted, please login to continue"),
    
    // Authentication Errors (3000-3099)
    TOKEN_REQUIRED(3001, "token_required", "Token required"),
    TOKEN_INVALID(3002, "token_invalid", "Token invalid or expired, please login again"),
    FINGERPRINT_REQUIRED(3003, "fingerprint_required", "Fingerprint required"),
    FINGERPRINT_INVALID(3004, "fingerprint_invalid", "Invalid fingerprint"),
    
    // Server Errors (500)
    INTERNAL_SERVER_ERROR(500, "internal_server_error", "Internal server error"),
    
    // Unknown Error
    UNKNOWN_ERROR(9999, "unknown_error", "Unknown error");
    
    private final int code;
    private final String name;
    private final String message;
    
    ResponseCode(int code, String name, String message) {
        this.code = code;
        this.name = name;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * Check if code represents success
     */
    public boolean isSuccess() {
        return this.code == 200;
    }
    
    /**
     * Find ResponseCode by code value
     */
    public static ResponseCode findByCode(int code) {
        for (ResponseCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        return UNKNOWN_ERROR;
    }
}

