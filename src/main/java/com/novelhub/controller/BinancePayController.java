package com.novelhub.controller;

import com.novelhub.service.BinancePayService;
import com.novelhub.service.UserService;
import com.novelhub.utils.JwtUtil;
import com.novelhub.vo.request.CreateBinancePayOrderRequest;
import com.novelhub.vo.response.AjaxResult;
import com.novelhub.vo.response.CreateBinancePayOrderResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Binance Pay 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/binance-pay")
@RequiredArgsConstructor
public class BinancePayController {

    private final BinancePayService binancePayService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 创建 Binance Pay 订单（生成二维码）
     *
     * @param request 创建请求
     * @return 订单信息（包含二维码）
     */
    @PostMapping("/create-order")
    public ResponseEntity<AjaxResult> createOrder(
            @RequestBody CreateBinancePayOrderRequest request,
            HttpServletRequest httpRequest) {

        try {
            log.info("Creating Binance Pay order for planId: {}", request.getPlanId());

            // 从 token 中获取用户 ID
            String username = jwtUtil.validUsername(httpRequest);
            var user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            Long userId = user.getUserId();

            CreateBinancePayOrderResponse response = binancePayService.createOrder(
                request.getPlanId(),
                userId,
                request.getReturnUrl(),
                request.getCancelUrl()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("order", response);
            return ResponseEntity.ok(AjaxResult.success("Binance Pay order created successfully", result));

        } catch (Exception e) {
            log.error("Failed to create Binance Pay order", e);
            return ResponseEntity.badRequest()
                .body(AjaxResult.error("Failed to create Binance Pay order: " + e.getMessage()));
        }
    }

    /**
     * 查询 Binance Pay 订单状态
     *
     * @param prepayId Binance Pay 订单 ID
     * @return 订单状态
     */
    @GetMapping("/order-status/{prepayId}")
    public ResponseEntity<AjaxResult> queryOrderStatus(@PathVariable String prepayId) {
        try {
            log.info("Querying Binance Pay order status: prepayId={}", prepayId);

            var status = binancePayService.queryOrderStatus(prepayId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", status);
            return ResponseEntity.ok(AjaxResult.success("Order status retrieved successfully", result));

        } catch (Exception e) {
            log.error("Failed to query Binance Pay order status", e);
            return ResponseEntity.badRequest()
                .body(AjaxResult.error("Failed to query order status: " + e.getMessage()));
        }
    }
}

