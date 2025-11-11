package com.novelhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_payment")
public class Payment {
    
    /**
     * 支付ID
     */
    @TableId(value = "payment_id", type = IdType.AUTO)
    private Long paymentId;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 支付单号
     */
    private String paymentNumber;
    
    /**
     * 支付方式：stripe, web3, binance_pay
     */
    private String paymentMethod;
    
    /**
     * 支付金额（分）
     */
    private Integer amount;
    
    /**
     * 货币
     */
    private String currency;
    
    /**
     * 状态：0-待支付 1-已支付 2-已取消 3-已过期
     */
    private Integer status;
    
    /**
     * 过期时间（15分钟）
     */
    private LocalDateTime expiresAt;
    
    /**
     * 支付超时时间
     */
    private LocalDateTime paymentTimeoutAt;
    
    // ============================================
    // Stripe相关字段
    // ============================================
    
    /**
     * Stripe会话ID
     */
    private String stripeSessionId;
    
    /**
     * Stripe客户ID
     */
    private String stripeCustomerId;
    
    /**
     * Stripe订阅ID
     */
    private String stripeSubscriptionId;
    
    /**
     * Stripe支付意图ID
     */
    private String stripePaymentIntentId;
    
    // ============================================
    // Binance Pay相关字段
    // ============================================
    
    /**
     * Binance Pay预支付ID
     */
    private String binancePrepayId;
    
    /**
     * Binance Pay交易ID
     */
    private String binanceTransactionId;
    
    // ============================================
    // Web3相关字段
    // ============================================
    
    /**
     * 区块链交易哈希
     */
    private String txHash;
    
    /**
     * 支付地址
     */
    private String fromAddress;
    
    /**
     * 收款地址
     */
    private String toAddress;
    
    /**
     * 代币数量
     */
    private BigDecimal tokenAmount;
    
    /**
     * 代币类型
     */
    private String tokenCurrency;
    
    /**
     * 区块链ID
     */
    private Integer chainId;
    
    /**
     * 区块高度
     */
    private Long blockNumber;
    
    /**
     * 确认数
     */
    private Integer confirmations;
    
    /**
     * 价格锁定时间
     */
    private Long priceTtl;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 支付完成时间
     */
    private LocalDateTime paidAt;
}
