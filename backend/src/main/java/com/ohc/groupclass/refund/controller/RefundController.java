package com.ohc.groupclass.refund.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.refund.dto.RefundApplyRequest;
import com.ohc.groupclass.refund.dto.RefundVO;
import com.ohc.groupclass.refund.service.RefundService;
import com.ohc.groupclass.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/enrollments/{id}/refund")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<RefundVO> apply(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) RefundApplyRequest request,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        String reason = request == null ? null : request.getReason();
        return Result.ok(refundService.apply(id, loginUser.getId(), reason));
    }

    @GetMapping("/my/refunds")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<List<RefundVO>> mine(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(refundService.listMine(loginUser.getId()));
    }
}
