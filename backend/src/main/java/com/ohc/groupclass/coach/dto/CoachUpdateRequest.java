package com.ohc.groupclass.coach.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CoachUpdateRequest {

    @NotBlank(message = "教练姓名不能为空")
    @Size(max = 50, message = "教练姓名最多 50 个字符")
    private String realName;

    @NotNull(message = "结算单价不能为空")
    @DecimalMin(value = "0.00", message = "结算单价不能为负数")
    private BigDecimal unitPrice;

    @Size(max = 500, message = "简介最多 500 个字符")
    private String intro;

    private Integer status;
}