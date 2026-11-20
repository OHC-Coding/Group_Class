package com.ohc.groupclass.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PrepayVO {

    private String channel;

    private String orderNo;

    private BigDecimal amount;

    private LocalDateTime expireTime;

    private String payToken;
}
