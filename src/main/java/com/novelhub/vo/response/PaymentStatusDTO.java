package com.novelhub.vo.response;

import lombok.Data;

/**
 * 支付状态DTO
 */
@Data
public class PaymentStatusDTO {
    
    /**
     * 支付ID
     */
    private Long paymentId;
    
    /**
     * 支付状态 (0-待支付 1-已支付 2-支付中 3-已过期 4-已取消)
     */
    private Integer status;
    
    /**
     * 交易哈希
     */
    private String txHash;
    
    /**
     * 确认数
     */
    private Integer confirmations;
    
    /**
     * 所需确认数
     */
    private Integer requiredConfirmations;
}

