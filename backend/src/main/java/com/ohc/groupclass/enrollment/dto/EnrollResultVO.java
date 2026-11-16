package com.ohc.groupclass.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EnrollResultVO {

    private Long enrollmentId;

    private Long classId;

    private String status;

    private LocalDateTime enrollTime;

    private Integer remaining;
}
