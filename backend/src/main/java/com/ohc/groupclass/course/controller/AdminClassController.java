package com.ohc.groupclass.course.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.course.dto.ClassCreateRequest;
import com.ohc.groupclass.course.dto.ClassUpdateRequest;
import com.ohc.groupclass.course.dto.ClassVO;
import com.ohc.groupclass.course.dto.SessionAdjustRequest;
import com.ohc.groupclass.course.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminClassController {

    private final ClassService classService;

    @GetMapping
    public Result<List<ClassVO>> list(@RequestParam(required = false) String status,
                                      @RequestParam(required = false) String keyword) {
        return Result.ok(classService.list(status, keyword));
    }

    @GetMapping("/{id}")
    public Result<ClassVO> get(@PathVariable Long id) {
        return Result.ok(classService.getById(id));
    }

    @PostMapping
    public Result<ClassVO> create(@Valid @RequestBody ClassCreateRequest request) {
        return Result.ok(classService.create(request));
    }

    @PutMapping("/{id}")
    public Result<ClassVO> update(@PathVariable Long id,
                                  @Valid @RequestBody ClassUpdateRequest request) {
        return Result.ok(classService.update(id, request));
    }

    @PutMapping("/{id}/publish")
    public Result<ClassVO> publish(@PathVariable Long id) {
        return Result.ok(classService.publish(id));
    }

    @PutMapping("/{id}/cancel")
    public Result<ClassVO> cancel(@PathVariable Long id) {
        return Result.ok(classService.cancel(id));
    }

    @PutMapping("/{id}/sessions/{sessionId}")
    public Result<ClassVO> adjustSession(@PathVariable Long id,
                                         @PathVariable Long sessionId,
                                         @Valid @RequestBody SessionAdjustRequest request) {
        return Result.ok(classService.adjustSession(id, sessionId, request));
    }

    @PutMapping("/{id}/sessions/{sessionId}/cancel")
    public Result<ClassVO> cancelSession(@PathVariable Long id, @PathVariable Long sessionId) {
        return Result.ok(classService.cancelSession(id, sessionId));
    }
}
