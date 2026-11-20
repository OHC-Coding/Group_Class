package com.ohc.groupclass.stats.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.stats.dto.ClassStatsVO;
import com.ohc.groupclass.stats.dto.DashboardVO;
import com.ohc.groupclass.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.ok(statsService.dashboard());
    }

    @GetMapping("/classes/{id}/stats")
    public Result<ClassStatsVO> classStats(@PathVariable Long id) {
        return Result.ok(statsService.classStats(id));
    }
}
