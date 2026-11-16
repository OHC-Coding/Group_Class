package com.ohc.groupclass.enrollment.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.enrollment.dto.ClassEnrollmentVO;
import com.ohc.groupclass.enrollment.dto.StockVO;
import com.ohc.groupclass.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/{classId}/enrollments")
    public Result<List<ClassEnrollmentVO>> list(@PathVariable Long classId) {
        return Result.ok(enrollmentService.listByClass(classId));
    }

    @PostMapping("/{classId}/stock/rebuild")
    public Result<StockVO> rebuildStock(@PathVariable Long classId) {
        return Result.ok(enrollmentService.rebuildStock(classId));
    }
}
