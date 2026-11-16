package com.ohc.groupclass.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class MemberSessionVO {

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venueName;

    private String coachName;
}
