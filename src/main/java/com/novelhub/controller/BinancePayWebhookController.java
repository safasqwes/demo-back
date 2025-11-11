package com.novelhub.controller;

import com.novelhub.service.BinancePayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Binance Pay Webhook 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/binance-pay/webhook")
@RequiredArgsConstructor
public class BinancePayWebhookController {

    private final BinancePayWebhookService binancePayWebhookService;

    /**
     * 处理 Binance Pay Webhook 事件
     *
     * @param payload Webhook 请求体
     * @param timestamp BinancePay-Timestamp 头部
     * @param nonce BinancePay-Nonce 头部
     * @param signature BinancePay-Signature 头部
     * @param certificateSN BinancePay-Certificate-SN 头部
     * @return 响应结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("BinancePay-Timestamp") String timestamp,
            @RequestHeader("BinancePay-Nonce") String nonce,
            @RequestHeader("BinancePay-Signature") String signature,
            @RequestHeader("BinancePay-Certificate-SN") String certificateSN) {

        log.info("Received Binance Pay webhook event");

        try {
            // 验证 Webhook 签名
            boolean isValid = binancePayWebhookService.verifySignature(
                timestamp, nonce, payload, signature
            );

            if (!isValid) {
                log.error("Webhook signature verification failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "returnCode", "FAIL",
                        "returnMessage", "Invalid signature"
                    ));
            }

            // 处理 Webhook 事件
            binancePayWebhookService.handleWebhook(payload);

            return ResponseEntity.ok(Map.of(
                "returnCode", "SUCCESS",
                "returnMessage", null
            ));

        } catch (Exception e) {
            log.error("Error processing Binance Pay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "returnCode", "FAIL",
                    "returnMessage", "Error processing webhook"
                ));
        }
    }
}

