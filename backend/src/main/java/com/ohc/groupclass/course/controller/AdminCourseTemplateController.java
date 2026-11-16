package com.ohc.groupclass.course.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.course.dto.CourseTemplateCreateRequest;
import com.ohc.groupclass.course.dto.CourseTemplateUpdateRequest;
import com.ohc.groupclass.course.dto.CourseTemplateVO;
import com.ohc.groupclass.course.service.CourseTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/course-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseTemplateController {

    private final CourseTemplateService courseTemplateService;

    @GetMapping
    public Result<List<CourseTemplateVO>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(courseTemplateService.list(status));
    }

    @GetMapping("/{id}")
    public Result<CourseTemplateVO> get(@PathVariable Long id) {
        return Result.ok(courseTemplateService.getById(id));
    }

    @PostMapping
    public Result<CourseTemplateVO> create(@Valid @RequestBody CourseTemplateCreateRequest request) {
        return Result.ok(courseTemplateService.create(request));
    }

    @PutMapping("/{id}")
    public Result<CourseTemplateVO> update(@PathVariable Long id,
                                           @Valid @RequestBody CourseTemplateUpdateRequest request) {
        return Result.ok(courseTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        courseTemplateService.disable(id);
        return Result.ok();
    }
}
