package com.ohc.groupclass.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CoachVO {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private String phone;

    private String realName;

    private BigDecimal unitPrice;

    private String intro;

    private Integer status;
}