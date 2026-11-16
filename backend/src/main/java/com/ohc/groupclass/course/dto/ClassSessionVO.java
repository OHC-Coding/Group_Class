package com.ohc.groupclass.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ClassSessionVO {

    private Long id;

    private Integer sessionNo;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long venueId;

    private String venueName;

    private Long coachId;

    private String coachName;

    private String status;
}
