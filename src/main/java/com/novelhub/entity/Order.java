package com.novelhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 订单实体类（购物订单）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_order")
public class Order {
    
    /**
     * 订单ID
     */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 订单号
     */
    private String orderNumber;
    
    /**
     * 套餐ID
     */
    private Long planId;
    
    /**
     * 套餐名称
     */
    private String planName;
    
    /**
     * 支付金额（分）
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
     * 状态：0-待支付 1-已支付 2-已取消 3-已过期 4-已退款
     */
    private Integer status;
    
    /**
     * 订单类型：1-订阅 2-一次性购买
     */
    private Integer orderType;
    
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
}


