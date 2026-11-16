package com.ohc.groupclass.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ClassVO {

    private Long id;

    private String classNo;

    private Long templateId;

    private String title;

    private Long coachId;

    private String coachName;

    private Long venueId;

    private String venueName;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer totalSessions;

    private Integer capacity;

    private BigDecimal price;

    private BigDecimal coachUnitPrice;

    private LocalDateTime enrollOpenTime;

    private LocalDateTime enrollCloseTime;

    private String status;

    private String remark;

    private List<ClassSessionVO> sessions;
}
