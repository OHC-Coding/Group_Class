package com.ohc.groupclass.settlement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SettlementGenerateRequest {

    @NotNull(message = "开始日期不能为空")
    private LocalDate periodStart;

    @NotNull(message = "结束日期不能为空")
    private LocalDate periodEnd;
}
