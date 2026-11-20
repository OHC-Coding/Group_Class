package com.ohc.groupclass.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DashboardVO {

    private int classCount;

    private int openClassCount;

    private int capacityTotal;

    private long confirmedTotal;

    private BigDecimal occupancyRate;

    private BigDecimal paidAmount;

    private BigDecimal refundedAmount;

    private BigDecimal netAmount;

    private long pendingRefundCount;

    private long todaySessionCount;

    private long todayCheckedCount;

    private long finishedSessionCount;

    private long expectedAttendance;

    private long checkedAttendance;

    private BigDecimal attendanceRate;

    private BigDecimal noShowRate;
}
