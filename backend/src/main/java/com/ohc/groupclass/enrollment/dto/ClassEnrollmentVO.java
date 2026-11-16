package com.ohc.groupclass.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ClassEnrollmentVO {

    private Long id;

    private Long memberId;

    private String username;

    private String nickname;

    private String phone;

    private String status;

    private LocalDateTime enrollTime;

    private LocalDateTime cancelTime;
}
