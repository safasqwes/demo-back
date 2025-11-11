package com.novelhub.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Image Upload Service
 * Handles uploading images to image hosting services (ImgBB)
 */
@Slf4j
@Service
public class ImageUploadService {

    @Value("${image.upload.provider:imgbb}")
    private String uploadProvider;

    @Value("${image.upload.imgbb.api-key:}")
    private String imgbbApiKeyRaw;
    
    private String imgbbApiKey;

    @Value("${image.upload.imgbb.api-url:https://api.imgbb.com/1/upload}")
    private String imgbbApiUrl;

    @Value("${image.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    private final RestTemplate restTemplate;

    public ImageUploadService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        // Trim and validate API key
        if (imgbbApiKeyRaw != null) {
            imgbbApiKey = imgbbApiKeyRaw.trim();
        } else {
            imgbbApiKey = "";
        }
        
        // Log API key configuration status (without exposing the full key)
        if (imgbbApiKey == null || imgbbApiKey.isEmpty()) {
            log.warn("ImgBB API key is NOT configured - image upload will fail");
            log.warn("Please set IMGBB_API_KEY environment variable or configure in application-dev.yml");
            log.warn("Get your API key from: https://api.imgbb.com/");
        } else {
            // Only log first 10 characters for security
            String keyPreview = imgbbApiKey.length() > 10 
                ? imgbbApiKey.substring(0, 10) + "..." 
                : "***";
            log.info("ImgBB API key configured: {} (length: {})", keyPreview, imgbbApiKey.length());
        }
    }

    /**
     * Upload image file to image hosting service
     * @param file Image file to upload
     * @return Image URL or error message
     */
    public Map<String, Object> uploadImage(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("error", "File is empty");
                return result;
            }

            // Check file size
            if (file.getSize() > maxFileSize) {
                result.put("success", false);
                result.put("error", "File size exceeds maximum limit of " + (maxFileSize / 1024 / 1024) + "MB");
                return result;
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("success", false);
                result.put("error", "File must be an image");
                return result;
            }

            // Route to appropriate upload provider
            if ("imgbb".equalsIgnoreCase(uploadProvider)) {
                return uploadToImgBB(file);
            } else {
                result.put("success", false);
                result.put("error", "Unsupported upload provider: " + uploadProvider);
                return result;
            }

        } catch (Exception e) {
            log.error("Error uploading image", e);
            result.put("success", false);
            result.put("error", "Internal error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Upload image to ImgBB
     */
    private Map<String, Object> uploadToImgBB(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Check API key
        if (imgbbApiKey == null || imgbbApiKey.isEmpty()) {
            result.put("success", false);
            result.put("error", "ImgBB API key is not configured");
            log.warn("ImgBB API key is not configured");
            return result;
        }

        // Convert image to Base64
        byte[] fileBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);

        // Prepare request
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("key", imgbbApiKey);
        requestBody.add("image", base64Image);
        requestBody.add("name", file.getOriginalFilename());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Call ImgBB API
        try {
            log.debug("Uploading image to ImgBB: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
            
            ResponseEntity<String> response = restTemplate.exchange(
                imgbbApiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = JSON.parseObject(response.getBody());
                JSONObject data = responseJson.getJSONObject("data");

                if (data != null) {
                    String imageUrl = data.getString("url");
                    String imageUrlViewer = data.getString("url_viewer");
                    
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        result.put("success", true);
                        result.put("url", imageUrl);
                        result.put("url_viewer", imageUrlViewer);
                        result.put("id", data.getString("id"));
                        log.info("Image uploaded successfully to ImgBB: {}", imageUrl);
                        return result;
                    }
                }

                result.put("success", false);
                result.put("error", "Failed to get image URL from ImgBB response");
                log.error("ImgBB API response missing image URL: {}", response.getBody());
                return result;

            } else {
                result.put("success", false);
                result.put("error", "ImgBB API returned status: " + response.getStatusCode());
                log.error("ImgBB API call failed with status: {}, body: {}", 
                    response.getStatusCode(), response.getBody());
                return result;
            }

        } catch (Exception e) {
            log.error("Error calling ImgBB API", e);
            result.put("success", false);
            result.put("error", "Failed to upload to ImgBB: " + e.getMessage());
            return result;
        }
    }
}

