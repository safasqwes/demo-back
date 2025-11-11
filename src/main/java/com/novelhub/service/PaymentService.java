package com.novelhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novelhub.vo.request.*;
import com.novelhub.vo.response.*;
import com.novelhub.entity.Order;
import com.novelhub.entity.Payment;
import com.novelhub.entity.PaymentPlan;
import com.novelhub.mapper.OrderMapper;
import com.novelhub.mapper.PaymentMapper;
import com.novelhub.mapper.PaymentPlanMapper;
import com.novelhub.utils.JwtUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    @Autowired
    private PaymentPlanMapper paymentPlanMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private PointService pointService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PriceService priceService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 获取可用套餐
     */
    public List<PaymentPlan> getAvailablePlans() {
        LambdaQueryWrapper<PaymentPlan> qw = new LambdaQueryWrapper<>();
        qw.eq(PaymentPlan::getStatus, 1).orderByAsc(PaymentPlan::getPrice);
        return paymentPlanMapper.selectList(qw);
    }

    /**
     * 根据ID获取套餐
     */
    public PaymentPlan getPlanById(Long planId) {
        return paymentPlanMapper.selectById(planId);
    }

    /**
     * 创建订单（购物订单）
     */
    public CreateOrderResponseDTO createOrder(CreateOrderRequestDTO request, Long userId) {
        try {
            // 1. 获取套餐信息
            PaymentPlan plan = getPlanById(request.getPlanId());
            if (plan == null) {
                return new CreateOrderResponseDTO(false, null, null, null, null, null, null, "套餐不存在");
            }

            // 2. 生成订单号
            String orderNumber = jwtUtil.generateNewToken();

            // 3. 创建订单
            Order order = Order.builder()
                    .userId(userId)
                    .orderNumber(orderNumber)
                    .planId(plan.getPlanId())
                    .planName(plan.getPlanName())
                    .amount(plan.getPrice())
                    .currency(plan.getCurrency())
                    .points(plan.getPointsAmount())
                    .status(0) // 待支付
                    .orderType(plan.getPlanType())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            orderMapper.insert(order);

            log.info("订单创建成功: orderId={}, userId={}, planId={}", order.getOrderId(), userId, request.getPlanId());

            return new CreateOrderResponseDTO(true, order.getOrderId(), order.getOrderNumber(),
                    order.getPlanName(), order.getAmount(), order.getCurrency(), order.getPoints(), null);

        } catch (Exception e) {
            log.error("创建订单失败: userId={}, planId={}", userId, request.getPlanId(), e);
            return new CreateOrderResponseDTO(false, null, null, null, null, null, null,
                    "创建订单失败: " + e.getMessage());
        }
    }

    /**
     * 创建Stripe支付
     */
    public CreatePaymentResponseDTO createStripePayment(Long orderId, String successUrl, String cancelUrl) {
        try {
            // 1. 获取订单信息
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return new CreatePaymentResponseDTO(false, null, null, null, null, null, null, "订单不存在");
            }

            if (order.getStatus() != 0) {
                return new CreatePaymentResponseDTO(false, null, null, null, null, null, null, "订单状态不正确");
            }

            // 2. 获取套餐信息
            PaymentPlan plan = getPlanById(order.getPlanId());
            if (plan == null || plan.getStripePriceId() == null) {
                return new CreatePaymentResponseDTO(false, null, null, null, null, null, null, "套餐配置错误");
            }

            // 3. 创建Stripe会话
            Customer customer = getOrCreateCustomer(order.getUserId());
            String baseUrl = "http://localhost:8080" + contextPath;
            String finalSuccessUrl = StringUtils.hasText(successUrl) ? successUrl : baseUrl + "/payment/success";
            String finalCancelUrl = StringUtils.hasText(cancelUrl) ? cancelUrl : baseUrl + "/payment/cancel";

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setCustomer(customer.getId())
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(plan.getPlanType() == 1 ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(finalSuccessUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(finalCancelUrl)
                    .putMetadata("order_id", orderId.toString())
                    .putMetadata("user_id", order.getUserId().toString())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(plan.getStripePriceId())
                            .setQuantity(1L)
                            .build());

            Session session = Session.create(paramsBuilder.build());

            // 4. 创建支付记录
            String paymentNumber = "stripe_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .userId(order.getUserId())
                    .paymentNumber(paymentNumber)
                    .paymentMethod("stripe")
                    .amount(order.getAmount())
                    .currency(order.getCurrency())
                    .status(0) // 待支付
                    .expiresAt(LocalDateTime.now().plusMinutes(15)) // 15分钟过期
                    .stripeSessionId(session.getId())
                    .stripeCustomerId(customer.getId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            paymentMapper.insert(payment);

            log.info("Stripe支付创建成功: paymentId={}, orderId={}, sessionId={}", payment.getPaymentId(), orderId, session.getId());

            return new CreatePaymentResponseDTO(true, payment.getPaymentId(), payment.getPaymentNumber(),
                    "stripe", payment.getAmount(), payment.getCurrency(), payment.getExpiresAt().toString(),
                    session.getId(), session.getUrl(), null, null, null, null, null);

        } catch (StripeException e) {
            log.error("创建Stripe支付失败: orderId={}", orderId, e);
            return new CreatePaymentResponseDTO(false, null, null, null, null, null, null,
                    "创建支付失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("创建Stripe支付失败: orderId={}", orderId, e);
            return new CreatePaymentResponseDTO(false, null, null, null, null, null, null,
                    "创建支付失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单详情（包含支付记录）
     */
    public OrderDTO getOrderDetail(Long orderId, Long userId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null || !order.getUserId().equals(userId)) {
                return null;
            }

            // 获取支付记录
            LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
            paymentQuery.eq(Payment::getOrderId, orderId).orderByDesc(Payment::getCreatedAt);
            List<Payment> payments = paymentMapper.selectList(paymentQuery);

            // 转换为DTO
            OrderDTO orderDTO = convertToOrderDTO(order);
            orderDTO.setPayments(convertToPaymentDTOs(payments));

            return orderDTO;

        } catch (Exception e) {
            log.error("获取订单详情失败: orderId={}, userId={}", orderId, userId, e);
            return null;
        }
    }

    /**
     * 根据订单号获取订单详情
     */
    public OrderDTO getOrderByOrderNumber(String orderNumber, Long userId) {
        try {
            LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
            orderQuery.eq(Order::getOrderNumber, orderNumber)
                     .eq(Order::getUserId, userId);
            Order order = orderMapper.selectOne(orderQuery);
            
            if (order == null) {
                return null;
            }

            // 获取支付记录
            LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
            paymentQuery.eq(Payment::getOrderId, order.getOrderId()).orderByDesc(Payment::getCreatedAt);
            List<Payment> payments = paymentMapper.selectList(paymentQuery);

            // 转换为DTO
            OrderDTO orderDTO = convertToOrderDTO(order);
            orderDTO.setPayments(convertToPaymentDTOs(payments));

            return orderDTO;

        } catch (Exception e) {
            log.error("根据订单号获取订单详情失败: orderNumber={}, userId={}", orderNumber, userId, e);
            return null;
        }
    }

    /**
     * 获取用户订单列表
     */
    public List<OrderDTO> getUserOrders(Long userId) {
        try {
            LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
            orderQuery.eq(Order::getUserId, userId).orderByDesc(Order::getCreatedAt);
            List<Order> orders = orderMapper.selectList(orderQuery);

            return orders.stream().map(order -> {
                OrderDTO orderDTO = convertToOrderDTO(order);
                
                // 获取最新支付记录
                LambdaQueryWrapper<Payment> paymentQuery = new LambdaQueryWrapper<>();
                paymentQuery.eq(Payment::getOrderId, order.getOrderId())
                           .orderByDesc(Payment::getCreatedAt)
                           .last("LIMIT 1");
                Payment latestPayment = paymentMapper.selectOne(paymentQuery);
                if (latestPayment != null) {
                    orderDTO.setPayments(List.of(convertToPaymentDTO(latestPayment)));
                }
                
                return orderDTO;
            }).collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户订单列表失败: userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * 获取支付信息
     */
    public PaymentInfoDTO getPaymentInfo(Long paymentId, Long userId) {
        try {
            // 1. 获取支付记录
            Payment payment = paymentMapper.selectById(paymentId);
            if (payment == null) {
                return null;
            }

            // 2. 验证用户权限
            if (!payment.getUserId().equals(userId)) {
                return null;
            }

            // 3. 获取订单信息
            Order order = orderMapper.selectById(payment.getOrderId());
            if (order == null) {
                return null;
            }

            // 4. 构建支付信息DTO
            PaymentInfoDTO dto = new PaymentInfoDTO();
            dto.setPaymentId(payment.getPaymentId());
            dto.setOrderId(payment.getOrderId());
            dto.setOrderNumber(order.getOrderNumber());
            dto.setPlanName(order.getPlanName());
            dto.setFiatAmount(payment.getAmount() / 100.0);
            dto.setCurrency(payment.getCurrency());
            dto.setTokenAmount(payment.getTokenAmount().toString());
            dto.setTokenCurrency(payment.getTokenCurrency());
            dto.setRecipientAddress(payment.getToAddress());
            dto.setChainId(payment.getChainId());
            dto.setStatus(payment.getStatus());
            dto.setExpiresAt(payment.getExpiresAt());
            dto.setPriceTTL(payment.getPriceTtl());

            return dto;

        } catch (Exception e) {
            log.error("获取支付信息失败: paymentId={}, userId={}", paymentId, userId, e);
            return null;
        }
    }

    /**
     * 获取支付状态
     */
    public PaymentStatusDTO getPaymentStatus(Long paymentId, Long userId) {
        try {
            // 1. 获取支付记录
            Payment payment = paymentMapper.selectById(paymentId);
            if (payment == null) {
                return null;
            }

            // 2. 验证用户权限
            if (!payment.getUserId().equals(userId)) {
                return null;
            }

            // 3. 构建支付状态DTO
            PaymentStatusDTO dto = new PaymentStatusDTO();
            dto.setPaymentId(payment.getPaymentId());
            dto.setStatus(payment.getStatus());
            dto.setTxHash(payment.getTxHash());
            dto.setConfirmations(payment.getConfirmations());
            dto.setRequiredConfirmations(0); // Web3支付已移除，设置为0

            return dto;

        } catch (Exception e) {
            log.error("获取支付状态失败: paymentId={}, userId={}", paymentId, userId, e);
            return null;
        }
    }

    /**
     * 获取或创建Stripe客户
     */
    private Customer getOrCreateCustomer(Long userId) throws StripeException {
        LambdaQueryWrapper<Payment> qw = new LambdaQueryWrapper<>();
        qw.eq(Payment::getUserId, userId).isNotNull(Payment::getStripeCustomerId).last("LIMIT 1");
        Payment existing = paymentMapper.selectOne(qw);
        if (existing != null && StringUtils.hasText(existing.getStripeCustomerId())) {
            return Customer.retrieve(existing.getStripeCustomerId());
        }
        Map<String, Object> params = new HashMap<>();
        params.put("description", "Customer for user " + userId);
        params.put("metadata", Map.of("user_id", userId.toString()));
        return Customer.create(params);
    }

    /**
     * 转换Order为OrderDTO
     */
    private OrderDTO convertToOrderDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUserId());
        dto.setPlanId(order.getPlanId());
        dto.setPlanName(order.getPlanName());
        dto.setAmount(order.getAmount());
        dto.setCurrency(order.getCurrency());
        dto.setPoints(order.getPoints());
        dto.setStatus(order.getStatus());
        dto.setOrderType(order.getOrderType());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }

    /**
     * 转换Payment为PaymentDTO
     */
    private PaymentDTO convertToPaymentDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setOrderId(payment.getOrderId());
        dto.setUserId(payment.getUserId());
        dto.setPaymentNumber(payment.getPaymentNumber());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setStatus(payment.getStatus());
        dto.setExpiresAt(payment.getExpiresAt());
        dto.setPaidAt(payment.getPaidAt());
        dto.setCreatedAt(payment.getCreatedAt());
        
        // Stripe字段
        dto.setStripeSessionId(payment.getStripeSessionId());
        dto.setStripeCustomerId(payment.getStripeCustomerId());
        dto.setStripeSubscriptionId(payment.getStripeSubscriptionId());
        dto.setStripePaymentIntentId(payment.getStripePaymentIntentId());
        
        // Web3字段
        dto.setTxHash(payment.getTxHash());
        dto.setFromAddress(payment.getFromAddress());
        dto.setToAddress(payment.getToAddress());
        dto.setTokenAmount(payment.getTokenAmount());
        dto.setTokenCurrency(payment.getTokenCurrency());
        dto.setChainId(payment.getChainId());
        dto.setBlockNumber(payment.getBlockNumber());
        dto.setConfirmations(payment.getConfirmations());
        dto.setPriceTtl(payment.getPriceTtl());
        
        return dto;
    }

    /**
     * 转换Payment列表为PaymentDTO列表
     */
    private List<PaymentDTO> convertToPaymentDTOs(List<Payment> payments) {
        return payments.stream()
                .map(this::convertToPaymentDTO)
                .collect(java.util.stream.Collectors.toList());
    }
}
