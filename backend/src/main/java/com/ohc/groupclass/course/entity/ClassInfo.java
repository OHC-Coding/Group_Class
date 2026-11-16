package com.ohc.groupclass.course.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("class_info")
public class ClassInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String classNo;

    private Long templateId;

    private String title;

    private Long coachId;

    private Long venueId;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer totalSessions;

    private Integer capacity;

    private BigDecimal price;

    private BigDecimal coachUnitPrice;

    private LocalDateTime enrollOpenTime;

    private LocalDateTime enrollCloseTime;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String status;

    private String remark;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
