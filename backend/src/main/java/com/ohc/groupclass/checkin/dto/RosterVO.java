package com.ohc.groupclass.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class RosterVO {

    private Long sessionId;

    private Long classId;

    private Integer sessionNo;

    private String className;

    private String venueName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String sessionStatus;

    private long total;

    private long checked;

    private List<RosterItemVO> records;
}
