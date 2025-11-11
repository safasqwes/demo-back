package com.novelhub.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Fingerprint Validation Utility
 * Validates browser fingerprints sent from frontend
 */
@Slf4j
public class FingerprintUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * Decrypt AES encrypted text
     *
     * @param encryptedText encrypted text (Base64 encoded)
     * @param key AES key
     * @return decrypted text
     */
    public static String aesDecrypt(String encryptedText, String key) {
        try {
            // Decode Base64
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

            // Prepare AES key (ensure it's 16, 24, or 32 bytes)
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] normalizedKey = new byte[16]; // Use 128-bit key
            System.arraycopy(keyBytes, 0, normalizedKey, 0, Math.min(keyBytes.length, 16));

            SecretKeySpec secretKey = new SecretKeySpec(normalizedKey, AES_ALGORITHM);

            // Decrypt
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES decryption failed", e);
            return null;
        }
    }

    /**
     * Simple XOR decryption (matching frontend's aseEncrypt function)
     * This matches the frontend's aseEncrypt implementation exactly
     *
     * @param encrypted encrypted text (Base64 encoded)
     * @param key encryption key
     * @return decrypted text
     */
    public static String xorDecrypt(String encrypted, String key) {
        try {
            // Decode Base64 first (matching frontend's btoa)
            String text = new String(Base64.getDecoder().decode(encrypted), StandardCharsets.UTF_8);
            StringBuilder decrypted = new StringBuilder();
            
            // XOR decryption (matching frontend's aseEncrypt logic)
            for (int i = 0; i < text.length(); i++) {
                char charCode = (char) (text.charAt(i) ^ key.charAt(i % key.length()));
                decrypted.append(charCode);
            }
            
//            log.debug("XOR decryption: encrypted={}, key={}, decrypted={}",
//                     encrypted.substring(0, Math.min(10, encrypted.length())) + "...",
//                     key.substring(0, Math.min(10, key.length())) + "...",
//                     decrypted.toString());
            
            return decrypted.toString();
        } catch (Exception e) {
            log.error("XOR decryption failed", e);
            return null;
        }
    }

    /**
     * Validate fingerprint
     *
     * @param fp original fingerprint
     * @param fp1 encrypted fingerprint
     * @param aesSecret AES secret key
     * @return true if valid
     */
    public static boolean validateFingerprint(String fp, String fp1, String aesSecret) {
        if (!StringUtils.hasText(fp) || !StringUtils.hasText(fp1) || !StringUtils.hasText(aesSecret)) {
            log.warn("Fingerprint validation failed: missing parameters");
            return false;
        }

        try {
//            log.debug("Validating fingerprint: fp={}, fp1={}, aesSecret={}",
//                     fp, fp1.substring(0, Math.min(10, fp1.length())) + "...",
//                     aesSecret.substring(0, Math.min(10, aesSecret.length())) + "...");
            
            // Decrypt fp1 using XOR (matching frontend implementation)
            String decryptedFp = xorDecrypt(fp1, aesSecret);

            if (decryptedFp == null) {
                log.warn("Failed to decrypt fp1");
                return false;
            }

            // Compare decrypted fp1 with original fp
            boolean isValid = fp.equals(decryptedFp);
            if (!isValid) {
                log.warn("Fingerprint mismatch: fp={}, decrypted fp1={}", fp, decryptedFp);
                log.warn("fp length: {}, decryptedFp length: {}", fp.length(), decryptedFp.length());
            } else {
//                log.debug("Fingerprint validation successful");
            }

            return isValid;
        } catch (Exception e) {
            log.error("Fingerprint validation error", e);
            return false;
        }
    }

    /**
     * Extract AES secret from x-guide header
     * The frontend now sends aesSecret directly as x-guide header
     *
     * @param xGuide x-guide header value (which is now the aesSecret)
     * @return extracted secret
     */
    public static String extractAesSecret(String xGuide) {
        if (!StringUtils.hasText(xGuide)) {
            log.warn("x-guide header is empty");
            return null;
        }
        
//        log.debug("Extracting AES secret from x-guide: {}", xGuide.substring(0, Math.min(10, xGuide.length())) + "...");
        
        // The frontend now sends aesSecret directly as x-guide
        return xGuide;
    }

    /**
     * Generate MD5 hash
     *
     * @param input input string
     * @return MD5 hash (hex string)
     */
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("MD5 hash generation failed", e);
            return null;
        }
    }

    /**
     * Validate timestamp to prevent replay attacks
     *
     * @param timestamp timestamp from header (X-code)
     * @param maxAgeSeconds maximum age in seconds
     * @return true if timestamp is valid
     */
    public static boolean validateTimestamp(String timestamp, long maxAgeSeconds) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long diff = Math.abs(currentTime - requestTime);

            boolean isValid = diff <= maxAgeSeconds * 1000;
            if (!isValid) {
                log.warn("Timestamp validation failed: diff={}ms, max={}ms", diff, maxAgeSeconds * 1000);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Timestamp validation error", e);
            return false;
        }
    }

    /**
     * Extract fingerprint from HTTP request headers
     *
     * @param request HTTP servlet request
     * @return fingerprint value or null if not found
     */
    public static String extractFingerprint(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader("fp");
    }

    /**
     * Extract and validate fingerprint from HTTP request
     * This method can be used WITHOUT @FingerprintRequired annotation
     * Validates fingerprint using fp, fp1, and x-guide headers
     * 
     * @param request HTTP servlet request
     * @return fingerprint value if valid, null otherwise
     */
    public static String extractAndValidateFingerprint(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            String fp = request.getHeader("fp");
            String fp1 = request.getHeader("fp1");
            String xGuide = request.getHeader("x-guide");

            // Check if all required headers are present
            if (!StringUtils.hasText(fp) || !StringUtils.hasText(fp1) || !StringUtils.hasText(xGuide)) {
                log.debug("Missing fingerprint headers");
                return null;
            }

            // Extract AES secret
            String aesSecret = extractAesSecret(xGuide);
            if (aesSecret == null) {
                log.debug("Failed to extract AES secret");
                return null;
            }

            // Validate fingerprint
            if (!validateFingerprint(fp, fp1, aesSecret)) {
                log.debug("Fingerprint validation failed");
                return null;
            }

            return fp;

        } catch (Exception e) {
            log.debug("Error validating fingerprint: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if request has valid fingerprint
     * 
     * @param request HTTP servlet request
     * @return true if fingerprint is valid
     */
    public static boolean hasValidFingerprint(HttpServletRequest request) {
        return extractAndValidateFingerprint(request) != null;
    }

    /**
     * Require valid fingerprint and return fingerprint value
     * Throws RuntimeException if fingerprint is missing or invalid
     * 
     * @param request HTTP servlet request
     * @return fingerprint value (never null)
     * @throws RuntimeException if validation fails
     */
    public static String requireFingerprint(HttpServletRequest request) {
        String fingerprint = extractAndValidateFingerprint(request);
        if (fingerprint == null) {
            throw new RuntimeException("Valid fingerprint required: missing or invalid fingerprint headers");
        }
        return fingerprint;
    }

    /**
     * Require valid fingerprint with timestamp validation
     * Throws RuntimeException if fingerprint or timestamp is invalid
     * 
     * @param request HTTP servlet request
     * @param maxAgeSeconds Maximum age for timestamp in seconds
     * @return fingerprint value (never null)
     * @throws RuntimeException if validation fails
     */
    public static String requireFingerprintWithTimestamp(HttpServletRequest request, long maxAgeSeconds) {
        String fingerprint = requireFingerprint(request);
        
        String xCode = request.getHeader("X-code");
        if (StringUtils.hasText(xCode)) {
            if (!validateTimestamp(xCode, maxAgeSeconds)) {
                throw new RuntimeException("Request expired, please refresh and try again");
            }
        }
        
        return fingerprint;
    }
}

