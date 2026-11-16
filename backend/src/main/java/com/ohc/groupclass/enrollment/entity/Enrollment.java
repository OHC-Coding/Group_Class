package com.ohc.groupclass.enrollment.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("enrollment")
public class Enrollment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;

    private Long memberId;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String status;

    private LocalDateTime enrollTime;

    private LocalDateTime cancelTime;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
