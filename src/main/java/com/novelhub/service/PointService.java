package com.novelhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novelhub.entity.PointDetail;
import com.novelhub.entity.UserPoint;
import com.novelhub.mapper.UserPointMapper;
import com.novelhub.mapper.PointDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PointService {

    @Autowired
    private UserPointMapper userPointMapper;

    @Autowired
    private PointDetailMapper pointDetailMapper;

    @Value("${points.daily-claim-amount:100}")
    private int dailyClaimAmount;

    @Value("${points.max-free-points:1000}")
    private int maxFreePoints;

    public UserPoint getUserPoints(Long userId) {
        return userPointMapper.selectOne(new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId));
    }

    public UserPoint initUserPoints(Long userId) {
        UserPoint up = UserPoint.builder()
                .userId(userId)
                .points(0).fixedPoints(0).subPoints(0).subPointsLeft(0)
                .freePoints(0).claimedDays(0)
                .claimedAt(LocalDateTime.of(1970,1,1,0,0))
                .build();
        userPointMapper.insert(up);
        return up;
    }

    @Transactional
    public Map<String,Object> claimDailyPoints(Long userId) {
        Map<String,Object> res = new HashMap<>();
        res.put("success", false);
        UserPoint up = userPointMapper.selectOne(new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId));
        if (up == null) up = initUserPoints(userId);
        LocalDateTime today0 = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        if (up.getClaimedAt() != null && up.getClaimedAt().isAfter(today0)) {
            res.put("message", "今日已领取");
            return res;
        }
        int can = Math.min(dailyClaimAmount, Math.max(0, maxFreePoints - up.getFreePoints()));
        if (can <= 0) { res.put("message", "已达上限"); return res; }
        up.setFreePoints(up.getFreePoints() + can);
        up.setClaimedDays(up.getClaimedDays() + 1);
        up.setClaimedAt(LocalDateTime.now());
        userPointMapper.updateById(up);
        PointDetail detail = PointDetail.builder()
                .userId(userId).points(can).type(1).funcType(0).pointsType(0)
                .taskId("daily_claim").isApi(0).extraData("每日免费积分")
                .build();
        pointDetailMapper.insert(detail);
        res.put("success", true);
        res.put("points", can);
        return res;
    }

    public IPage<PointDetail> getUserPointDetails(Long userId, Integer pageIndex, Integer pageSize) {
        Page<PointDetail> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<PointDetail> qw = new LambdaQueryWrapper<PointDetail>()
                .eq(PointDetail::getUserId, userId)
                .orderByDesc(PointDetail::getCreatedAt);
        return pointDetailMapper.selectPage(page, qw);
    }

    public IPage<PointDetail> getUserPointDetailsWithFilters(Long userId, Integer pageIndex, Integer pageSize, Integer type, Integer pointsType) {
        Page<PointDetail> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<PointDetail> qw = new LambdaQueryWrapper<PointDetail>()
                .eq(PointDetail::getUserId, userId);
        
        if (type != null) {
            qw.eq(PointDetail::getType, type);
        }
        if (pointsType != null) {
            qw.eq(PointDetail::getPointsType, pointsType);
        }
        
        qw.orderByDesc(PointDetail::getCreatedAt);
        return pointDetailMapper.selectPage(page, qw);
    }

    @Transactional
    public Map<String, Object> consumePoints(Long userId, Integer points, Integer funcType, String taskId, String extraData) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        
        try {
            UserPoint up = userPointMapper.selectOne(new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId));
            if (up == null) {
                up = initUserPoints(userId);
            }
            
            // Check if user has enough points
            int totalAvailable = up.getFreePoints() + up.getSubPointsLeft() + up.getPoints();
            if (totalAvailable < points) {
                result.put("message", "Insufficient points");
                return result;
            }
            
            // Consume points with priority: free > sub > fixed
            int remainingPoints = points;
            int pointsType = 0; // Track which type was consumed
            
            // First consume free points
            if (remainingPoints > 0 && up.getFreePoints() > 0) {
                int freeToConsume = Math.min(remainingPoints, up.getFreePoints());
                up.setFreePoints(up.getFreePoints() - freeToConsume);
                remainingPoints -= freeToConsume;
                pointsType = 0; // Free points
            }
            
            // Then consume subscription points
            if (remainingPoints > 0 && up.getSubPointsLeft() > 0) {
                int subToConsume = Math.min(remainingPoints, up.getSubPointsLeft());
                up.setSubPointsLeft(up.getSubPointsLeft() - subToConsume);
                remainingPoints -= subToConsume;
                pointsType = 2; // Subscription points
            }
            
            // Finally consume fixed points
            if (remainingPoints > 0 && up.getPoints() > 0) {
                int fixedToConsume = Math.min(remainingPoints, up.getPoints());
                up.setPoints(up.getPoints() - fixedToConsume);
                remainingPoints -= fixedToConsume;
                pointsType = 1; // Fixed points
            }
            
            // Update user points
            userPointMapper.updateById(up);
            
            // Create point detail record
            PointDetail detail = PointDetail.builder()
                    .userId(userId)
                    .points(points)
                    .type(0) // 0 = consume points
                    .funcType(funcType)
                    .pointsType(pointsType)
                    .taskId(taskId)
                    .isApi(0)
                    .extraData(extraData)
                    .createdAt(LocalDateTime.now())
                    .build();
            pointDetailMapper.insert(detail);
            
            result.put("success", true);
            result.put("pointsConsumed", points);
            result.put("pointsType", pointsType);
            result.put("remainingPoints", up.getPoints() + up.getSubPointsLeft() + up.getFreePoints());
            
        } catch (Exception e) {
            log.error("Failed to consume points for user: {}", userId, e);
            result.put("message", "Failed to consume points");
        }
        
        return result;
    }

    @Transactional
    public boolean refundPoints(Long userId, Integer points, Integer pointsType, String reason) {
        try {
            UserPoint up = userPointMapper.selectOne(new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId));
            if (up == null) {
                up = initUserPoints(userId);
            }
            
            // Add points back based on type
            switch (pointsType) {
                case 0: // FreePoints
                    up.setFreePoints(up.getFreePoints() + points);
                    break;
                case 1: // FixedPoints
                    up.setPoints(up.getPoints() + points);
                    break;
                case 2: // SubPoints
                    up.setSubPointsLeft(up.getSubPointsLeft() + points);
                    break;
                default:
                    up.setPoints(up.getPoints() + points);
                    break;
            }
            
            userPointMapper.updateById(up);
            
            // Create point detail record
            PointDetail detail = PointDetail.builder()
                    .userId(userId)
                    .points(points)
                    .type(1) // 1 = add points
                    .funcType(0) // 0 = refund
                    .pointsType(pointsType)
                    .taskId("refund_" + System.currentTimeMillis())
                    .isApi(0)
                    .extraData(reason)
                    .createdAt(LocalDateTime.now())
                    .build();
            pointDetailMapper.insert(detail);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to refund points for user: {}", userId, e);
            return false;
        }
    }

    @Transactional
    public boolean addPoints(Long userId, int points, int pointsType, String description, String orderNumber) {
        try {
            UserPoint up = userPointMapper.selectOne(new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId));
            if (up == null) up = initUserPoints(userId);

            // Update user points based on type
            switch (pointsType) {
                case 0: // FreePoints
                    up.setFreePoints(up.getFreePoints() + points);
                    break;
                case 1: // FixedPoints
                    up.setFixedPoints(up.getFixedPoints() + points);
                    break;
                case 2: // SubPoints
                    up.setSubPoints(up.getSubPoints() + points);
                    up.setSubPointsLeft(up.getSubPointsLeft() + points);
                    break;
                default:
                    up.setFreePoints(up.getFreePoints() + points);
                    break;
            }
            up.setPoints(up.getPoints() + points);
            userPointMapper.updateById(up);

            // Add point detail record
            PointDetail detail = PointDetail.builder()
                    .userId(userId)
                    .points(points)
                    .type(1) // 1 = add points
                    .funcType(0) // 0 = payment
                    .pointsType(pointsType)
                    .taskId(orderNumber)
                    .isApi(0)
                    .extraData(description)
                    .createdAt(LocalDateTime.now())
                    .build();
            pointDetailMapper.insert(detail);

            return true;
        } catch (Exception e) {
            log.error("Failed to add points for user: {}", userId, e);
            return false;
        }
    }
}


