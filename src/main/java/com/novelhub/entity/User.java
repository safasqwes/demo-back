package com.novelhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_user")
public class User {
    
    /**
     * 用户ID
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码（PBKDF2加密）
     */
    private String password;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 性别：0-未知 1-男 2-女
     */
    private Integer gender;
    
    /**
     * 生日
     */
    private LocalDate birthday;
    
    /**
     * 个人简介
     */
    private String introduction;
    
    /**
     * 状态：0-禁用 1-正常
     */
    private Integer status;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 最后登录IP
     */
    private String lastLoginIp;
    /**
     * 邮箱是否验证：0-未验证 1-已验证
     */
    @TableField(exist = false)
    private Integer emailVerified;

    /**
     * 邮箱验证令牌
     */
    @TableField(exist = false)
    private String verificationToken;

    /**
     * 令牌过期时间
     */
    @TableField(exist = false)
    private LocalDateTime tokenExpiry;

    /**
     * 钱包地址
     */
    @TableField("wallet_address")
    private String walletAddress;
    
    /**
     * 钱包类型：metamask, walletconnect, coinbase
     */
    @TableField("wallet_type")
    private String walletType;
    
    /**
     * 防重放攻击的nonce
     */
    @TableField("nonce")
    private String nonce;
    
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
    
    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}

