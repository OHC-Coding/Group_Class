package com.ohc.groupclass.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class VenueVO {

    private Long id;

    private String name;

    private String address;

    private Integer capacity;

    private Integer status;

    private LocalDateTime createdAt;
}
