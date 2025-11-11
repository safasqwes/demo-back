package com.novelhub.service;

import com.novelhub.enums.FunctionConfig;
import com.novelhub.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Business Service
 * Handles function usage tracking for guest users (fingerprint-based)
 */
@Slf4j
@Service
public class BusinessService {

    @Autowired
    private RedisUtil redisUtil;

    private static final String GUEST_USAGE_KEY_PREFIX = "guest_usage:";

    /**
     * Check if guest user can use the function
     * @param fingerprint User fingerprint
     * @param functionName Function name
     * @return Map with result (canUse, usageCount, dailyLimit, message)
     */
    public Map<String, Object> checkGuestUsage(String fingerprint, String functionName) {
        Map<String, Object> result = new HashMap<>();
        
        // Find function config
        FunctionConfig config = FunctionConfig.findByName(functionName);
        if (config == null) {
            result.put("canUse", false);
            result.put("message", "Function not found: " + functionName);
            return result;
        }

        // Get current usage count
        String usageKey = buildUsageKey(fingerprint, functionName);
        Integer usageCount = getUsageCount(usageKey);
        
        int dailyLimit = config.getGuestDailyLimit();
        boolean canUse = usageCount < dailyLimit;
        
        result.put("canUse", canUse);
        result.put("usageCount", usageCount);
        result.put("dailyLimit", dailyLimit);
        result.put("remaining", dailyLimit - usageCount);
        
        if (canUse) {
            result.put("message", String.format("You can use this function. %d/%d uses today.", 
                usageCount, dailyLimit));
        } else {
            result.put("message", String.format("Daily limit reached. You have used %d/%d tries today. Please login for unlimited access.", 
                usageCount, dailyLimit));
        }
        
        return result;
    }

    /**
     * Record guest user function usage
     * @param fingerprint User fingerprint
     * @param functionName Function name
     * @return Map with result (success, usageCount, message)
     */
    public Map<String, Object> recordGuestUsage(String fingerprint, String functionName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check if can use
            Map<String, Object> checkResult = checkGuestUsage(fingerprint, functionName);
            if (!(Boolean) checkResult.get("canUse")) {
                result.put("success", false);
                result.put("message", checkResult.get("message"));
                result.put("usageCount", checkResult.get("usageCount"));
                result.put("dailyLimit", checkResult.get("dailyLimit"));
                return result;
            }

            // Increment usage count and set expiry to midnight
            String usageKey = buildUsageKey(fingerprint, functionName);
            Long newCount = redisUtil.incrementUntilMidnight(usageKey);
            
            FunctionConfig config = FunctionConfig.findByName(functionName);
            int dailyLimit = config.getGuestDailyLimit();
            
            result.put("success", true);
            result.put("usageCount", newCount.intValue());
            result.put("dailyLimit", dailyLimit);
            result.put("remaining", dailyLimit - newCount.intValue());
            result.put("message", String.format("Function executed successfully. %d/%d uses today.", 
                newCount, dailyLimit));
            
            log.info("Guest usage recorded - Fingerprint: {}, Function: {}, Count: {}/{}", 
                fingerprint, functionName, newCount, dailyLimit);
            
        } catch (Exception e) {
            log.error("Error recording guest usage - Fingerprint: {}, Function: {}, Error: {}", 
                fingerprint, functionName, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Failed to record usage: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Get current usage count for a guest user and function
     * @param usageKey Redis key
     * @return Usage count
     */
    private Integer getUsageCount(String usageKey) {
        Integer count = redisUtil.get(usageKey, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Build Redis key for guest usage tracking
     * Format: guest_usage:fingerprint:function_name:yyyy-MM-dd
     * @param fingerprint User fingerprint
     * @param functionName Function name
     * @return Redis key
     */
    private String buildUsageKey(String fingerprint, String functionName) {
        return GUEST_USAGE_KEY_PREFIX + fingerprint + ":" + functionName + ":" + redisUtil.getToday();
    }

    /**
     * Get function configuration
     * @param functionName Function name
     * @return Function config or null if not found
     */
    public FunctionConfig getFunctionConfig(String functionName) {
        return FunctionConfig.findByName(functionName);
    }

    /**
     * Get guest usage statistics
     * @param fingerprint User fingerprint
     * @param functionName Function name
     * @return Usage statistics
     */
    public Map<String, Object> getGuestUsageStats(String fingerprint, String functionName) {
        Map<String, Object> stats = new HashMap<>();
        
        FunctionConfig config = FunctionConfig.findByName(functionName);
        if (config == null) {
            stats.put("error", "Function not found");
            return stats;
        }
        
        String usageKey = buildUsageKey(fingerprint, functionName);
        Integer usageCount = getUsageCount(usageKey);
        int dailyLimit = config.getGuestDailyLimit();
        
        stats.put("functionName", functionName);
        stats.put("usageCount", usageCount);
        stats.put("dailyLimit", dailyLimit);
        stats.put("remaining", dailyLimit - usageCount);
        stats.put("canUse", usageCount < dailyLimit);
        
        return stats;
    }
}

