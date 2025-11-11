package com.novelhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户积分实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_user_points")
public class UserPoint {
    @TableId(value = "user_points_id", type = IdType.AUTO)
    private Long userPointsId;
    private Long userId;
    private Integer points;
    private Integer fixedPoints;
    private Integer subPoints;
    private Integer subPointsLeft;
    private Integer freePoints;
    private Integer claimedDays;
    private LocalDateTime claimedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}


