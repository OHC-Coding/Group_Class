package com.ohc.groupclass.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CheckinResultVO {

    private Long memberId;

    private Long enrollmentId;

    private boolean accepted;

    private boolean duplicate;

    private LocalDateTime checkinTime;

    private String message;
}
