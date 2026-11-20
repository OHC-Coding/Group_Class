package com.ohc.groupclass.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RosterItemVO {

    private Long enrollmentId;

    private Long memberId;

    private String username;

    private String nickname;

    private String phone;

    private String status;

    private LocalDateTime checkinTime;

    private String source;
}
