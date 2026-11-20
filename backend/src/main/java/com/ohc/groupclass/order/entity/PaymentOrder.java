package com.ohc.groupclass.order.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_order")
public class PaymentOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long enrollmentId;

    private Long memberId;

    private BigDecimal amount;

    private String channel;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String status;

    private LocalDateTime expireTime;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime payTime;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String notifyNo;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
