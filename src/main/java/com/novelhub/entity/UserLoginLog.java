package com.novelhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户登录日志实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_user_login_log")
public class UserLoginLog {

    @TableId(value = "login_log_id", type = IdType.AUTO)
    private Long loginLogId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime loginTime;

    private String loginIp;

    private String device;

    private String browser;

    private String os;

    private String fingerprint;

    private Integer loginStatus;

    private String remark;
}


