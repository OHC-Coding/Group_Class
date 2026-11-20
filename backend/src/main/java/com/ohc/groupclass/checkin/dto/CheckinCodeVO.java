package com.ohc.groupclass.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CheckinCodeVO {

    private Long sessionId;

    private Long classId;

    private Integer sessionNo;

    private String className;

    private String venueName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String code;

    private LocalDateTime expireAt;

    private long checked;

    private long total;
}
