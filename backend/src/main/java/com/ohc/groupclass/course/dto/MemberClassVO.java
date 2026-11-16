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
public class MemberClassVO {

    private Long id;

    private String title;

    private String sportType;

    private String coachName;

    private String venueName;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer totalSessions;

    private Integer capacity;

    private BigDecimal price;

    private LocalDateTime enrollOpenTime;

    private LocalDateTime enrollCloseTime;

    private List<MemberSessionVO> sessions;
}
