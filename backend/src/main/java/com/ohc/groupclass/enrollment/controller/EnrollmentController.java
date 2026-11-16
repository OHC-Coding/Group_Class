package com.ohc.groupclass.enrollment.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.enrollment.dto.EnrollResultVO;
import com.ohc.groupclass.enrollment.dto.MyEnrollmentVO;
import com.ohc.groupclass.enrollment.service.EnrollmentService;
import com.ohc.groupclass.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/classes/{classId}/enroll")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<EnrollResultVO> enroll(@PathVariable Long classId,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(enrollmentService.enroll(classId, loginUser.getId()));
    }

    @DeleteMapping("/enrollments/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<Void> cancel(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser) {
        enrollmentService.cancel(id, loginUser.getId());
        return Result.ok();
    }

    @GetMapping("/my/enrollments")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<List<MyEnrollmentVO>> mine(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(enrollmentService.listMine(loginUser.getId()));
    }
}
