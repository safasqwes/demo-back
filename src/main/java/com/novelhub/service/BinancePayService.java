package com.novelhub.service;

import com.novelhub.entity.Order;
import com.novelhub.entity.Payment;
import com.novelhub.entity.PaymentPlan;
import com.novelhub.mapper.OrderMapper;
import com.novelhub.mapper.PaymentMapper;
import com.novelhub.mapper.PaymentPlanMapper;
import com.novelhub.vo.response.CreateBinancePayOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Binance Pay 支付服务
 *
 * 官方文档：https://developers.binance.com/docs/binance-pay/introduction
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BinancePayService {

    private final PaymentPlanMapper paymentPlanMapper;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final RestTemplate restTemplate;

    @Value("${binance.pay.api-key:}")
    private String apiKey;

    @Value("${binance.pay.api-secret:}")
    private String apiSecret;

    @Value("${binance.pay.api-url:https://bpay.binanceapi.com}")
    private String apiUrl;

    /**
     * 创建 Binance Pay 订单
     *
     * @param planId 套餐 ID
     * @param userId 用户 ID
     * @param returnUrl 成功返回 URL
     * @param cancelUrl 取消返回 URL
     * @return 订单响应（包含二维码）
     */
    @Transactional
    public CreateBinancePayOrderResponse createOrder(
            Long planId,
            Long userId,
            String returnUrl,
            String cancelUrl) {

        try {
            // 查询套餐信息
            PaymentPlan plan = paymentPlanMapper.selectById(planId);
            if (plan == null) {
                throw new RuntimeException("Plan not found");
            }

            // 创建订单
            Order order = createOrderRecord(userId, plan);

            // 创建支付记录
            Payment payment = createPaymentRecord(order);

            // 调用 Binance Pay API 创建订单
            Map<String, Object> requestBody = buildBinancePayRequest(order, payment, returnUrl, cancelUrl);

            // 发送请求到 Binance Pay API
            Map<String, Object> response = callBinancePayAPI("/binancepay/openapi/v2/order", requestBody);

            // 解析响应
            if (!"SUCCESS".equals(response.get("status"))) {
                throw new RuntimeException("Binance Pay API error: " + response.get("errorMessage"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            String prepayId = (String) data.get("prepayId");
            String qrcodeLink = (String) data.get("qrcodeLink");
            String qrContent = (String) data.get("qrContent");
            String checkoutUrl = (String) data.get("checkoutUrl");
            String deeplink = (String) data.get("deeplink");
            String universalUrl = (String) data.get("universalUrl");
            Long expireTime = (Long) data.get("expireTime");

            // 更新支付记录，保存 Binance Pay 订单 ID
            payment.setBinancePrepayId(prepayId);
            paymentMapper.updateById(payment);

            log.info("Binance Pay order created: prepayId={}, orderId={}, paymentId={}",
                prepayId, order.getOrderId(), payment.getPaymentId());

            return CreateBinancePayOrderResponse.builder()
                .success(true)
                .prepayId(prepayId)
                .qrcodeLink(qrcodeLink)
                .qrContent(qrContent)
                .checkoutUrl(checkoutUrl)
                .deeplink(deeplink)
                .universalUrl(universalUrl)
                .expireTime(expireTime)
                .build();

        } catch (Exception e) {
            log.error("Failed to create Binance Pay order", e);
            throw new RuntimeException("Failed to create Binance Pay order: " + e.getMessage());
        }
    }

    /**
     * 查询 Binance Pay 订单状态
     *
     * @param prepayId Binance Pay 订单 ID
     * @return 订单状态
     */
    public Map<String, Object> queryOrderStatus(String prepayId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prepayId", prepayId);

            Map<String, Object> response = callBinancePayAPI("/binancepay/openapi/v2/order/query", requestBody);

            if (!"SUCCESS".equals(response.get("status"))) {
                throw new RuntimeException("Binance Pay API error: " + response.get("errorMessage"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            return data;

        } catch (Exception e) {
            log.error("Failed to query Binance Pay order status", e);
            throw new RuntimeException("Failed to query order status: " + e.getMessage());
        }
    }

    /**
     * 构建 Binance Pay 请求参数
     */
    private Map<String, Object> buildBinancePayRequest(Order order, Payment payment, String returnUrl, String cancelUrl) {
        Map<String, Object> request = new HashMap<>();

        // 环境信息
        Map<String, Object> env = new HashMap<>();
        env.put("terminalType", "WEB");
        request.put("env", env);

        // 商户订单信息
        Map<String, Object> merchantTradeNo = new HashMap<>();
        merchantTradeNo.put("merchantTradeNo", payment.getPaymentNumber());
        merchantTradeNo.put("tradeType", "WEB");
        request.put("merchantTradeNo", merchantTradeNo);

        // 订单金额（Binance Pay 使用美元，单位为分）
        request.put("orderAmount", order.getAmount() / 100.0); // 转换为美元
        request.put("currency", "USD");

        // 商品信息
        Map<String, Object> goods = new HashMap<>();
        goods.put("goodsType", "02"); // 虚拟商品
        goods.put("goodsCategory", "D000"); // 数字内容
        goods.put("referenceGoodsId", order.getPlanId().toString());
        goods.put("goodsName", order.getPlanName());
        request.put("goods", goods);

        // 回调 URL
        if (returnUrl != null) {
            request.put("returnUrl", returnUrl);
        }
        if (cancelUrl != null) {
            request.put("cancelUrl", cancelUrl);
        }

        return request;
    }

    /**
     * 调用 Binance Pay API
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callBinancePayAPI(String endpoint, Map<String, Object> requestBody) {
        try {
            // 生成时间戳和随机数
            long timestamp = System.currentTimeMillis();
            String nonce = UUID.randomUUID().toString().replace("-", "");

            // 将请求体转换为 JSON 字符串
            String payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);

            // 生成签名
            String signature = generateSignature(timestamp, nonce, payload);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("BinancePay-Timestamp", String.valueOf(timestamp));
            headers.set("BinancePay-Nonce", nonce);
            headers.set("BinancePay-Certificate-SN", apiKey);
            headers.set("BinancePay-Signature", signature);

            // 发送请求
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                apiUrl + endpoint,
                entity,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            return responseBody;

        } catch (Exception e) {
            log.error("Binance Pay API call failed", e);
            throw new RuntimeException("Binance Pay API call failed: " + e.getMessage());
        }
    }

    /**
     * 生成 Binance Pay 签名
     *
     * Signature = HMAC_SHA512(API_SECRET, timestamp + "\n" + nonce + "\n" + payload)
     */
    private String generateSignature(long timestamp, String nonce, String payload) {
        try {
            String message = timestamp + "\n" + nonce + "\n" + payload;

            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                apiSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA512"
            );
            sha512Hmac.init(secretKeySpec);

            byte[] hash = sha512Hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * 创建订单记录
     */
    private Order createOrderRecord(Long userId, PaymentPlan plan) {
        Order order = Order.builder()
            .userId(userId)
            .planId(plan.getPlanId())
            .planName(plan.getPlanName())
            .orderNumber("ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
            .amount(plan.getPrice())
            .currency("USD")
            .points(plan.getPointsAmount())
            .status(0) // 待支付
            .orderType(plan.getPlanType())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        orderMapper.insert(order);
        return order;
    }

    /**
     * 创建支付记录
     */
    private Payment createPaymentRecord(Order order) {
        Payment payment = Payment.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .paymentNumber("PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
            .paymentMethod("BINANCE_PAY")
            .amount(order.getAmount())
            .currency(order.getCurrency())
            .status(0) // 待支付
            .expiresAt(LocalDateTime.now().plusHours(1)) // 1小时后过期
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        paymentMapper.insert(payment);
        return payment;
    }
}

