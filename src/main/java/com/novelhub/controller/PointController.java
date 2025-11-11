package com.novelhub.controller;

import com.alibaba.fastjson2.JSONObject;
import com.novelhub.entity.UserPoint;
import com.novelhub.service.DailyClaimService;
import com.novelhub.service.PointService;
import com.novelhub.service.UserService;
import com.novelhub.vo.response.AjaxResult;
import com.novelhub.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user/points")
public class PointController {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private DailyClaimService dailyClaimService;

    @GetMapping
    public ResponseEntity<AjaxResult> getUserPoints(HttpServletRequest httpRequest) {
        String username = jwtUtil.validUsername(httpRequest);
        try {
            var user = userService.getUserByUsername(username);
            if (user == null) return ResponseEntity.notFound().build();
            Long userId = user.getUserId();
            
            // 获取用户积分
            UserPoint userPoint = pointService.getUserPoints(userId);
            if (userPoint == null) {
                userPoint = pointService.initUserPoints(userId);
            }
            
            // 获取每日签到信息
            Map<String, Object> claimInfo = dailyClaimService.getClaimInfo(userId);
            
            // 合并数据
            var responseData = new JSONObject();
            responseData.put("userPoint", userPoint);
            responseData.put("claimInfo", claimInfo);
            
            return ResponseEntity.ok(AjaxResult.success(responseData));
        } catch (Exception e) {
            log.error("Get user points error", e);
            return ResponseEntity.internalServerError().body(AjaxResult.error("Failed to get user points: " + e.getMessage()));
        }
    }

    /**
     * Get User Points History (detailed version with pagination)
     *
     * @param page page number (default: 1)
     * @param size page size (default: 10)
     * @return response with points history
     */
    @GetMapping("/history")
    public ResponseEntity<AjaxResult> getPointsHistoryDetailed(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        log.info("Get points history request - page: {}, size: {}", page, size);
        
        try {
            // 获取当前用户ID
            String username = jwtUtil.validUsername(httpRequest);
            log.info("Current username: {}", username);
            var user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            Long userId = user.getUserId();
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(AjaxResult.error("User not found"));
            }
            
            // 验证分页参数
            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 10;
            
            // 查询积分历史
            var pointDetails = pointService.getUserPointDetails(userId, page, size);
            
            // 获取用户最新的金币银币数量
            var userPoint = pointService.getUserPoints(userId);
            if (userPoint == null) {
                userPoint = pointService.initUserPoints(userId);
            }
            
            // 构建响应数据
            var responseData = new JSONObject();
            responseData.put("data", pointDetails.getRecords());
            responseData.put("currentPage", pointDetails.getCurrent());
            responseData.put("pageSize", pointDetails.getSize());
            responseData.put("totalPages", pointDetails.getPages());
            responseData.put("totalRecords", pointDetails.getTotal());
            responseData.put("hasNext", pointDetails.getCurrent() < pointDetails.getPages());
            responseData.put("hasPrevious", pointDetails.getCurrent() > 1);
            responseData.put("goldCoins", userPoint.getFixedPoints() != null ? userPoint.getFixedPoints() : 0);
            responseData.put("silverCoins", userPoint.getFreePoints() != null ? userPoint.getFreePoints() : 0);
            responseData.put("totalPoints", userPoint.getPoints() != null ? userPoint.getPoints() : 0);
            
            return ResponseEntity.ok(AjaxResult.success("Points history retrieved successfully", responseData));
        } catch (Exception e) {
            log.error("Get points history error", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Failed to get points history: " + e.getMessage()));
        }
    }

    /**
     * 每日领取银币
     * @return 领取结果
     */
    @PostMapping("/claim-free-points")
    public ResponseEntity<AjaxResult> claimFreePoints(HttpServletRequest httpRequest) {
        log.info("Claim free points request");
        
        try {
            // 获取当前用户ID
            String username = jwtUtil.validUsername(httpRequest);
            var user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            Long userId = user.getUserId();
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(AjaxResult.error("User not found"));
            }
            
            // 调用每日签到服务
            Map<String, Object> claimResult = dailyClaimService.claimFreePoints(userId);
            
            if ((Boolean) claimResult.get("success")) {
                return ResponseEntity.ok(AjaxResult.success((String) claimResult.get("message"), claimResult));
            } else {
                return ResponseEntity.badRequest()
                        .body(AjaxResult.error((String) claimResult.get("message")));
            }
            
        } catch (Exception e) {
            log.error("Claim free points error", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Failed to claim free points: " + e.getMessage()));
        }
    }

}


