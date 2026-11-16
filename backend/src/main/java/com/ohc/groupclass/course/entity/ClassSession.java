package com.ohc.groupclass.course.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_session")
public class ClassSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;

    private Integer sessionNo;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long venueId;

    private Long coachId;

    private String checkinCode;

    private LocalDateTime checkinExpireAt;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String status;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
