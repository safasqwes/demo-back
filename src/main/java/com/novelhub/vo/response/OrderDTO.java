package com.novelhub.vo.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 DTO（包含支付信息）
 */
@Data
public class OrderDTO {
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 订单号
     */
    private String orderNumber;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 套餐ID
     */
    private Long planId;
    
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
     * 订单状态：0-待支付 1-已支付 2-已取消 3-已过期 4-已退款
     */
    private Integer status;
    
    /**
     * 订单类型：1-订阅 2-一次性购买
     */
    private Integer orderType;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 支付记录列表
     */
    private List<PaymentDTO> payments;
}

