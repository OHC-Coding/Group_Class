package com.ohc.groupclass.coach.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CoachCreateRequest {

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 50, message = "登录账号最多 50 个字符")
    private String username;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 位之间")
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Size(max = 20, message = "手机号最多 20 个字符")
    private String phone;

    @Size(max = 50, message = "昵称最多 50 个字符")
    private String nickname;

    @NotBlank(message = "教练姓名不能为空")
    @Size(max = 50, message = "教练姓名最多 50 个字符")
    private String realName;

    @NotNull(message = "结算单价不能为空")
    @DecimalMin(value = "0.00", message = "结算单价不能为负数")
    private BigDecimal unitPrice;

    @Size(max = 500, message = "简介最多 500 个字符")
    private String intro;
}