package com.ohc.groupclass.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ClassStatsVO {

    private Long classId;

    private String title;

    private String status;

    private Integer capacity;

    private long confirmed;

    private BigDecimal occupancyRate;

    private long finishedSessions;

    private long expectedAttendance;

    private long checkedAttendance;

    private BigDecimal attendanceRate;

    private BigDecimal noShowRate;

    private BigDecimal paidAmount;

    private BigDecimal refundedAmount;
}
