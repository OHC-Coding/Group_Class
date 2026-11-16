package com.ohc.groupclass.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class MyEnrollmentVO {

    private Long id;

    private Long classId;

    private String classTitle;

    private String sportType;

    private String coachName;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer totalSessions;

    private BigDecimal price;

    private String status;

    private LocalDateTime enrollTime;

    private LocalDateTime cancelTime;
}
