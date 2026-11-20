package com.ohc.groupclass.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SettlementVO {

    private Long id;

    private String billNo;

    private Long coachId;

    private String coachName;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private Integer sessionCount;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private String status;

    private LocalDateTime createTime;
}
