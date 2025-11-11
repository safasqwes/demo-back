package com.novelhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.HttpTransport;
import java.net.Proxy;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import com.novelhub.entity.SocialAccount;
import com.novelhub.entity.User;
import com.novelhub.mapper.SocialAccountMapper;
import com.novelhub.mapper.UserMapper;
import com.novelhub.entity.UserPoint;
// PointService import removed as not used in this class
import com.novelhub.utils.JwtUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GoogleAuthService {

    @Autowired
    private SocialAccountMapper socialAccountMapper;
    
    /**
     * 确保用户有有效的username，如果为空则自动生成
     *
     * @param user 用户对象
     */
    private void ensureUsernameExists(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            String generatedUsername = "user_" + UUID.randomUUID().toString();
            user.setUsername(generatedUsername);
            userMapper.updateById(user);
            log.info("为用户 {} 自动生成用户名: {}", user.getUserId(), generatedUsername);
        }
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PointService pointService;

    @Autowired
    private DailyClaimService dailyClaimService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${google.client-id}")
    private String googleClientId;
    
    @Value("${google.proxy.host:}")
    private String proxyHost;
    
    @Value("${google.proxy.port:0}")
    private int proxyPort;
    
    @Value("${google.proxy.type:HTTP}")
    private String proxyType;

    /**
     * 验证Google ID Token
     */
    public GoogleIdToken.Payload verifyGoogleToken(String idTokenString) throws GeneralSecurityException, IOException {
        // 创建带代理的HttpTransport
        HttpTransport httpTransport = createHttpTransportWithProxy();
        
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            return idToken.getPayload();
        }
        return null;
    }
    
    /**
     * 创建带代理的HttpTransport
     */
    private HttpTransport createHttpTransportWithProxy() {
        if (StringUtils.hasText(proxyHost) && proxyPort > 0) {
            try {
                // 创建代理
                Proxy.Type type = Proxy.Type.HTTP;
                if ("SOCKS".equalsIgnoreCase(proxyType)) {
                    type = Proxy.Type.SOCKS;
                }
                
                Proxy proxy = new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
                log.info("Using proxy: {}:{} (type: {})", proxyHost, proxyPort, proxyType);
                
                // 创建带代理的NetHttpTransport
                return new NetHttpTransport.Builder()
                        .setProxy(proxy)
                        .build();
            } catch (Exception e) {
                log.warn("Failed to create proxy transport, using default: {}", e.getMessage());
            }
        }
        
        // 如果没有配置代理或创建失败，使用默认的HttpTransport
        return new NetHttpTransport();
    }

    /**
     * 使用Google的tokeninfo API验证credential（更安全的方法）
     * 通过Google的官方API验证credential
     */
    public GoogleIdToken.Payload verifyGoogleCredential(String credential) {
        try {
            // 使用Google的tokeninfo API验证credential
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + credential;
            
            // 使用RestTemplate调用Google API
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenInfo = restTemplate.getForObject(url, Map.class);
            
            if (tokenInfo == null) {
                log.error("Failed to get response from Google tokeninfo API");
                return null;
            }
            
            // 验证audience（client-id）
            String aud = (String) tokenInfo.get("aud");
            if (!googleClientId.equals(aud)) {
                log.warn("Invalid audience in Google credential: expected={}, actual={}", googleClientId, aud);
                return null;
            }
            
            // 验证email_verified
            Object emailVerifiedObj = tokenInfo.get("email_verified");
            boolean emailVerified = false;
            if (emailVerifiedObj instanceof Boolean) {
                emailVerified = (Boolean) emailVerifiedObj;
            } else if (emailVerifiedObj instanceof String) {
                emailVerified = "true".equalsIgnoreCase((String) emailVerifiedObj);
            }
            
            if (!emailVerified) {
                log.warn("Email not verified in Google credential");
                return null;
            }
            
            // 创建Payload对象
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject((String) tokenInfo.get("sub"));
            payload.setEmail((String) tokenInfo.get("email"));
            payload.setEmailVerified(emailVerified);
            payload.set("name", tokenInfo.get("name"));
            payload.set("picture", tokenInfo.get("picture"));
            payload.setAudience(Collections.singletonList(aud));
            payload.setIssuer((String) tokenInfo.get("iss"));
            
            // 处理时间戳
            Object iat = tokenInfo.get("iat");
            Object exp = tokenInfo.get("exp");
            try {
                if (iat instanceof String) {
                    payload.setIssuedAtTimeSeconds(Long.parseLong((String) iat));
                } else if (iat instanceof Number) {
                    payload.setIssuedAtTimeSeconds(((Number) iat).longValue());
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid iat timestamp: {}", iat);
            }
            
            try {
                if (exp instanceof String) {
                    payload.setExpirationTimeSeconds(Long.parseLong((String) exp));
                } else if (exp instanceof Number) {
                    payload.setExpirationTimeSeconds(((Number) exp).longValue());
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid exp timestamp: {}", exp);
            }
            
            log.info("Google credential verified successfully for user: {}", payload.getEmail());
            return payload;
            
        } catch (Exception e) {
            log.error("Error verifying Google credential", e);
            return null;
        }
    }

    /**
     * 从Google用户信息创建或获取用户
     */
    @Transactional
    public Map<String, Object> getOrCreateUserFromGoogle(GoogleIdToken.Payload payload) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String googleId = payload.getSubject();

            // 检查是否已存在社交账号
            LambdaQueryWrapper<SocialAccount> socialQuery = new LambdaQueryWrapper<>();
            socialQuery.eq(SocialAccount::getProviderId, googleId)
                      .eq(SocialAccount::getProvider, 1); // 1 = Google
            SocialAccount existingSocialAccount = socialAccountMapper.selectOne(socialQuery);

            User user;
            SocialAccount socialAccount;

            if (existingSocialAccount != null) {
                // 社交账号已存在，获取关联的用户
                user = userMapper.selectById(existingSocialAccount.getUserId());
                socialAccount = existingSocialAccount;
                
                // 更新社交账号信息
                socialAccount.setEmail(email);
                socialAccount.setExtraData(createExtraData(name, picture));
                socialAccount.setUpdatedAt(LocalDateTime.now());
                socialAccountMapper.updateById(socialAccount);
                
                log.info("Existing Google user logged in: {}", email);
            } else {
                // 检查邮箱是否已注册
                LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
                userQuery.eq(User::getEmail, email);
                User existingUser = userMapper.selectOne(userQuery);

                if (existingUser != null) {
                    // 邮箱已注册，关联社交账号
                    user = existingUser;
                    socialAccount = SocialAccount.builder()
                            .userId(user.getUserId())
                            .email(email)
                            .provider(1) // 1 = Google
                            .providerId(googleId)
                            .extraData(createExtraData(name, picture))
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    socialAccountMapper.insert(socialAccount);
                    
                    log.info("Linked Google account to existing user: {}", email);
                } else {
                    // 创建新用户
                    user = User.builder()
                            .username("user_" + UUID.randomUUID().toString())
                            .email(email)
                            .nickname(name)
                            .avatar(picture)
                            .status(1)
                            .lastLoginTime(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .deleted(0)
                            .build();
                    userMapper.insert(user);
                    
                    // 确保username不为空
                    ensureUsernameExists(user);

                    // 创建社交账号
                    socialAccount = SocialAccount.builder()
                            .userId(user.getUserId())
                            .email(email)
                            .provider(1) // 1 = Google
                            .providerId(googleId)
                            .extraData(createExtraData(name, picture))
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    socialAccountMapper.insert(socialAccount);
                    
                    log.info("Created new user with Google account: {}", email);
                }
            }

            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);

            // 生成JWT token
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("email", user.getEmail());
            String token = jwtUtil.generateToken(user.getUsername(), extraClaims);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            // 获取用户金币银币数量
            UserPoint userPoint = pointService.getUserPoints(user.getUserId());
            if (userPoint == null) {
                userPoint = pointService.initUserPoints(user.getUserId());
            }

            // 获取用户签到信息
            Map<String, Object> claimInfo = dailyClaimService.getClaimInfo(user.getUserId());

            result.put("success", true);
            result.put("token", token);
            result.put("refreshToken", refreshToken);
            result.put("user", Map.of(
                    "id", user.getUserId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "nickname", user.getNickname() != null ? user.getNickname() : "",
                    "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                    "status", user.getStatus(),
                    "goldCoins", userPoint.getFixedPoints() != null ? userPoint.getFixedPoints() : 0,
                    "silverCoins", userPoint.getFreePoints() != null ? userPoint.getFreePoints() : 0,
                    "totalPoints", userPoint.getPoints() != null ? userPoint.getPoints() : 0
            ));
            result.put("claimInfo", claimInfo);

        } catch (Exception e) {
            log.error("Error processing Google authentication", e);
            result.put("success", false);
            result.put("message", "Authentication failed");
        }

        return result;
    }

    /**
     * 从Google用户信息创建或获取用户（前端已验证）
     * 用于前端已经验证过Google token的情况
     */
    @Transactional
    public Map<String, Object> getOrCreateUserFromGoogleInfo(String email, String name, 
                                                           String picture, String googleId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查是否已存在社交账号
            LambdaQueryWrapper<SocialAccount> socialQuery = new LambdaQueryWrapper<>();
            socialQuery.eq(SocialAccount::getProviderId, googleId)
                      .eq(SocialAccount::getProvider, 1); // 1 = Google
            SocialAccount existingSocialAccount = socialAccountMapper.selectOne(socialQuery);

            User user;
            SocialAccount socialAccount;

            if (existingSocialAccount != null) {
                // 社交账号已存在，获取关联的用户
                user = userMapper.selectById(existingSocialAccount.getUserId());
                socialAccount = existingSocialAccount;
                
                // 更新社交账号信息
                socialAccount.setEmail(email);
                socialAccount.setExtraData(createExtraData(name, picture));
                socialAccount.setUpdatedAt(LocalDateTime.now());
                socialAccountMapper.updateById(socialAccount);
                
                log.info("Existing Google user logged in (frontend-verified): {}", email);
            } else {
                // 检查邮箱是否已注册
                LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
                userQuery.eq(User::getEmail, email);
                User existingUser = userMapper.selectOne(userQuery);

                if (existingUser != null) {
                    // 邮箱已注册，关联社交账号
                    user = existingUser;
                    socialAccount = SocialAccount.builder()
                            .userId(user.getUserId())
                            .email(email)
                            .provider(1) // 1 = Google
                            .providerId(googleId)
                            .extraData(createExtraData(name, picture))
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    socialAccountMapper.insert(socialAccount);
                    
                    log.info("Linked Google account to existing user (frontend-verified): {}", email);
                } else {
                    // 创建新用户
                    user = User.builder()
                            .username("user_" + UUID.randomUUID().toString())
                            .email(email)
                            .nickname(name)
                            .avatar(picture)
                            .status(1)
                            .lastLoginTime(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .deleted(0)
                            .build();
                    userMapper.insert(user);
                    
                    // 确保username不为空
                    ensureUsernameExists(user);

                    // 创建社交账号
                    socialAccount = SocialAccount.builder()
                            .userId(user.getUserId())
                            .email(email)
                            .provider(1) // 1 = Google
                            .providerId(googleId)
                            .extraData(createExtraData(name, picture))
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    socialAccountMapper.insert(socialAccount);
                    
                    log.info("Created new user with Google account (frontend-verified): {}", email);
                }
            }

            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);

            // 生成JWT token
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("email", user.getEmail());
            String token = jwtUtil.generateToken(user.getUsername(), extraClaims);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            // 获取用户金币银币数量
            UserPoint userPoint = pointService.getUserPoints(user.getUserId());
            if (userPoint == null) {
                userPoint = pointService.initUserPoints(user.getUserId());
            }

            // 获取用户签到信息
            Map<String, Object> claimInfo = dailyClaimService.getClaimInfo(user.getUserId());

            result.put("success", true);
            result.put("token", token);
            result.put("refreshToken", refreshToken);
            result.put("user", Map.of(
                    "id", user.getUserId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "nickname", user.getNickname() != null ? user.getNickname() : "",
                    "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                    "status", user.getStatus(),
                    "goldCoins", userPoint.getFixedPoints() != null ? userPoint.getFixedPoints() : 0,
                    "silverCoins", userPoint.getFreePoints() != null ? userPoint.getFreePoints() : 0,
                    "totalPoints", userPoint.getPoints() != null ? userPoint.getPoints() : 0
            ));
            result.put("claimInfo", claimInfo);

        } catch (Exception e) {
            log.error("Error processing Google authentication (frontend-verified)", e);
            result.put("success", false);
            result.put("message", "Authentication failed");
        }

        return result;
    }

    /**
     * 创建额外数据JSON字符串
     */
    private String createExtraData(String name, String picture) {
        // 使用Fastjson2进行JSON序列化
        JSONObject extraData = new JSONObject();
        extraData.put("name", name != null ? name : "");
        extraData.put("picture", picture != null ? picture : "");
        extraData.put("provider", "google");
        return extraData.toJSONString();
    }
}
