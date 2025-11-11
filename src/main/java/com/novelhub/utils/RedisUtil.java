package com.novelhub.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Utility Class
 * Provides common Redis operations with type safety and error handling
 */
@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ========== Common Operations ==========

    /**
     * Set key-value
     * @param key Key
     * @param value Value
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis set error - key: {}, error: {}", key, e.getMessage(), e);
            throw new RuntimeException("Redis set operation failed", e);
        }
    }

    /**
     * Set key-value with expiration time
     * @param key Key
     * @param value Value
     * @param timeout Timeout value
     * @param unit Time unit
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("Redis set with expire error - key: {}, timeout: {}, error: {}", 
                    key, timeout, e.getMessage(), e);
            throw new RuntimeException("Redis set with expire operation failed", e);
        }
    }

    /**
     * Set key-value with expiration at end of day (midnight)
     * @param key Key
     * @param value Value
     */
    public void setUntilMidnight(String key, Object value) {
        try {
            long secondsUntilMidnight = getSecondsUntilMidnight();
            redisTemplate.opsForValue().set(key, value, secondsUntilMidnight, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis set until midnight error - key: {}, error: {}", key, e.getMessage(), e);
            throw new RuntimeException("Redis set until midnight operation failed", e);
        }
    }

    /**
     * Get value by key
     * @param key Key
     * @return Value
     */
    public Object get(String key) {
        try {
            return key == null ? null : redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis get error - key: {}, error: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get value by key with type casting
     * @param key Key
     * @param clazz Target class
     * @return Value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = get(key);
            return value == null ? null : (T) value;
        } catch (ClassCastException e) {
            log.error("Redis get with type casting error - key: {}, class: {}, error: {}", 
                    key, clazz.getName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Delete key
     * @param key Key
     * @return true if deleted
     */
    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis delete error - key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete multiple keys
     * @param keys Keys
     * @return Number of keys deleted
     */
    public Long delete(Collection<String> keys) {
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            log.error("Redis batch delete error - keys count: {}, error: {}", 
                    keys.size(), e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Check if key exists
     * @param key Key
     * @return true if exists
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis hasKey error - key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Set expiration time
     * @param key Key
     * @param timeout Timeout value
     * @param unit Time unit
     * @return true if successful
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis expire error - key: {}, timeout: {}, error: {}", 
                    key, timeout, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Set expiration time to end of day (midnight)
     * @param key Key
     * @return true if successful
     */
    public Boolean expireAtMidnight(String key) {
        try {
            long secondsUntilMidnight = getSecondsUntilMidnight();
            return redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis expire at midnight error - key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get remaining time to live
     * @param key Key
     * @return TTL in seconds, -1 if no expiration, -2 if key not exists
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis getExpire error - key: {}, error: {}", key, e.getMessage(), e);
            return -2L;
        }
    }

    // ========== Increment/Decrement Operations ==========

    /**
     * Increment value by 1
     * @param key Key
     * @return Value after increment
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key, 1);
        } catch (Exception e) {
            log.error("Redis increment error - key: {}, error: {}", key, e.getMessage(), e);
            throw new RuntimeException("Redis increment operation failed", e);
        }
    }

    /**
     * Increment value by delta
     * @param key Key
     * @param delta Increment value
     * @return Value after increment
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis increment error - key: {}, delta: {}, error: {}", 
                    key, delta, e.getMessage(), e);
            throw new RuntimeException("Redis increment operation failed", e);
        }
    }

    /**
     * Increment value and set expiration to midnight
     * @param key Key
     * @return Value after increment
     */
    public Long incrementUntilMidnight(String key) {
        try {
            Long newValue = increment(key);
            expireAtMidnight(key);
            return newValue;
        } catch (Exception e) {
            log.error("Redis increment until midnight error - key: {}, error: {}", 
                    key, e.getMessage(), e);
            throw new RuntimeException("Redis increment until midnight operation failed", e);
        }
    }

    /**
     * Increment value by delta and set expiration to midnight
     * @param key Key
     * @param delta Increment value
     * @return Value after increment
     */
    public Long incrementUntilMidnight(String key, long delta) {
        try {
            Long newValue = increment(key, delta);
            expireAtMidnight(key);
            return newValue;
        } catch (Exception e) {
            log.error("Redis increment until midnight error - key: {}, delta: {}, error: {}", 
                    key, delta, e.getMessage(), e);
            throw new RuntimeException("Redis increment until midnight operation failed", e);
        }
    }

    /**
     * Decrement value by 1
     * @param key Key
     * @return Value after decrement
     */
    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(key, 1);
        } catch (Exception e) {
            log.error("Redis decrement error - key: {}, error: {}", key, e.getMessage(), e);
            throw new RuntimeException("Redis decrement operation failed", e);
        }
    }

    /**
     * Decrement value by delta
     * @param key Key
     * @param delta Decrement value
     * @return Value after decrement
     */
    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("Redis decrement error - key: {}, delta: {}, error: {}", 
                    key, delta, e.getMessage(), e);
            throw new RuntimeException("Redis decrement operation failed", e);
        }
    }

    // ========== Hash Operations ==========

    /**
     * Get hash value
     * @param key Key
     * @param field Hash field
     * @return Value
     */
    public Object hGet(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (Exception e) {
            log.error("Redis hGet error - key: {}, field: {}, error: {}", 
                    key, field, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Set hash field
     * @param key Key
     * @param field Hash field
     * @param value Value
     */
    public void hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
        } catch (Exception e) {
            log.error("Redis hSet error - key: {}, field: {}, error: {}", 
                    key, field, e.getMessage(), e);
            throw new RuntimeException("Redis hSet operation failed", e);
        }
    }

    /**
     * Get all hash entries
     * @param key Key
     * @return Map of field-value pairs
     */
    public Map<Object, Object> hGetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("Redis hGetAll error - key: {}, error: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Set multiple hash fields
     * @param key Key
     * @param map Map of field-value pairs
     */
    public void hSetAll(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
        } catch (Exception e) {
            log.error("Redis hSetAll error - key: {}, error: {}", key, e.getMessage(), e);
            throw new RuntimeException("Redis hSetAll operation failed", e);
        }
    }

    /**
     * Delete hash field
     * @param key Key
     * @param fields Hash fields
     * @return Number of fields deleted
     */
    public Long hDelete(String key, Object... fields) {
        try {
            return redisTemplate.opsForHash().delete(key, fields);
        } catch (Exception e) {
            log.error("Redis hDelete error - key: {}, error: {}", key, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Check if hash field exists
     * @param key Key
     * @param field Hash field
     * @return true if exists
     */
    public Boolean hHasKey(String key, String field) {
        try {
            return redisTemplate.opsForHash().hasKey(key, field);
        } catch (Exception e) {
            log.error("Redis hHasKey error - key: {}, field: {}, error: {}", 
                    key, field, e.getMessage(), e);
            return false;
        }
    }

    // ========== Set Operations ==========

    /**
     * Add members to set
     * @param key Key
     * @param values Members
     * @return Number of members added
     */
    public Long sAdd(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("Redis sAdd error - key: {}, error: {}", key, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Get all members in set
     * @param key Key
     * @return Set of members
     */
    public Set<Object> sMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Redis sMembers error - key: {}, error: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check if member exists in set
     * @param key Key
     * @param value Member
     * @return true if exists
     */
    public Boolean sIsMember(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error("Redis sIsMember error - key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Remove members from set
     * @param key Key
     * @param values Members
     * @return Number of members removed
     */
    public Long sRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            log.error("Redis sRemove error - key: {}, error: {}", key, e.getMessage(), e);
            return 0L;
        }
    }

    // ========== List Operations ==========

    /**
     * Push value to right end of list
     * @param key Key
     * @param value Value
     * @return List length after push
     */
    public Long lPush(String key, Object value) {
        try {
            return redisTemplate.opsForList().rightPush(key, value);
        } catch (Exception e) {
            log.error("Redis lPush error - key: {}, error: {}", key, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Get list range
     * @param key Key
     * @param start Start index
     * @param end End index
     * @return List of values
     */
    public List<Object> lRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("Redis lRange error - key: {}, start: {}, end: {}, error: {}", 
                    key, start, end, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get list size
     * @param key Key
     * @return List size
     */
    public Long lSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("Redis lSize error - key: {}, error: {}", key, e.getMessage(), e);
            return 0L;
        }
    }

    // ========== Utility Methods ==========

    /**
     * Build key with date suffix (yyyy-MM-dd)
     * @param prefix Key prefix
     * @param suffix Additional suffix (optional)
     * @return Key with date
     */
    public String buildDailyKey(String prefix, String... suffix) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        StringBuilder key = new StringBuilder(prefix).append(today);
        for (String s : suffix) {
            if (s != null && !s.isEmpty()) {
                key.append(":").append(s);
            }
        }
        return key.toString();
    }

    /**
     * Calculate seconds until midnight (next day 00:00:00)
     * @return Seconds until midnight
     */
    public long getSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);
        return Duration.between(now, midnight).getSeconds();
    }

    /**
     * Get current date string (yyyy-MM-dd)
     * @return Date string
     */
    public String getToday() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * Pattern matching - find keys by pattern
     * @param pattern Pattern (e.g., "user:*")
     * @return Set of matching keys
     */
    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Redis keys error - pattern: {}, error: {}", pattern, e.getMessage(), e);
            return null;
        }
    }
}

