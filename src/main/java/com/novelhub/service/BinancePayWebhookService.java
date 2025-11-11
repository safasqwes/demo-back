package com.novelhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novelhub.entity.Order;
import com.novelhub.entity.Payment;
import com.novelhub.mapper.OrderMapper;
import com.novelhub.mapper.PaymentMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Binance Pay Webhook 事件处理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BinancePayWebhookService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final PointService pointService;
    private final ObjectMapper objectMapper;

    @Value("${binance.pay.api-secret:}")
    private String apiSecret;

    /**
     * 验证 Webhook 签名
     *
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param payload 请求体
     * @param signature 签名
     * @return 是否有效
     */
    public boolean verifySignature(String timestamp, String nonce, String payload, String signature) {
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

            String calculatedSignature = hexString.toString().toUpperCase();

            return calculatedSignature.equals(signature);

        } catch (Exception e) {
            log.error("Error verifying Binance Pay webhook signature", e);
            return false;
        }
    }

    /**
     * 处理 Webhook 事件
     *
     * @param payload Webhook 请求体（JSON 字符串）
     */
    @Transactional
    public void handleWebhook(String payload) {
        try {
            // 解析 Webhook 数据
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);

            String bizType = (String) webhookData.get("bizType");
            String bizStatus = (String) webhookData.get("bizStatus");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");

            log.info("Processing Binance Pay webhook: bizType={}, bizStatus={}", bizType, bizStatus);

            // 处理支付成功事件
            if ("PAY".equals(bizType) && "PAY_SUCCESS".equals(bizStatus)) {
                handlePaymentSuccess(data);
            }
            // 处理支付关闭事件
            else if ("PAY".equals(bizType) && "PAY_CLOSED".equals(bizStatus)) {
                handlePaymentClosed(data);
            }
            // 处理退款事件
            else if ("REFUND".equals(bizType) && "REFUND_SUCCESS".equals(bizStatus)) {
                handleRefundSuccess(data);
            } else {
                log.info("Unhandled webhook event: bizType={}, bizStatus={}", bizType, bizStatus);
            }

        } catch (Exception e) {
            log.error("Error handling Binance Pay webhook", e);
            throw new RuntimeException("Failed to process webhook", e);
        }
    }

    /**
     * 处理支付成功事件
     */
    private void handlePaymentSuccess(Map<String, Object> data) {
        try {
            String merchantTradeNo = (String) data.get("merchantTradeNo");
            String transactionId = (String) data.get("transactionId");

            log.info("Processing payment success: merchantTradeNo={}, transactionId={}",
                merchantTradeNo, transactionId);

            // 根据支付编号查询支付记录
            LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
            paymentQuery.eq(Payment::getPaymentNumber, merchantTradeNo);
            Payment payment = paymentMapper.selectOne(paymentQuery);

            if (payment == null) {
                throw new RuntimeException("Payment not found: " + merchantTradeNo);
            }

            // 检查支付状态，避免重复处理
            if (payment.getStatus() == 1) {
                log.warn("Payment already processed: paymentId={}", payment.getPaymentId());
                return;
            }

            // 更新支付记录
            payment.setStatus(1); // 已支付
            payment.setPaidAt(LocalDateTime.now());
            payment.setBinanceTransactionId(transactionId);
            paymentMapper.updateById(payment);

            // 更新订单状态
            Order order = orderMapper.selectById(payment.getOrderId());
            if (order == null) {
                throw new RuntimeException("Order not found: " + payment.getOrderId());
            }

            order.setStatus(1); // 已支付
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);

            // 更新用户积分 (pointsType: 2 = FixedPoints, 购买获得的积分)
            pointService.addPoints(order.getUserId(), order.getPoints(), 2, "支付成功", order.getOrderNumber());

            log.info("Payment processed successfully: paymentId={}, orderId={}, userId={}, points={}",
                payment.getPaymentId(), order.getOrderId(), order.getUserId(), order.getPoints());

            // TODO: 发送支付成功通知邮件
            // emailService.sendPaymentSuccessEmail(user.getEmail(), order);

        } catch (Exception e) {
            log.error("Error processing payment success event", e);
            throw new RuntimeException("Failed to process payment success", e);
        }
    }

    /**
     * 处理支付关闭事件
     */
    private void handlePaymentClosed(Map<String, Object> data) {
        try {
            String merchantTradeNo = (String) data.get("merchantTradeNo");

            log.info("Processing payment closed: merchantTradeNo={}", merchantTradeNo);

            // 根据支付编号查询支付记录
            LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
            paymentQuery.eq(Payment::getPaymentNumber, merchantTradeNo);
            Payment payment = paymentMapper.selectOne(paymentQuery);

            if (payment != null && payment.getStatus() == 0) {
                // 更新支付状态为已关闭
                payment.setStatus(3); // 已过期
                paymentMapper.updateById(payment);

                // 更新订单状态
                Order order = orderMapper.selectById(payment.getOrderId());
                if (order != null && order.getStatus() == 0) {
                    order.setStatus(3); // 已过期
                    order.setUpdatedAt(LocalDateTime.now());
                    orderMapper.updateById(order);
                }

                log.info("Payment closed: paymentId={}", payment.getPaymentId());
            }

        } catch (Exception e) {
            log.error("Error processing payment closed event", e);
        }
    }

    /**
     * 处理退款成功事件
     */
    private void handleRefundSuccess(Map<String, Object> data) {
        try {
            String merchantTradeNo = (String) data.get("merchantTradeNo");
            String refundRequestId = (String) data.get("refundRequestId");

            log.info("Processing refund success: merchantTradeNo={}, refundRequestId={}",
                merchantTradeNo, refundRequestId);

            // 根据支付编号查询支付记录
            LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
            paymentQuery.eq(Payment::getPaymentNumber, merchantTradeNo);
            Payment payment = paymentMapper.selectOne(paymentQuery);

            if (payment != null) {
                // 更新支付状态为已退款
                payment.setStatus(5); // 已退款
                paymentMapper.updateById(payment);

                // 更新订单状态
                Order order = orderMapper.selectById(payment.getOrderId());
                if (order != null) {
                    order.setStatus(4); // 已退款
                    order.setUpdatedAt(LocalDateTime.now());
                    orderMapper.updateById(order);

                    // 扣除用户积分
                    pointService.refundPoints(order.getUserId(), order.getPoints(), 2, "退款");
                }

                log.info("Refund processed: paymentId={}", payment.getPaymentId());
            }

        } catch (Exception e) {
            log.error("Error processing refund success event", e);
            throw new RuntimeException("Failed to process refund", e);
        }
    }
}

