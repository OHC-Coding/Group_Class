package com.ohc.groupclass.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockVO {

    private Long classId;

    private Integer capacity;

    private Integer occupied;

    private Integer remaining;
}
