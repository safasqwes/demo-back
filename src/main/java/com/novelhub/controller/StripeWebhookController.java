package com.novelhub.controller;

import com.novelhub.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe Webhook 控制器
 * 用于接收和处理 Stripe 的 Webhook 事件
 */
@Slf4j
@RestController
@RequestMapping("/api/stripe/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    /**
     * 处理 Stripe Webhook 事件
     *
     * @param payload Webhook 请求体（原始 JSON 字符串）
     * @param sigHeader Stripe-Signature 头部
     * @return 响应结果
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Received Stripe webhook event");

        Event event;

        try {
            // 验证 Webhook 签名
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            log.info("Webhook signature verified. Event type: {}, Event ID: {}",
                event.getType(), event.getId());

        } catch (SignatureVerificationException e) {
            // 签名验证失败
            log.error("Webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid signature");
        } catch (Exception e) {
            log.error("Error parsing webhook event", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error parsing event");
        }

        // 处理不同类型的事件
        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    // 支付成功
                    stripeWebhookService.handleCheckoutSessionCompleted(event);
                    break;

                case "checkout.session.expired":
                    // Checkout Session 过期
                    stripeWebhookService.handleCheckoutSessionExpired(event);
                    break;

                case "payment_intent.succeeded":
                    // Payment Intent 成功
                    stripeWebhookService.handlePaymentIntentSucceeded(event);
                    break;

                case "payment_intent.payment_failed":
                    // Payment Intent 失败
                    stripeWebhookService.handlePaymentIntentFailed(event);
                    break;

                case "charge.refunded":
                    // 退款
                    stripeWebhookService.handleChargeRefunded(event);
                    break;

                default:
                    log.info("Unhandled event type: {}", event.getType());
            }

            return ResponseEntity.ok("Webhook handled successfully");

        } catch (Exception e) {
            log.error("Error processing webhook event: {}", event.getType(), e);
            // 返回 500 会让 Stripe 重试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
}

