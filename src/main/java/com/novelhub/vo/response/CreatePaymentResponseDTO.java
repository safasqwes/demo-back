package com.novelhub.vo.response;

import lombok.Data;

/**
 * 创建支付响应DTO
 */
@Data
public class CreatePaymentResponseDTO {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 支付ID
     */
    private Long paymentId;
    
    /**
     * 支付单号
     */
    private String paymentNumber;
    
    /**
     * 支付方式
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
     * 过期时间
     */
    private String expiresAt;
    
    // Stripe相关
    /**
     * Stripe会话ID
     */
    private String stripeSessionId;
    
    /**
     * Stripe支付URL
     */
    private String stripeCheckoutUrl;
    
    // Web3相关
    /**
     * 收款地址
     */
    private String recipientAddress;
    
    /**
     * 代币数量
     */
    private String tokenAmount;
    
    /**
     * 代币类型
     */
    private String tokenCurrency;
    
    /**
     * 价格锁定时间
     */
    private Long priceTtl;
    
    /**
     * 错误信息
     */
    private String error;
    
    public CreatePaymentResponseDTO(Boolean success, Long paymentId, String paymentNumber,
                                   String paymentMethod, Integer amount, String currency,
                                   String expiresAt, String error) {
        this.success = success;
        this.paymentId = paymentId;
        this.paymentNumber = paymentNumber;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
        this.expiresAt = expiresAt;
        this.error = error;
    }
    
    public CreatePaymentResponseDTO(Boolean success, Long paymentId, String paymentNumber,
                                   String paymentMethod, Integer amount, String currency,
                                   String expiresAt, String stripeSessionId, String stripeCheckoutUrl,
                                   String recipientAddress, String tokenAmount, String tokenCurrency,
                                   Long priceTtl, String error) {
        this.success = success;
        this.paymentId = paymentId;
        this.paymentNumber = paymentNumber;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
        this.expiresAt = expiresAt;
        this.stripeSessionId = stripeSessionId;
        this.stripeCheckoutUrl = stripeCheckoutUrl;
        this.recipientAddress = recipientAddress;
        this.tokenAmount = tokenAmount;
        this.tokenCurrency = tokenCurrency;
        this.priceTtl = priceTtl;
        this.error = error;
    }
}

