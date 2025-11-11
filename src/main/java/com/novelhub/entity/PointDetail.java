package com.novelhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 积分明细实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_point_detail")
public class PointDetail {
    @TableId(value = "point_detail_id", type = IdType.AUTO)
    private Long pointDetailId;
    private Long userId;
    private Integer points;
    private Integer type;
    private Integer funcType;
    private Integer pointsType;
    private String taskId;
    private Integer isApi;
    private String extraData;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}


