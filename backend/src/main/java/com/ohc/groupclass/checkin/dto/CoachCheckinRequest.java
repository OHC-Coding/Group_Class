package com.ohc.groupclass.checkin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CoachCheckinRequest {

    @NotEmpty(message = "请选择学员")
    private List<Long> memberIds;
}
