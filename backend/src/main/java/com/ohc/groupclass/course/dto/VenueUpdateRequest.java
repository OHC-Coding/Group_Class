package com.ohc.groupclass.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VenueUpdateRequest {

    @NotBlank(message = "场地名称不能为空")
    @Size(max = 50, message = "场地名称最多 50 个字符")
    private String name;

    @Size(max = 200, message = "地址最多 200 个字符")
    private String address;

    @NotNull(message = "容纳人数不能为空")
    @Min(value = 0, message = "容纳人数不能为负数")
    private Integer capacity;

    private Integer status;
}
