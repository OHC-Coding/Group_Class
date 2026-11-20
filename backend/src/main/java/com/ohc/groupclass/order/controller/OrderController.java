package com.ohc.groupclass.order.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.order.dto.OrderVO;
import com.ohc.groupclass.order.service.OrderService;
import com.ohc.groupclass.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/my/orders")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<List<OrderVO>> mine(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(orderService.listMine(loginUser.getId()));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<OrderVO> detail(@PathVariable Long id,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(orderService.getById(id, loginUser.getId()));
    }
}
