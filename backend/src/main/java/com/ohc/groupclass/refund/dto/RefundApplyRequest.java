package com.ohc.groupclass.refund.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundApplyRequest {

    @Size(max = 200, message = "退款原因不能超过 200 字")
    private String reason;
}
