package com.novelhub.vo.request;

import lombok.Data;

/**
 * 验证支付请求 DTO
 */
@Data
public class VerifyPaymentRequestDTO {
    
    /**
     * 支付ID
     */
    private Long paymentId;
    
    /**
     * 交易哈希
     */
    private String txHash;
    
    /**
     * 支付地址
     */
    private String fromAddress;
}

