package com.ohc.groupclass.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long classId;

    private String classTitle;

    private Long memberId;

    private String memberName;

    private String phone;

    private BigDecimal amount;

    private String channel;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;
}
