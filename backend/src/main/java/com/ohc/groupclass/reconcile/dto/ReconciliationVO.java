package com.ohc.groupclass.reconcile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReconciliationVO {

    private Long id;

    private LocalDate bizDate;

    private Integer enrollCount;

    private Integer paidCount;

    private Integer occupiedCount;

    private Integer redisRemain;

    private Integer diffCount;

    private String result;

    private String detail;

    private LocalDateTime createTime;
}
