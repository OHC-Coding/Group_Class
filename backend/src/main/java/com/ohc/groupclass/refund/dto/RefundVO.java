package com.ohc.groupclass.refund.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RefundVO {

    private Long id;

    private String refundNo;

    private Long enrollmentId;

    private Long classId;

    private String classTitle;

    private Long memberId;

    private String memberName;

    private BigDecimal amount;

    private Integer totalSessions;

    private Integer checkedSessions;

    private String reason;

    private String status;

    private String auditRemark;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;
}
