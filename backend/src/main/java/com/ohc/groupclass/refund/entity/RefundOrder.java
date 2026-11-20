package com.ohc.groupclass.refund.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_order")
public class RefundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long enrollmentId;

    private Long orderId;

    private BigDecimal amount;

    private Integer totalSessions;

    private Integer checkedSessions;

    private String reason;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String status;

    private String auditRemark;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime refundTime;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
