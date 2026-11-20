package com.ohc.groupclass.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CallbackResultVO {

    private boolean accepted;

    private boolean duplicate;

    private String orderStatus;

    private String message;
}
