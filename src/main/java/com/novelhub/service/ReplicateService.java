package com.novelhub.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Replicate Service
 * Handles calls to Replicate API for Google Nano Banana model
 */
@Slf4j
@Service
public class ReplicateService {

    @Value("${replicate.api.token:}")
    private String replicateApiTokenRaw;

    private String replicateApiToken;

    @Value("${replicate.api.base-url:https://api.replicate.com/v1}")
    private String replicateBaseUrl;

    @Value("${replicate.api.model:google/nano-banana}")
    private String modelName;

    @Value("${replicate.api.timeout:300000}")
    private int timeoutMs;

    @Value("${replicate.api.poll-interval:2000}")
    private int pollIntervalMs;

    private final RestTemplate restTemplate;

    public ReplicateService() {
        this.restTemplate = new RestTemplate();
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        // Trim and validate token
        if (replicateApiTokenRaw != null) {
            replicateApiToken = replicateApiTokenRaw.trim();
        } else {
            replicateApiToken = "";
        }
        
        // Log token configuration status (without exposing the full token)
        if (replicateApiToken == null || replicateApiToken.isEmpty()) {
            log.warn("Replicate API token is NOT configured - API calls will fail");
            log.warn("Please set REPLICATE_API_TOKEN environment variable or configure in application-dev.yml");
        } else {
            // Validate token format (should start with r8_)
            if (!replicateApiToken.startsWith("r8_")) {
                log.warn("Replicate API token format may be incorrect (should start with 'r8_')");
            }
            // Only log first 10 characters for security
            String tokenPreview = replicateApiToken.length() > 10 
                ? replicateApiToken.substring(0, 10) + "..." 
                : "***";
            log.info("Replicate API token configured: {} (length: {})", tokenPreview, replicateApiToken.length());
            
            // Additional validation
            if (replicateApiToken.length() < 20) {
                log.warn("Replicate API token seems too short (expected ~40+ characters). Current length: {}", replicateApiToken.length());
            }
        }
    }

    /**
     * Get token configuration status (for debugging)
     * @return Token configuration status
     */
    public Map<String, Object> getTokenStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean isConfigured = replicateApiToken != null && !replicateApiToken.isEmpty();
        status.put("configured", isConfigured);
        status.put("length", isConfigured ? replicateApiToken.length() : 0);
        
        if (isConfigured) {
            String preview = replicateApiToken.length() > 10 
                ? replicateApiToken.substring(0, 10) + "..." 
                : "***";
            status.put("preview", preview);
            status.put("formatValid", replicateApiToken.startsWith("r8_"));
        }
        
        return status;
    }

    /**
     * Generate image using Nano Banana model
     * @param prompt Text prompt describing the image to generate
     * @param imageUrls Optional list of input image URLs for editing/fusion (max 3 images)
     * @param aspectRatio Optional aspect ratio (e.g., "16:9", "1:1", "9:16")
     * @return Generated image URL or error message
     */
    public Map<String, Object> generateImage(String prompt, List<String> imageUrls, String aspectRatio) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (replicateApiToken == null || replicateApiToken.isEmpty()) {
                result.put("success", false);
                result.put("error", "Replicate API token is not configured");
                return result;
            }

            // Build input parameters
            Map<String, Object> input = new HashMap<>();
            input.put("prompt", prompt);
            
            // Support multiple images (up to 3) for fusion
            // Replicate API uses "image_input" as array for both single and multiple images
            if (imageUrls != null && !imageUrls.isEmpty()) {
                // Filter out null and empty URLs
                List<String> validUrls = imageUrls.stream()
                    .filter(url -> url != null && !url.isEmpty())
                    .limit(3) // Nano Banana supports up to 3 images
                    .collect(Collectors.toList());
                
                if (!validUrls.isEmpty()) {
                    // Use "image_input" parameter (array format) for all cases
                    input.put("image_input", validUrls);
                }
            }
            
            if (aspectRatio != null && !aspectRatio.isEmpty()) {
                input.put("aspect_ratio", aspectRatio);
            }

            // Create prediction request
            // Replicate API supports /models/{model}/predictions endpoint
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", input);
            requestBody.put("stream", false);

            // Create HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Replicate API uses "Bearer" prefix for Authorization
            String authHeader = "Bearer " + replicateApiToken;
            headers.set("Authorization", authHeader);
            // Add "Prefer: wait" header as shown in curl example (optional, but may help)
            // headers.set("Prefer", "wait");
            
            // Log request details (without exposing token)
            log.debug("Creating prediction - Model: {}, Prompt length: {}, Has images: {}", 
                modelName, prompt.length(), imageUrls != null && !imageUrls.isEmpty());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Step 1: Create prediction
            // Replicate API supports two formats:
            // 1. POST /predictions (requires version in body)
            // 2. POST /models/{model}/predictions (uses latest version)
            // Using format 2 with model name in URL
            String createUrl = replicateBaseUrl + "/models/" + modelName + "/predictions";
            
            // Print detailed request information for debugging
            log.info("=== Replicate API Request Details ===");
            log.info("URL: {}", createUrl);
            log.info("Method: POST");
            log.info("Headers:");
            headers.forEach((key, values) -> {
                if ("Authorization".equalsIgnoreCase(key)) {
                    // Mask token for security
                    if (!values.isEmpty() && values.get(0) != null) {
                        String authValue = values.get(0);
                        if (authValue.length() > 20) {
                            log.info("  {}: {}...{} (length: {})", key, authValue.substring(0, 15), 
                                authValue.substring(authValue.length() - 5), authValue.length());
                        } else {
                            log.info("  {}: {}", key, "***MASKED***");
                        }
                    }
                } else {
                    log.info("  {}: {}", key, values);
                }
            });
            log.info("Request Body:");
            try {
                String requestBodyJson = JSON.toJSONString(requestBody);
                log.info("  {}", requestBodyJson);
            } catch (Exception e) {
                log.warn("Failed to serialize request body: {}", e.getMessage());
                log.info("  Request body map: {}", requestBody);
            }
            log.info("Token status: configured={}, length={}, prefix={}", 
                replicateApiToken != null && !replicateApiToken.isEmpty(),
                replicateApiToken != null ? replicateApiToken.length() : 0,
                replicateApiToken != null && replicateApiToken.length() > 3 ? replicateApiToken.substring(0, 3) : "N/A");
            log.info("=====================================");
            
            log.info("Creating prediction with prompt: {} using model: {}", prompt, modelName);
            
            ResponseEntity<String> createResponse;
            try {
                createResponse = restTemplate.exchange(
                    createUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
                );
                
                // Log response details
                log.info("=== Replicate API Response ===");
                log.info("Status Code: {}", createResponse.getStatusCode());
                log.info("Response Headers: {}", createResponse.getHeaders());
                log.info("Response Body: {}", createResponse.getBody());
                log.info("==============================");
                
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // Capture detailed error information for 401 and other client errors
                log.error("=== HTTP Client Error ===");
                log.error("Status Code: {}", e.getStatusCode());
                log.error("Status Text: {}", e.getStatusText());
                log.error("Response Headers: {}", e.getResponseHeaders());
                log.error("Response Body: {}", e.getResponseBodyAsString());
                log.error("Request URL: {}", createUrl);
                log.error("Request Method: POST");
                log.error("Authorization header sent: Bearer {}... (length: {})", 
                    replicateApiToken != null && replicateApiToken.length() > 10 
                        ? replicateApiToken.substring(0, 10) : "***",
                    authHeader != null ? authHeader.length() : 0);
                log.error("Full Authorization header preview: {}", 
                    authHeader != null && authHeader.length() > 20 
                        ? authHeader.substring(0, 15) + "..." + authHeader.substring(authHeader.length() - 5) 
                        : authHeader);
                log.error("Request Body JSON: {}", JSON.toJSONString(requestBody));
                log.error("=========================");
                
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    result.put("success", false);
                    result.put("error", "401 Unauthorized - Invalid or missing API token. Please check your REPLICATE_API_TOKEN configuration.");
                    log.error("Replicate API returned 401 Unauthorized. Token configured: {}, length: {}, prefix: {}", 
                        replicateApiToken != null && !replicateApiToken.isEmpty(),
                        replicateApiToken != null ? replicateApiToken.length() : 0,
                        replicateApiToken != null && replicateApiToken.length() > 3 ? replicateApiToken.substring(0, 3) : "N/A");
                    return result;
                }
                
                // Re-throw for other client errors
                throw e;
            } catch (Exception e) {
                log.error("Unexpected error during API call: {}", e.getMessage(), e);
                result.put("success", false);
                result.put("error", "Internal error: " + e.getMessage());
                return result;
            }

            if (createResponse.getStatusCode() != HttpStatus.CREATED && 
                createResponse.getStatusCode() != HttpStatus.OK) {
                result.put("success", false);
                result.put("error", "Failed to create prediction: " + createResponse.getStatusCode() + 
                    (createResponse.getBody() != null ? " - " + createResponse.getBody() : ""));
                log.error("Replicate API call failed with status: {}, body: {}", 
                    createResponse.getStatusCode(), createResponse.getBody());
                return result;
            }

            JSONObject createResult = JSON.parseObject(createResponse.getBody());
            String predictionId = createResult.getString("id");
            
            if (predictionId == null || predictionId.isEmpty()) {
                result.put("success", false);
                result.put("error", "Failed to get prediction ID from response");
                return result;
            }

            log.info("Prediction created with ID: {}", predictionId);

            // Step 2: Poll for result
            String status = createResult.getString("status");
            JSONObject predictionResult = createResult;
            
            long startTime = System.currentTimeMillis();
            while (!status.equals("succeeded") && !status.equals("failed") && !status.equals("canceled")) {
                // Check timeout
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    result.put("success", false);
                    result.put("error", "Prediction timed out after " + (timeoutMs / 1000) + " seconds");
                    result.put("predictionId", predictionId);
                    return result;
                }

                // Wait before next poll
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result.put("success", false);
                    result.put("error", "Polling interrupted");
                    return result;
                }

                // Get prediction status
                String statusUrl = replicateBaseUrl + "/predictions/" + predictionId;
                HttpEntity<String> statusEntity = new HttpEntity<>(headers);
                
                ResponseEntity<String> statusResponse = restTemplate.exchange(
                    statusUrl,
                    HttpMethod.GET,
                    statusEntity,
                    String.class
                );

                if (statusResponse.getStatusCode() != HttpStatus.OK) {
                    result.put("success", false);
                    result.put("error", "Failed to get prediction status: " + statusResponse.getStatusCode());
                    return result;
                }

                predictionResult = JSON.parseObject(statusResponse.getBody());
                status = predictionResult.getString("status");
                
                log.debug("Prediction status: {}", status);
            }

            // Step 3: Process result
            if (status.equals("succeeded")) {
                Object output = predictionResult.get("output");
                
                if (output == null) {
                    result.put("success", false);
                    result.put("error", "No output in prediction result");
                    return result;
                }

                // Handle different output types
                String generatedImageUrl = null;
                if (output instanceof String) {
                    // Single image URL
                    generatedImageUrl = (String) output;
                } else if (output instanceof List) {
                    // Multiple images (list of URLs)
                    List<?> outputList = (List<?>) output;
                    if (!outputList.isEmpty() && outputList.get(0) instanceof String) {
                        generatedImageUrl = (String) outputList.get(0);
                    }
                }

                if (generatedImageUrl == null || generatedImageUrl.isEmpty()) {
                    result.put("success", false);
                    result.put("error", "Invalid output format: " + output);
                    return result;
                }

                result.put("success", true);
                result.put("imageUrl", generatedImageUrl);
                result.put("predictionId", predictionId);
                result.put("status", status);
                log.info("Image generated successfully: {}", generatedImageUrl);
                
                return result;
            } else {
                // Failed or canceled
                String error = predictionResult.getString("error");
                result.put("success", false);
                result.put("error", error != null ? error : "Prediction " + status);
                result.put("status", status);
                result.put("predictionId", predictionId);
                return result;
            }

        } catch (Exception e) {
            log.error("Error generating image with Replicate API", e);
            result.put("success", false);
            result.put("error", "Internal error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Get prediction status
     * @param predictionId Prediction ID
     * @return Prediction status and result
     */
    public Map<String, Object> getPredictionStatus(String predictionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (replicateApiToken == null || replicateApiToken.isEmpty()) {
                result.put("success", false);
                result.put("error", "Replicate API token is not configured");
                return result;
            }

            HttpHeaders headers = new HttpHeaders();
            // Replicate API uses "Bearer" prefix for Authorization
            headers.set("Authorization", "Bearer " + replicateApiToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String statusUrl = replicateBaseUrl + "/predictions/" + predictionId;
            ResponseEntity<String> response = restTemplate.exchange(
                statusUrl,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                result.put("success", false);
                result.put("error", "Failed to get prediction status: " + response.getStatusCode());
                return result;
            }

            JSONObject predictionResult = JSON.parseObject(response.getBody());
            String status = predictionResult.getString("status");
            
            result.put("success", true);
            result.put("status", status);
            result.put("predictionId", predictionId);
            
            if (status.equals("succeeded")) {
                Object output = predictionResult.get("output");
                if (output != null) {
                    String imageUrl = null;
                    if (output instanceof String) {
                        imageUrl = (String) output;
                    } else if (output instanceof List) {
                        List<?> outputList = (List<?>) output;
                        if (!outputList.isEmpty() && outputList.get(0) instanceof String) {
                            imageUrl = (String) outputList.get(0);
                        }
                    }
                    result.put("imageUrl", imageUrl);
                }
            } else if (status.equals("failed") || status.equals("canceled")) {
                String error = predictionResult.getString("error");
                result.put("error", error);
            }
            
            return result;

        } catch (Exception e) {
            log.error("Error getting prediction status", e);
            result.put("success", false);
            result.put("error", "Internal error: " + e.getMessage());
            return result;
        }
    }
}

