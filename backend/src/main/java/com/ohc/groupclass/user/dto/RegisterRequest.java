package com.ohc.groupclass.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "不能为空")
    @Size(min = 4, max = 20, message = "长度需在 4-20 之间")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 32, message = "长度需在 6-32 之间")
    private String password;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "格式不正确")
    private String phone;

    @Size(max = 20, message = "长度不能超过 20")
    private String nickname;
}
