package com.ohc.groupclass.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockPayRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "支付令牌不能为空")
    private String payToken;
}
