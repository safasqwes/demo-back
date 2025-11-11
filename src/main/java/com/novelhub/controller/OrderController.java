package com.novelhub.controller;

import com.novelhub.vo.request.*;
import com.novelhub.vo.response.*;
import com.novelhub.entity.PaymentPlan;
import com.novelhub.service.PaymentService;
import com.novelhub.service.UserService;
import com.novelhub.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建订单
     */
    @PostMapping
    public ResponseEntity<AjaxResult> createOrder(@RequestBody CreateOrderRequestDTO request, HttpServletRequest httpRequest) {
        try {
            String username = jwtUtil.validUsername(httpRequest);
            log.info("username={}", username);
            
            var user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            Long userId = user.getUserId();
            CreateOrderResponseDTO response = paymentService.createOrder(request, userId);
            
            if (response.getSuccess()) {
                log.info("Order created successfully - orderId: {}, orderNumber: {}", 
                        response.getOrderId(), response.getOrderNumber());
                Map<String, Object> result = new HashMap<>();
                result.put("order", response);
                return ResponseEntity.ok(AjaxResult.success("Order created successfully", result));
            } else {
                return ResponseEntity.badRequest().body(AjaxResult.error(response.getError()));
            }
        } catch (Exception e) {
            log.error("Create order error", e);
            return ResponseEntity.internalServerError().body(AjaxResult.error("Failed to create order: " + e.getMessage()));
        }
    }

    /**
     * 获取订单状态
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<AjaxResult> getOrderStatus(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        try {
            String username = jwtUtil.validUsername(httpRequest);
            
            var user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Long userId = user.getUserId();
            OrderDTO order = paymentService.getOrderDetail(orderId, userId);
            
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            
            return ResponseEntity.ok(AjaxResult.success("Order status retrieved successfully", result));
        } catch (Exception e) {
            log.error("Get order status error", e);
            return ResponseEntity.internalServerError().body(AjaxResult.error("Failed to get order status: " + e.getMessage()));
        }
    }

    /**
     * 根据订单号获取订单信息
     */
    @GetMapping("/by-order-no/{orderNo}")
    public ResponseEntity<AjaxResult> getOrderByOrderNo(@PathVariable String orderNo, HttpServletRequest httpRequest) {
        try {
            String username = jwtUtil.validUsername(httpRequest);
            
            var user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Long userId = user.getUserId();
            OrderDTO order = paymentService.getOrderByOrderNumber(orderNo, userId);
            
            if (order == null) {
                return ResponseEntity.status(404).body(AjaxResult.error("Order not found with order number: " + orderNo));
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            
            return ResponseEntity.ok(AjaxResult.success("Order retrieved successfully", result));
        } catch (Exception e) {
            log.error("Get order by order number error", e);
            return ResponseEntity.internalServerError().body(AjaxResult.error("Failed to get order by order number: " + e.getMessage()));
        }
    }

    /**
     * 获取用户订单历史
     */
    @GetMapping
    public ResponseEntity<AjaxResult> getUserOrders(HttpServletRequest httpRequest) {
        try {
            String username = jwtUtil.validUsername(httpRequest);
            
            var user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Long userId = user.getUserId();
            List<OrderDTO> orders = paymentService.getUserOrders(userId);
            
            return ResponseEntity.ok(AjaxResult.success(orders));
        } catch (Exception e) {
            log.error("Get user orders error", e);
            return ResponseEntity.internalServerError().body(AjaxResult.error("Failed to get user orders: " + e.getMessage()));
        }
    }

    /**
     * 获取支付计划
     */
    @GetMapping("/plans")
    public ResponseEntity<AjaxResult> getPlans() {
        try {
            List<PaymentPlan> plans = paymentService.getAvailablePlans();
            return ResponseEntity.ok(AjaxResult.success(plans));
        } catch (Exception e) {
            log.error("Get payment plans error", e);
            return ResponseEntity.internalServerError().body(AjaxResult.error("Failed to get payment plans: " + e.getMessage()));
        }
    }

    /**
     * 创建结账（创建订单+支付）
     */
    @PostMapping("/checkout")
    public ResponseEntity<AjaxResult> createCheckout(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String username = jwtUtil.validUsername(httpRequest);
        try {
            var user = userService.getUserByUsername(username);
            if (user == null) return ResponseEntity.notFound().build();

            Long userId = user.getUserId();
            String planIdStr = request.get("planId");
            String successUrl = request.get("successUrl");
            String cancelUrl = request.get("cancelUrl");
            if (!StringUtils.hasText(planIdStr)) {
                return ResponseEntity.badRequest().body(AjaxResult.error("Plan ID is required"));
            }
            Long planId = Long.valueOf(planIdStr);
            // Create order first
            CreateOrderRequestDTO orderRequest = new CreateOrderRequestDTO();
            orderRequest.setPlanId(planId);
            CreateOrderResponseDTO orderResponse = paymentService.createOrder(orderRequest, userId);
            
            if (!orderResponse.getSuccess()) {
                return ResponseEntity.badRequest().body(AjaxResult.error(orderResponse.getError()));
            }
            
            // Create Stripe payment
            CreatePaymentResponseDTO paymentResponse = paymentService.createStripePayment(orderResponse.getOrderId(), successUrl, cancelUrl);
            
            if (!paymentResponse.getSuccess()) {
                return ResponseEntity.badRequest().body(AjaxResult.error(paymentResponse.getError()));
            }
            
            String checkoutUrl = paymentResponse.getStripeCheckoutUrl();
            Map<String, Object> result = new HashMap<>();
            result.put("checkoutUrl", checkoutUrl);
            return ResponseEntity.ok(AjaxResult.success("Checkout session created", result));
        } catch (Exception e) {
            log.error("Create checkout error", e);
            return ResponseEntity.internalServerError().body(AjaxResult.error("Failed to create checkout: " + e.getMessage()));
        }
    }

    /**
     * 支付成功回调
     */
    @GetMapping("/payment/success")
    public ResponseEntity<AjaxResult> getPaymentSuccess(@RequestParam String session_id, HttpServletRequest httpRequest) {
        try {
            String username = jwtUtil.validUsername(httpRequest);
            var user = userService.getUserByUsername(username);
            if (user == null) return ResponseEntity.notFound().build();

            // Get order details by session ID - need to implement this method or use alternative approach
            // For now, we'll return a simple success message
            // TODO: Implement getOrderBySessionId method in PaymentService
            return ResponseEntity.ok(AjaxResult.success("Payment successful"));
        } catch (Exception e) {
            log.error("Failed to get payment success data", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Failed to get payment success data: " + e.getMessage()));
        }
    }

    /**
     * 支付取消回调
     */
    @GetMapping("/payment/cancel")
    public ResponseEntity<AjaxResult> getPaymentCancel() {
        try {
            return ResponseEntity.ok(AjaxResult.success("Payment cancelled"));
        } catch (Exception e) {
            log.error("Failed to get payment cancel data", e);
            return ResponseEntity.internalServerError()
                    .body(AjaxResult.error("Failed to get payment cancel data: " + e.getMessage()));
        }
    }
}
