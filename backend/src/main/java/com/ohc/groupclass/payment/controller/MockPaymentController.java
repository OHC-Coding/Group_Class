package com.ohc.groupclass.payment.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.payment.dto.CallbackResultVO;
import com.ohc.groupclass.payment.dto.MockPayRequest;
import com.ohc.groupclass.payment.service.PaymentCallbackService;
import com.ohc.groupclass.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class MockPaymentController {

    private final PaymentService paymentService;
    private final PaymentCallbackService paymentCallbackService;

    @PostMapping("/mock/pay")
    public Result<CallbackResultVO> pay(@Valid @RequestBody MockPayRequest request) {
        return Result.ok(paymentService.mockPay(request.getOrderNo(), request.getPayToken()));
    }

    @PostMapping("/callback/{channel}")
    public Result<CallbackResultVO> callback(@PathVariable String channel,
                                             @RequestHeader(value = "X-Pay-Sign", required = false) String signature,
                                             @RequestBody String rawBody) {
        return Result.ok(paymentCallbackService.handle(channel, rawBody, signature));
    }
}
