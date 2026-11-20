package com.ohc.groupclass.checkin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckinRequest {

    @NotBlank(message = "签到码不能为空")
    private String code;
}
