package com.ohc.groupclass.payment.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.payment.dto.PaymentStatusVO;
import com.ohc.groupclass.payment.dto.PrepayVO;
import com.ohc.groupclass.payment.service.PaymentService;
import com.ohc.groupclass.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<PrepayVO> prepay(@PathVariable Long id,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(paymentService.prepay(id, loginUser.getId()));
    }

    @GetMapping("/{id}/pay-status")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<PaymentStatusVO> status(@PathVariable Long id,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(paymentService.status(id, loginUser.getId()));
    }
}
