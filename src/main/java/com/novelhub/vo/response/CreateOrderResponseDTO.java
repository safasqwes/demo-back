package com.novelhub.vo.response;

import lombok.Data;

/**
 * 创建订单响应DTO
 */
@Data
public class CreateOrderResponseDTO {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
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
     * 订单金额（分）
     */
    private Integer amount;
    
    /**
     * 货币
     */
    private String currency;
    
    /**
     * 获得积分
     */
    private Integer points;
    
    /**
     * 错误信息
     */
    private String error;
    
    public CreateOrderResponseDTO(Boolean success, Long orderId, String orderNumber, 
                                 String planName, Integer amount, String currency, 
                                 Integer points, String error) {
        this.success = success;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.planName = planName;
        this.amount = amount;
        this.currency = currency;
        this.points = points;
        this.error = error;
    }
}

