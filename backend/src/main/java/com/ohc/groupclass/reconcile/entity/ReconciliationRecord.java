package com.ohc.groupclass.reconcile.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reconciliation_record")
public class ReconciliationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate bizDate;

    private Integer enrollCount;

    private Integer paidCount;

    private Integer occupiedCount;

    private Integer redisRemain;

    private Integer diffCount;

    private String result;

    private String detail;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
