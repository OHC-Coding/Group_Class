package com.ohc.groupclass.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PaymentStatusVO {

    private Long orderId;

    private String orderNo;

    private String status;

    private String channelState;

    private LocalDateTime payTime;

    private long remainSeconds;
}
