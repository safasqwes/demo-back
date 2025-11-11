package com.novelhub.controller;

import com.alibaba.fastjson2.JSON;
import com.novelhub.enums.FunctionConfig;
import com.novelhub.enums.PointsType;
import com.novelhub.enums.ResponseCode;
import com.novelhub.service.BusinessService;
import com.novelhub.service.ImageUploadService;
import com.novelhub.service.ReplicateService;
import com.novelhub.utils.FingerprintUtil;
import com.novelhub.utils.JwtUtil;
import com.novelhub.vo.response.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business Controller
 * Handles business function calls with guest usage tracking
 */
@Slf4j
@RestController
@RequestMapping("/api/business")
public class BusinessController {

    @Value("${replicate.webhook.enabled}")
    private boolean webhookEnabled;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ReplicateService replicateService;

    @Autowired
    private ImageUploadService imageUploadService;

    /**
     * Demo test function
     * Priority: Token (authenticated) > Fingerprint (guest) > Error
     * Guests can use this function with daily limit (5 times/day)
     * Authenticated users can use unlimited with points cost
     */
    @PostMapping("/demo-test")
    public ResponseEntity<AjaxResult> demoTest(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> requestData) {
        try {
            // Get function configuration
            FunctionConfig config = businessService.getFunctionConfig("demo-test");
            if (config == null) {
                return ResponseEntity.ok(
                    AjaxResult.error(ResponseCode.FUNCTION_CONFIG_NOT_FOUND.getCode(), 
                                   ResponseCode.FUNCTION_CONFIG_NOT_FOUND.getMessage(), null)
                );
            }
            
            // Step 1: Try to get username from JWT token (returns null if not authenticated)
            String username = jwtUtil.validUsername(request);

            if (username != null) {
                // Authenticated user - execute business logic with points cost
                log.info("Demo test called by authenticated user: {}", username);
                
                // TODO: Get user's actual points from database
                // For demo purposes, assume user has some points
                int userFreePoints = 100;   // User's silver coins
                int userFixedPoints = 10;   // User's gold coins
                
                // Check if user has enough points
                if (!config.hasEnoughPoints(userFreePoints, userFixedPoints)) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("code", ResponseCode.INSUFFICIENT_POINTS.getName());
                    errorData.put("message", ResponseCode.INSUFFICIENT_POINTS.getMessage());
                    errorData.put("required", config.getCostDisplay());
                    errorData.put("yourFreePoints", userFreePoints);
                    errorData.put("yourFixedPoints", userFixedPoints);
                    return ResponseEntity.ok(AjaxResult.error(ResponseCode.INSUFFICIENT_POINTS.getCode(), 
                                                              ResponseCode.INSUFFICIENT_POINTS.getMessage(), 
                                                              errorData));
                }
                
                // Determine which type of points to deduct
                Integer pointsType = config.determinePointsType(userFreePoints, userFixedPoints);
                if (pointsType == null) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("code", ResponseCode.INSUFFICIENT_POINTS.getName());
                    errorData.put("message", ResponseCode.INSUFFICIENT_POINTS.getMessage());
                    errorData.put("required", config.getCostDisplay());
                    errorData.put("yourFreePoints", userFreePoints);
                    errorData.put("yourFixedPoints", userFixedPoints);
                    return ResponseEntity.ok(AjaxResult.error(ResponseCode.INSUFFICIENT_POINTS.getCode(), 
                                                              ResponseCode.INSUFFICIENT_POINTS.getMessage(), 
                                                              errorData));
                }
                
                int pointsDeducted = pointsType == PointsType.FREE.getCode() 
                    ? config.getFreePointsCost() 
                    : config.getFixedPointsCost();
                
                // TODO: Actually deduct points from user account in database
                // pointService.deductPoints(userId, pointsType, pointsDeducted);
                
                // TODO: Log function usage to database
                // FunctionUsageLog log = FunctionUsageLog.builder()
                //     .userId(userId)
                //     .functionType(config.getFunctionType())  // integer: 1001, 1002, etc.
                //     .pointsType(pointsType)                   // integer: 1 (free) or 2 (fixed)
                //     .pointsCost(pointsDeducted)
                //     .build();
                // usageLogMapper.insert(log);
                
                Map<String, Object> result = new HashMap<>();
                result.put("authenticated", true);
                result.put("username", username);
                result.put("message", "Demo test executed successfully");
                result.put("businessResult", "This is a demo business function result for authenticated user");
                result.put("timestamp", System.currentTimeMillis());
                result.put("functionType", config.getFunctionType());
                result.put("pointsDeducted", pointsDeducted);
                result.put("pointsType", pointsType);
                result.put("pointsTypeName", PointsType.findByCode(pointsType).getName());
                result.put("remainingFreePoints", pointsType == PointsType.FREE.getCode() 
                    ? userFreePoints - pointsDeducted : userFreePoints);
                result.put("remainingFixedPoints", pointsType == PointsType.FIXED.getCode() 
                    ? userFixedPoints - pointsDeducted : userFixedPoints);
                
                return ResponseEntity.ok(AjaxResult.success(result));
            }
                
            // Step 2: No token, try to get fingerprint for guest access
                String fingerprint = FingerprintUtil.extractAndValidateFingerprint(request);
                
            if (fingerprint != null && !fingerprint.isEmpty()) {
                // Guest user - check daily limit
                log.info("Demo test called by guest user with fingerprint: {}", fingerprint);
                
                // Record usage (this also checks if limit is reached)
                Map<String, Object> usageResult = businessService.recordGuestUsage(fingerprint, "demo-test");
                
                if (!(Boolean) usageResult.get("success")) {
                    // Guest limit reached - daily limit exceeded
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("code", ResponseCode.EXCEED_DAILY_LIMIT.getName());
                    errorData.put("message", ResponseCode.EXCEED_DAILY_LIMIT.getMessage());
                    errorData.put("usageCount", usageResult.get("usageCount"));
                    errorData.put("dailyLimit", usageResult.get("dailyLimit"));
                    errorData.put("remaining", usageResult.get("remaining"));
                    errorData.put("requireLogin", true);
                    return ResponseEntity.ok(AjaxResult.error(ResponseCode.EXCEED_DAILY_LIMIT.getCode(), 
                                                              ResponseCode.EXCEED_DAILY_LIMIT.getMessage(), 
                                                              errorData));
                }
                
                // TODO: Log guest function usage to database
                // FunctionUsageLog log = FunctionUsageLog.builder()
                //     .fingerprint(fingerprint)
                //     .functionType(config.getFunctionType())          // integer: 1001, 1002, etc.
                //     .pointsType(config.getGuestPointsType())         // integer: 0 (trial)
                //     .pointsCost(0)
                //     .build();
                // usageLogMapper.insert(log);
                
                // Execute business logic for guest
                Map<String, Object> result = new HashMap<>();
                result.put("authenticated", false);
                result.put("fingerprint", fingerprint);
                result.put("message", "Demo test executed successfully");
                result.put("businessResult", "This is a demo business function result for guest user");
                result.put("timestamp", System.currentTimeMillis());
                result.put("functionType", config.getFunctionType());
                result.put("pointsType", config.getGuestPointsType());
                result.put("pointsTypeName", PointsType.TRIAL.getName());
                result.put("usageInfo", usageResult);
                
                return ResponseEntity.ok(AjaxResult.success(result));
            }
            
            // Step 3: No token and no fingerprint - quota exhausted
            log.warn("Demo test called without valid token or fingerprint");
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("code", ResponseCode.QUOTA_EXHAUSTED.getName());
            errorData.put("message", ResponseCode.QUOTA_EXHAUSTED.getMessage());
            errorData.put("requireLogin", true);
            return ResponseEntity.ok(AjaxResult.error(ResponseCode.QUOTA_EXHAUSTED.getCode(), 
                                                      ResponseCode.QUOTA_EXHAUSTED.getMessage(), 
                                                      errorData));
            
        } catch (Exception e) {
            log.error("Error executing demo test", e);
            return ResponseEntity.ok(
                AjaxResult.error(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), 
                               ResponseCode.INTERNAL_SERVER_ERROR.getMessage() + ": " + e.getMessage(), 
                               null)
            );
        }
    }

    /**
     * Check guest usage status for a function
     * No authentication required
     */
    @GetMapping("/guest-usage/{functionName}")
    public ResponseEntity<AjaxResult> checkGuestUsage(
            HttpServletRequest request,
            @PathVariable String functionName) {
        try {
            String fingerprint = FingerprintUtil.extractAndValidateFingerprint(request);
            
            if (fingerprint == null || fingerprint.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    AjaxResult.error("Valid fingerprint required")
                );
            }
            
            Map<String, Object> stats = businessService.getGuestUsageStats(fingerprint, functionName);
            
            if (stats.containsKey("error")) {
                return ResponseEntity.badRequest().body(
                    AjaxResult.error((String) stats.get("error"))
                );
            }
            
            return ResponseEntity.ok(AjaxResult.success(stats));
            
        } catch (Exception e) {
            log.error("Error checking guest usage", e);
            return ResponseEntity.internalServerError().body(
                AjaxResult.error("Failed to check guest usage: " + e.getMessage())
            );
        }
    }

    /**
     * Get all available functions and their configurations
     * No authentication required
     */
    @GetMapping("/functions")
    public ResponseEntity<AjaxResult> getFunctions() {
        try {
            Map<String, Object> functions = new HashMap<>();
            
            for (FunctionConfig config : FunctionConfig.values()) {
                Map<String, Object> funcInfo = new HashMap<>();
                funcInfo.put("functionType", config.getFunctionType());
                funcInfo.put("functionName", config.getFunctionName());
                
                // Scene information
                Map<String, Object> sceneInfo = new HashMap<>();
                sceneInfo.put("code", config.getScene());
                sceneInfo.put("name", config.getSceneType().getName());
                sceneInfo.put("description", config.getSceneType().getDescription());
                funcInfo.put("scene", sceneInfo);
                
                // Model information
                Map<String, Object> modelInfo = new HashMap<>();
                modelInfo.put("code", config.getModel());
                modelInfo.put("name", config.getModelType().getName());
                modelInfo.put("provider", config.getModelType().getProvider());
                modelInfo.put("description", config.getModelType().getDescription());
                funcInfo.put("model", modelInfo);
                
                funcInfo.put("guestDailyLimit", config.getGuestDailyLimit());
                funcInfo.put("freePointsCost", config.getFreePointsCost());
                funcInfo.put("fixedPointsCost", config.getFixedPointsCost());
                funcInfo.put("costDisplay", config.getCostDisplay());
                
                // Pricing details with points type codes
                Map<String, Object> pricing = new HashMap<>();
                if (config.getFreePointsCost() > 0) {
                    Map<String, Object> freePricing = new HashMap<>();
                    freePricing.put("cost", config.getFreePointsCost());
                    freePricing.put("type", PointsType.FREE.getCode());  // 1
                    freePricing.put("typeName", PointsType.FREE.getName());
                    pricing.put("silverCoins", freePricing);
                }
                if (config.getFixedPointsCost() > 0) {
                    Map<String, Object> fixedPricing = new HashMap<>();
                    fixedPricing.put("cost", config.getFixedPointsCost());
                    fixedPricing.put("type", PointsType.FIXED.getCode());  // 2
                    fixedPricing.put("typeName", PointsType.FIXED.getName());
                    pricing.put("goldCoins", fixedPricing);
                }
                if (config.getGuestDailyLimit() > 0) {
                    Map<String, Object> trialPricing = new HashMap<>();
                    trialPricing.put("dailyLimit", config.getGuestDailyLimit());
                    trialPricing.put("type", PointsType.TRIAL.getCode());  // 0
                    trialPricing.put("typeName", PointsType.TRIAL.getName());
                    pricing.put("trial", trialPricing);
                }
                funcInfo.put("pricing", pricing);
                
                functions.put(config.getFunctionName(), funcInfo);
            }
            
            return ResponseEntity.ok(AjaxResult.success(functions));
            
        } catch (Exception e) {
            log.error("Error getting functions", e);
            return ResponseEntity.internalServerError().body(
                AjaxResult.error("Failed to get functions: " + e.getMessage())
            );
        }
    }

    /**
     * Nano Banana image generation/editing function
     * Supports both authenticated users (with points) and guest users (with daily limit)
     * Priority: Token (authenticated) > Fingerprint (guest) > Error
     */
    @PostMapping("/nano-banana")
    public ResponseEntity<AjaxResult> nanoBanana(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> requestData) {
        try {
            // Get function configuration
            FunctionConfig config = businessService.getFunctionConfig("nano-banana");
            if (config == null) {
                return ResponseEntity.ok(
                    AjaxResult.error(ResponseCode.FUNCTION_CONFIG_NOT_FOUND.getCode(), 
                                   ResponseCode.FUNCTION_CONFIG_NOT_FOUND.getMessage(), null)
                );
            }

            // Validate request data
            if (requestData == null) {
                return ResponseEntity.ok(
                    AjaxResult.error(ResponseCode.BAD_REQUEST.getCode(), 
                                   "Request data is required", null)
                );
            }

            String prompt = (String) requestData.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.ok(
                    AjaxResult.error(ResponseCode.BAD_REQUEST.getCode(), 
                                   "Prompt is required", null)
                );
            }

            // Support both single image URL (backward compatibility) and multiple image URLs
            String imageUrl = (String) requestData.get("imageUrl");
            List<String> imageUrls = null;
            Object imageUrlsObj = requestData.get("imageUrls");
            if (imageUrlsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> rawList = (List<Object>) imageUrlsObj;
                imageUrls = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof String) {
                        imageUrls.add((String) item);
                    }
                }
            }
            // If single image URL provided, use it; otherwise use imageUrls array
            if (imageUrl != null && !imageUrl.isEmpty() && (imageUrls == null || imageUrls.isEmpty())) {
                imageUrls = new ArrayList<>();
                imageUrls.add(imageUrl);
            }
            String aspectRatio = (String) requestData.get("aspectRatio");

            // Step 1: Try to get username from JWT token (returns null if not authenticated)
            String username = jwtUtil.getUsername(request);

            if (username != null) {
                // Authenticated user - execute with points cost
                log.info("Nano Banana called by authenticated user: {}", username);
                
                // TODO: Get user's actual points from database
                int userFreePoints = 100;   // User's silver coins
                int userFixedPoints = 10;   // User's gold coins
                
                // Check if user has enough points
                if (!config.hasEnoughPoints(userFreePoints, userFixedPoints)) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("code", ResponseCode.INSUFFICIENT_POINTS.getName());
                    errorData.put("message", ResponseCode.INSUFFICIENT_POINTS.getMessage());
                    errorData.put("required", config.getCostDisplay());
                    errorData.put("yourFreePoints", userFreePoints);
                    errorData.put("yourFixedPoints", userFixedPoints);
                    return ResponseEntity.ok(AjaxResult.error(ResponseCode.INSUFFICIENT_POINTS.getCode(), 
                                                              ResponseCode.INSUFFICIENT_POINTS.getMessage(), 
                                                              errorData));
                }
                
                // Determine which type of points to deduct
                Integer pointsType = config.determinePointsType(userFreePoints, userFixedPoints);
                if (pointsType == null) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("code", ResponseCode.INSUFFICIENT_POINTS.getName());
                    errorData.put("message", ResponseCode.INSUFFICIENT_POINTS.getMessage());
                    errorData.put("required", config.getCostDisplay());
                    errorData.put("yourFreePoints", userFreePoints);
                    errorData.put("yourFixedPoints", userFixedPoints);
                    return ResponseEntity.ok(AjaxResult.error(ResponseCode.INSUFFICIENT_POINTS.getCode(), 
                                                              ResponseCode.INSUFFICIENT_POINTS.getMessage(), 
                                                              errorData));
                }
                
                int pointsDeducted = pointsType == PointsType.FREE.getCode() 
                    ? config.getFreePointsCost() 
                    : config.getFixedPointsCost();
                
                // TODO: Actually deduct points from user account in database
                // pointService.deductPoints(userId, pointsType, pointsDeducted);
                
                // Call Replicate API
                Map<String, Object> apiResult = replicateService.generateImage(prompt, imageUrls, aspectRatio);
                
                if (!(Boolean) apiResult.get("success")) {
                    // API call failed - don't deduct points
                    String error = (String) apiResult.get("error");
                    log.error("Replicate API call failed: {}", error);
                    return ResponseEntity.ok(
                        AjaxResult.error(ResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                                       "Image generation failed: " + error,
                                       null)
                    );
                }
                
                // TODO: Log function usage to database
                
                Map<String, Object> result = new HashMap<>();
                result.put("authenticated", true);
                result.put("username", username);
                result.put("message", "Image generated successfully");
                result.put("imageUrl", apiResult.get("imageUrl"));
                result.put("predictionId", apiResult.get("predictionId"));
                result.put("timestamp", System.currentTimeMillis());
                result.put("functionType", config.getFunctionType());
                result.put("pointsDeducted", pointsDeducted);
                result.put("pointsType", pointsType);
                result.put("pointsTypeName", PointsType.findByCode(pointsType).getName());
                result.put("remainingFreePoints", pointsType == PointsType.FREE.getCode() 
                    ? userFreePoints - pointsDeducted : userFreePoints);
                result.put("remainingFixedPoints", pointsType == PointsType.FIXED.getCode() 
                    ? userFixedPoints - pointsDeducted : userFixedPoints);
                
                return ResponseEntity.ok(AjaxResult.success(result));
            }
            
            // Step 2: No token, try to get fingerprint for guest access
            String fingerprint = FingerprintUtil.extractAndValidateFingerprint(request);
            
            if (fingerprint != null && !fingerprint.isEmpty()) {
                // Guest user - check daily limit
                log.info("Nano Banana called by guest user with fingerprint: {}", fingerprint);
                
                // Record usage (this also checks if limit is reached)
                Map<String, Object> usageResult = businessService.recordGuestUsage(fingerprint, "nano-banana");
                
                if (!(Boolean) usageResult.get("success")) {
                    // Guest limit reached - daily limit exceeded
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("code", ResponseCode.EXCEED_DAILY_LIMIT.getName());
                    errorData.put("message", ResponseCode.EXCEED_DAILY_LIMIT.getMessage());
                    errorData.put("usageCount", usageResult.get("usageCount"));
                    errorData.put("dailyLimit", usageResult.get("dailyLimit"));
                    errorData.put("remaining", usageResult.get("remaining"));
                    errorData.put("requireLogin", true);
                    return ResponseEntity.ok(AjaxResult.error(ResponseCode.EXCEED_DAILY_LIMIT.getCode(), 
                                                              ResponseCode.EXCEED_DAILY_LIMIT.getMessage(), 
                                                              errorData));
                }
                
                // TODO: Log guest function usage to database
                
                // Call Replicate API
                Map<String, Object> apiResult = replicateService.generateImage(prompt, imageUrls, aspectRatio);
                
                if (!(Boolean) apiResult.get("success")) {
                    // API call failed
                    String error = (String) apiResult.get("error");
                    log.error("Replicate API call failed: {}", error);
                    return ResponseEntity.ok(
                        AjaxResult.error(ResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                                       "Image generation failed: " + error,
                                       null)
                    );
                }
                
                // Execute business logic for guest
                Map<String, Object> result = new HashMap<>();
                result.put("authenticated", false);
                result.put("fingerprint", fingerprint);
                result.put("message", "Image generated successfully");
                result.put("imageUrl", apiResult.get("imageUrl"));
                result.put("predictionId", apiResult.get("predictionId"));
                result.put("timestamp", System.currentTimeMillis());
                result.put("functionType", config.getFunctionType());
                result.put("pointsType", config.getGuestPointsType());
                result.put("pointsTypeName", PointsType.TRIAL.getName());
                result.put("usageInfo", usageResult);
                
                return ResponseEntity.ok(AjaxResult.success(result));
            }
            
            // Step 3: No token and no fingerprint - quota exhausted
            log.warn("Nano Banana called without valid token or fingerprint");
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("code", ResponseCode.QUOTA_EXHAUSTED.getName());
            errorData.put("message", ResponseCode.QUOTA_EXHAUSTED.getMessage());
            errorData.put("requireLogin", true);
            return ResponseEntity.ok(AjaxResult.error(ResponseCode.QUOTA_EXHAUSTED.getCode(), 
                                                      ResponseCode.QUOTA_EXHAUSTED.getMessage(), 
                                                      errorData));
            
        } catch (Exception e) {
            log.error("Error executing nano banana", e);
            return ResponseEntity.ok(
                AjaxResult.error(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), 
                               ResponseCode.INTERNAL_SERVER_ERROR.getMessage() + ": " + e.getMessage(), 
                               null)
            );
        }
    }

    /**
     * Get prediction status for Nano Banana
     * No authentication required (for polling)
     */
    @GetMapping("/nano-banana/status/{predictionId}")
    public ResponseEntity<AjaxResult> getPredictionStatus(
            @PathVariable String predictionId) {
        try {
            Map<String, Object> result = replicateService.getPredictionStatus(predictionId);
            
            if (!(Boolean) result.get("success")) {
                String error = (String) result.get("error");
                return ResponseEntity.ok(
                    AjaxResult.error(ResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                                   "Failed to get prediction status: " + error,
                                   null)
                );
            }
            
            return ResponseEntity.ok(AjaxResult.success(result));
            
        } catch (Exception e) {
            log.error("Error getting prediction status", e);
            return ResponseEntity.ok(
                AjaxResult.error(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), 
                               "Failed to get prediction status: " + e.getMessage(), 
                               null)
            );
        }
    }

    @PostMapping("/callback")
    public ResponseEntity<AjaxResult> receiveWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        try {
            // Check if callback is enabled
            if (!webhookEnabled) {
                log.debug("Callback received but disabled in configuration");
                return ResponseEntity.ok(AjaxResult.success("Callback disabled"));
            }

            // Log callback received
            log.info("Replicate callback received: {}", JSON.toJSONString(payload));

            if (payload == null || payload.isEmpty()) {
                log.warn("Received empty callback payload");
                return ResponseEntity.badRequest().body(
                        AjaxResult.error("Empty callback payload")
                );
            }

            // Extract webhook data
            String id = (String) payload.get("id");
            String status = (String) payload.get("status");
            Object output = payload.get("output");
            String error = (String) payload.get("error");

            log.info("Webhook data - ID: {}, Status: {}, Error: {}", id, status, error);

            // Process webhook based on status
            Map<String, Object> result = new HashMap<>();
            result.put("id", id);
            result.put("status", status);
            result.put("received", true);
            result.put("timestamp", System.currentTimeMillis());

            if (status != null) {
                switch (status) {
                    case "starting":
                        log.info("Prediction {} is starting", id);
                        break;
                    case "processing":
                        log.info("Prediction {} is processing", id);
                        break;
                    case "succeeded":
                        log.info("Prediction {} succeeded", id);
                        if (output != null) {
                            result.put("output", output);
                            log.info("Output: {}", output);
                        }
                        break;
                    case "failed":
                    case "canceled":
                        log.warn("Prediction {} {}: {}", id, status, error);
                        result.put("error", error);
                        break;
                    default:
                        log.debug("Prediction {} status: {}", id, status);
                }
            }

            // TODO: Update prediction status in database or cache
            // TODO: Notify frontend via WebSocket or polling
            // TODO: Process the output if prediction succeeded

            return ResponseEntity.ok(AjaxResult.success(result));

        } catch (Exception e) {
            log.error("Error processing callback", e);
            return ResponseEntity.internalServerError().body(
                    AjaxResult.error("Failed to process callback: " + e.getMessage())
            );
        }
    }

    /**
     * Upload image to image hosting service (ImgBB)
     * @param file Image file to upload
     * @return Image URL
     */
    @PostMapping("/upload-image")
    public ResponseEntity<AjaxResult> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.ok(
                    AjaxResult.error(ResponseCode.BAD_REQUEST.getCode(),
                                   "File is empty", null)
                );
            }

            Map<String, Object> uploadResult = imageUploadService.uploadImage(file);
            
            if (Boolean.TRUE.equals(uploadResult.get("success"))) {
                Map<String, Object> result = new HashMap<>();
                result.put("url", uploadResult.get("url"));
                result.put("urlViewer", uploadResult.get("url_viewer"));
                result.put("id", uploadResult.get("id"));
                return ResponseEntity.ok(AjaxResult.success(result));
            } else {
                String error = (String) uploadResult.get("error");
                return ResponseEntity.ok(
                    AjaxResult.error(ResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                                   error != null ? error : "Failed to upload image", null)
                );
            }
        } catch (Exception e) {
            log.error("Error uploading image", e);
            return ResponseEntity.ok(
                AjaxResult.error(ResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                               "Failed to upload image: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Get Replicate API token configuration status (for debugging)
     * This endpoint helps diagnose token configuration issues
     */
    @GetMapping("/replicate/token-status")
    public ResponseEntity<AjaxResult> getTokenStatus() {
        try {
            Map<String, Object> status = replicateService.getTokenStatus();
            return ResponseEntity.ok(AjaxResult.success(status));
        } catch (Exception e) {
            log.error("Error getting token status", e);
            return ResponseEntity.ok(
                AjaxResult.error(ResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                               "Failed to get token status: " + e.getMessage(),
                               null)
            );
        }
    }
}

