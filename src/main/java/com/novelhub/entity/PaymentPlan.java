package com.novelhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 支付套餐实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_payment_plan")
public class PaymentPlan {
    
    /**
     * 套餐ID
     */
    @TableId(value = "plan_id", type = IdType.AUTO)
    private Long planId;
    
    /**
     * Stripe价格ID
     */
    private String stripePriceId;
    
    /**
     * 套餐名称
     */
    private String planName;
    
    /**
     * 套餐描述
     */
    private String description;
    
    /**
     * 价格（分）
     */
    private Integer price;
    
    /**
     * 货币
     */
    private String currency;
    
    /**
     * 包含积分
     */
    private Integer pointsAmount;
    
    /**
     * 有效期（天），NULL表示永久
     */
    private Integer durationDays;
    
    /**
     * 套餐类型：1-订阅 2-一次性购买
     */
    private Integer planType;
    
    /**
     * 功能特性
     */
    private String features;
    
    /**
     * 是否启用：0-禁用 1-启用
     */
    private Integer status;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
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


