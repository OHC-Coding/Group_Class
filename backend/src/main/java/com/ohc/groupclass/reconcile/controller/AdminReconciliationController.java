package com.ohc.groupclass.reconcile.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.reconcile.dto.ReconciliationVO;
import com.ohc.groupclass.reconcile.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reconciliation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    public Result<ReconciliationVO> run(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reconciliationService.run(date));
    }

    @GetMapping
    public Result<List<ReconciliationVO>> list(
            @RequestParam(defaultValue = "30") int limit) {
        return Result.ok(reconciliationService.list(limit));
    }
}
