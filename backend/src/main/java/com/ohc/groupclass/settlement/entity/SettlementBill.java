package com.ohc.groupclass.settlement.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("settlement_bill")
public class SettlementBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String billNo;

    private Long coachId;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private Integer sessionCount;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String status;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
