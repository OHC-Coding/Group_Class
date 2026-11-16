package com.ohc.groupclass.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CourseTemplateVO {

    private Long id;

    private String name;

    private String sportType;

    private Integer defaultSessions;

    private BigDecimal defaultPrice;

    private String description;

    private Integer status;

    private LocalDateTime createdAt;
}
