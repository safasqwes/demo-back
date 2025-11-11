package com.novelhub.vo.response;

import lombok.Data;

/**
 * 验证支付响应 DTO
 */
@Data
public class VerifyPaymentResponseDTO {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 是否已确认
     */
    private Boolean confirmed;
    
    /**
     * 当前确认数
     */
    private Integer confirmations;
    
    /**
     * 所需确认数
     */
    private Integer requiredConfirmations;
    
    /**
     * 区块高度
     */
    private Long blockNumber;
    
    /**
     * Gas使用量
     */
    private String gasUsed;
    
    /**
     * Gas价格
     */
    private String gasPrice;
    
    /**
     * 错误信息
     */
    private String error;
    
    public VerifyPaymentResponseDTO() {}
    
    public VerifyPaymentResponseDTO(Boolean success, Boolean confirmed, Integer confirmations, 
                                   Integer requiredConfirmations, Long blockNumber, String gasUsed, 
                                   String gasPrice, String error) {
        this.success = success;
        this.confirmed = confirmed;
        this.confirmations = confirmations;
        this.requiredConfirmations = requiredConfirmations;
        this.blockNumber = blockNumber;
        this.gasUsed = gasUsed;
        this.gasPrice = gasPrice;
        this.error = error;
    }
}

