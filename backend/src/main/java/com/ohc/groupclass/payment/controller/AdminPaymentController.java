package com.ohc.groupclass.payment.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.payment.dto.CallbackResultVO;
import com.ohc.groupclass.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{id}/notify")
    public Result<CallbackResultVO> repush(@PathVariable Long id) {
        return Result.ok(paymentService.repush(id));
    }
}
