package com.novelhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novelhub.entity.Order;
import com.novelhub.entity.Payment;
import com.novelhub.entity.User;
import com.novelhub.mapper.OrderMapper;
import com.novelhub.mapper.PaymentMapper;
import com.novelhub.mapper.UserMapper;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Stripe Webhook 事件处理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final UserMapper userMapper;
    private final PointService pointService;

    /**
     * 处理 checkout.session.completed 事件
     * 当客户成功完成 Checkout 时触发
     */
    @Transactional
    public void handleCheckoutSessionCompleted(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));

            log.info("Processing checkout.session.completed: sessionId={}", session.getId());

            // 从 metadata 中获取订单和支付信息
            Map<String, String> metadata = session.getMetadata();
            Long orderId = Long.parseLong(metadata.get("orderId"));
            Long paymentId = Long.parseLong(metadata.get("paymentId"));
            Long userId = Long.parseLong(metadata.get("userId"));

            // 查询订单
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                throw new RuntimeException("Order not found: " + orderId);
            }

            // 查询支付记录
            Payment payment = paymentMapper.selectById(paymentId);
            if (payment == null) {
                throw new RuntimeException("Payment not found: " + paymentId);
            }

            // 检查订单状态，避免重复处理
            if (order.getStatus() == 1) {
                log.warn("Order already paid: orderId={}", orderId);
                return;
            }

            // 更新支付记录
            payment.setStatus(1); // 已支付
            payment.setPaidAt(LocalDateTime.now());
            payment.setStripePaymentIntentId(session.getPaymentIntent());
            payment.setStripeCustomerId(session.getCustomer());
            paymentMapper.updateById(payment);

            // 更新订单状态
            order.setStatus(1); // 已支付
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);

            // 更新用户积分
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("User not found: " + userId);
            }

            // 使用PointService增加积分 (pointsType: 2 = FixedPoints, 购买获得的积分)
            pointService.addPoints(userId, order.getPoints(), 2, "支付成功", order.getOrderNumber());

            log.info("Payment completed successfully: orderId={}, paymentId={}, userId={}, points={}",
                orderId, paymentId, userId, order.getPoints());

            // TODO: 发送支付成功通知邮件
            // emailService.sendPaymentSuccessEmail(user.getEmail(), order);

        } catch (Exception e) {
            log.error("Error handling checkout.session.completed event", e);
            throw new RuntimeException("Failed to process checkout.session.completed", e);
        }
    }

    /**
     * 处理 checkout.session.expired 事件
     * 当 Checkout Session 过期时触发
     */
    @Transactional
    public void handleCheckoutSessionExpired(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));

            log.info("Processing checkout.session.expired: sessionId={}", session.getId());

            // 查询支付记录
            LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
            paymentQuery.eq(Payment::getStripeSessionId, session.getId());
            Payment payment = paymentMapper.selectOne(paymentQuery);

            if (payment != null && payment.getStatus() == 0) {
                // 更新支付状态为已过期
                payment.setStatus(3); // 已过期
                paymentMapper.updateById(payment);

                // 更新订单状态
                Order order = orderMapper.selectById(payment.getOrderId());
                if (order != null && order.getStatus() == 0) {
                    order.setStatus(3); // 已过期
                    order.setUpdatedAt(LocalDateTime.now());
                    orderMapper.updateById(order);
                }

                log.info("Checkout session expired: sessionId={}, paymentId={}",
                    session.getId(), payment.getPaymentId());
            }

        } catch (Exception e) {
            log.error("Error handling checkout.session.expired event", e);
            throw new RuntimeException("Failed to process checkout.session.expired", e);
        }
    }

    /**
     * 处理 payment_intent.succeeded 事件
     * 当 PaymentIntent 成功时触发
     */
    @Transactional
    public void handlePaymentIntentSucceeded(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize payment intent"));

            log.info("Processing payment_intent.succeeded: paymentIntentId={}", paymentIntent.getId());

            // 根据 PaymentIntent ID 查询支付记录
            LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
            paymentQuery.eq(Payment::getStripePaymentIntentId, paymentIntent.getId());
            Payment payment = paymentMapper.selectOne(paymentQuery);

            if (payment != null) {
                log.info("Payment intent succeeded: paymentIntentId={}, paymentId={}",
                    paymentIntent.getId(), payment.getPaymentId());
            }

        } catch (Exception e) {
            log.error("Error handling payment_intent.succeeded event", e);
        }
    }

    /**
     * 处理 payment_intent.payment_failed 事件
     * 当 PaymentIntent 失败时触发
     */
    @Transactional
    public void handlePaymentIntentFailed(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize payment intent"));

            log.info("Processing payment_intent.payment_failed: paymentIntentId={}", paymentIntent.getId());

            // 根据 PaymentIntent ID 查询支付记录
            LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
            paymentQuery.eq(Payment::getStripePaymentIntentId, paymentIntent.getId());
            Payment payment = paymentMapper.selectOne(paymentQuery);

            if (payment != null && payment.getStatus() != 1) {
                // 更新支付状态为失败（如果还未成功）
                payment.setStatus(4); // 失败
                paymentMapper.updateById(payment);

                log.warn("Payment intent failed: paymentIntentId={}, paymentId={}",
                    paymentIntent.getId(), payment.getPaymentId());
            }

        } catch (Exception e) {
            log.error("Error handling payment_intent.payment_failed event", e);
        }
    }

    /**
     * 处理 charge.refunded 事件
     * 当退款时触发
     */
    @Transactional
    public void handleChargeRefunded(Event event) {
        try {
            Charge charge = (Charge) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize charge"));

            log.info("Processing charge.refunded: chargeId={}", charge.getId());

            // 根据 PaymentIntent ID 查询支付记录
            LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
            paymentQuery.eq(Payment::getStripePaymentIntentId, charge.getPaymentIntent());
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
                    User user = userMapper.selectById(order.getUserId());
                    if (user != null) {
                    // 使用PointService扣除积分
                    pointService.refundPoints(order.getUserId(), order.getPoints(), 2, "退款");
                    }
                }

                log.info("Charge refunded: chargeId={}, paymentId={}", charge.getId(), payment.getPaymentId());
            }

        } catch (Exception e) {
            log.error("Error handling charge.refunded event", e);
            throw new RuntimeException("Failed to process charge.refunded", e);
        }
    }
}

