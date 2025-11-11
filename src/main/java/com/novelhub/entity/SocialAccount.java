package com.novelhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 社交账号实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_social_account")
public class SocialAccount {
    
    /**
     * 社交账号ID
     */
    @TableId(value = "social_account_id", type = IdType.AUTO)
    private Long socialAccountId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 第三方平台：1-Google 2-Facebook 3-GitHub
     */
    private Integer provider;
    
    /**
     * 第三方平台用户ID
     */
    private String providerId;
    
    /**
     * 额外信息（头像、姓名等JSON数据）
     */
    private String extraData;
    
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
