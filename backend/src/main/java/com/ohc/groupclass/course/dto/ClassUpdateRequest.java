package com.ohc.groupclass.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClassUpdateRequest {

    @NotNull(message = "教练不能为空")
    private Long coachId;

    @NotNull(message = "场地不能为空")
    private Long venueId;

    @NotNull(message = "班级价格不能为空")
    @DecimalMin(value = "0.00", message = "班级价格不能为负数")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "教练课酬不能为负数")
    private BigDecimal coachUnitPrice;

    @NotNull(message = "名额不能为空")
    @Min(value = 1, message = "名额至少 1 个")
    private Integer capacity;

    @NotNull(message = "报名开放时间不能为空")
    private LocalDateTime enrollOpenTime;

    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime enrollCloseTime;

    @Size(max = 500, message = "备注最多 500 个字符")
    private String remark;

    @Valid
    private List<SessionItem> sessions;
}
