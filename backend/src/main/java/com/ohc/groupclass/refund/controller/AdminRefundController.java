package com.ohc.groupclass.refund.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.refund.dto.RefundRejectRequest;
import com.ohc.groupclass.refund.dto.RefundVO;
import com.ohc.groupclass.refund.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRefundController {

    private final RefundService refundService;

    @GetMapping
    public Result<List<RefundVO>> list(@RequestParam(required = false) String status) {
        return Result.ok(refundService.listByStatus(status));
    }

    @PostMapping("/{id}/approve")
    public Result<RefundVO> approve(@PathVariable Long id) {
        return Result.ok(refundService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public Result<RefundVO> reject(@PathVariable Long id,
                                   @Valid @RequestBody RefundRejectRequest request) {
        return Result.ok(refundService.reject(id, request.getReason()));
    }
}
