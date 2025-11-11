package com.novelhub.vo.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付信息DTO
 */
@Data
public class PaymentInfoDTO {
    
    /**
     * 支付ID
     */
    private Long paymentId;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 订单号
     */
    private String orderNumber;
    
    /**
     * 套餐名称
     */
    private String planName;
    
    /**
     * 法币金额
     */
    private Double fiatAmount;
    
    /**
     * 法币类型
     */
    private String currency;
    
    /**
     * 代币数量
     */
    private String tokenAmount;
    
    /**
     * 代币类型
     */
    private String tokenCurrency;
    
    /**
     * 收款地址
     */
    private String recipientAddress;
    
    /**
     * 链ID
     */
    private Integer chainId;
    
    /**
     * 支付状态 (0-待支付 1-已支付 2-支付中 3-已过期 4-已取消)
     */
    private Integer status;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 价格有效期时间戳
     */
    private Long priceTTL;
}

