package com.novelhub.vo.request;

import lombok.Data;

/**
 * 创建订单请求DTO
 */
@Data
public class CreateOrderRequestDTO {
    
    /**
     * 套餐ID
     */
    private Long planId;
    
    /**
     * 订单描述
     */
    private String description;
}

