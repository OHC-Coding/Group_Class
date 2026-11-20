package com.ohc.groupclass.settlement.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.settlement.dto.SettlementGenerateRequest;
import com.ohc.groupclass.settlement.dto.SettlementVO;
import com.ohc.groupclass.settlement.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettlementController {

    private final SettlementService settlementService;

    @PostMapping("/generate")
    public Result<List<SettlementVO>> generate(@Valid @RequestBody SettlementGenerateRequest request) {
        return Result.ok(settlementService.generate(request.getPeriodStart(), request.getPeriodEnd()));
    }

    @GetMapping
    public Result<List<SettlementVO>> list(
            @RequestParam(required = false) Long coachId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return Result.ok(settlementService.list(coachId, periodStart, periodEnd));
    }

    @PostMapping("/{id}/confirm")
    public Result<SettlementVO> confirm(@PathVariable Long id) {
        return Result.ok(settlementService.confirm(id));
    }
}
