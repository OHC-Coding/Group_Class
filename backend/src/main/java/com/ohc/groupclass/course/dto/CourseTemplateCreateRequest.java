package com.ohc.groupclass.course.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseTemplateCreateRequest {

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称最多 100 个字符")
    private String name;

    @Size(max = 30, message = "类型最多 30 个字符")
    private String sportType;

    @NotNull(message = "默认课次不能为空")
    @Min(value = 1, message = "默认课次至少 1 次")
    private Integer defaultSessions;

    @NotNull(message = "默认价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能为负数")
    private BigDecimal defaultPrice;

    @Size(max = 500, message = "描述最多 500 个字符")
    private String description;
}
