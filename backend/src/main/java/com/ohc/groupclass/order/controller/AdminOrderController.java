package com.ohc.groupclass.order.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.order.dto.OrderVO;
import com.ohc.groupclass.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/{classId}/orders")
    public Result<List<OrderVO>> list(@PathVariable Long classId) {
        return Result.ok(orderService.listByClass(classId));
    }
}
