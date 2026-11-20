package com.ohc.groupclass.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundRejectRequest {

    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 200, message = "驳回原因不能超过 200 字")
    private String reason;
}
