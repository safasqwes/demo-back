package com.novelhub.service;

import com.novelhub.enums.ErrorMessage;
import com.novelhub.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Daily Claim Service
 */
@Slf4j
@Service
public class DailyClaimService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private PointService pointService;

    private static final String CLAIM_KEY_PREFIX = "daily_claim:";
    private static final String STREAK_KEY_PREFIX = "daily_streak:";

    /**
     * User daily claim free points
     * @param userId User ID
     * @return Claim result
     */
    public Map<String, Object> claimFreePoints(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String today = redisUtil.getToday();
            String claimKey = CLAIM_KEY_PREFIX + userId + ":" + today;
            String streakKey = STREAK_KEY_PREFIX + userId;
            
            // Check if already claimed today
            if (redisUtil.hasKey(claimKey)) {
                result.put("success", false);
                result.put("message", ErrorMessage.ALREADY_CLAIMED_TODAY.getMessage());
                return result;
            }
            
            // Get streak days
            Integer streakDays = getStreakDays(userId);
            if (streakDays == null) {
                streakDays = 0;
            }
            
            // Calculate points to claim today
            int pointsToClaim = calculatePointsToClaim(streakDays);
            
            // Add points to user account
            boolean addSuccess = pointService.addPoints(userId, pointsToClaim, 0, "Daily claim reward", "daily_claim_" + today);
            if (!addSuccess) {
                result.put("success", false);
                result.put("message", ErrorMessage.ADD_POINTS_FAILED.getMessage());
                return result;
            }
            
            // Update Redis: mark today as claimed (expires at midnight)
            redisUtil.setUntilMidnight(claimKey, "1");
            
            // Update Redis: update streak days (expires in 30 days)
            redisUtil.set(streakKey, streakDays + 1, 30, TimeUnit.DAYS);
            
            result.put("success", true);
            result.put("message", "Claim successful");
            result.put("points", pointsToClaim);
            result.put("streakDays", streakDays + 1);
            result.put("nextDayPoints", calculatePointsToClaim(streakDays + 1));
            
            log.info("User {} claimed {} points on day {}", userId, pointsToClaim, streakDays + 1);
            
        } catch (Exception e) {
            log.error("Error claiming free points for user: {}", userId, e);
            result.put("success", false);
            result.put("message", ErrorMessage.CLAIM_FAILED.getMessage() + ": " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get user streak days
     * @param userId User ID
     * @return Streak days
     */
    public Integer getStreakDays(Long userId) {
        String streakKey = STREAK_KEY_PREFIX + userId;
        Integer streakDays = redisUtil.get(streakKey, Integer.class);
        return streakDays != null ? streakDays : 0;
    }
    
    /**
     * Check if user has claimed today
     * @param userId User ID
     * @return Has claimed today
     */
    public boolean hasClaimedToday(Long userId) {
        String claimKey = CLAIM_KEY_PREFIX + userId + ":" + redisUtil.getToday();
        return redisUtil.hasKey(claimKey);
    }
    
    /**
     * Calculate points to claim
     * @param streakDays Streak days
     * @return Points amount
     */
    private int calculatePointsToClaim(int streakDays) {
        if (streakDays < 7) {
            // First 7 days: 20 + days * 10
            return 20 + streakDays * 10;
        } else {
            // After 7 days: 100 points per day
            return 100;
        }
    }
    
    /**
     * Get user claim information
     * @param userId User ID
     * @return Claim information
     */
    public Map<String, Object> getClaimInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            Integer streakDays = getStreakDays(userId);
            boolean hasClaimedToday = hasClaimedToday(userId);
            int todayPoints = calculatePointsToClaim(streakDays);
            int nextDayPoints = calculatePointsToClaim(streakDays + 1);
            
            info.put("streakDays", streakDays);
            info.put("hasClaimedToday", hasClaimedToday);
            info.put("todayPoints", todayPoints);
            info.put("nextDayPoints", nextDayPoints);
            info.put("success", true);
            
        } catch (Exception e) {
            log.error("Error getting claim info for user: {}", userId, e);
            info.put("success", false);
            info.put("message", ErrorMessage.GET_CLAIM_INFO_FAILED.getMessage());
        }
        
        return info;
    }
}
