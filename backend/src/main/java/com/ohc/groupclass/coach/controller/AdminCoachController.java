package com.ohc.groupclass.coach.controller;

import com.ohc.groupclass.coach.dto.CoachCreateRequest;
import com.ohc.groupclass.coach.dto.CoachUpdateRequest;
import com.ohc.groupclass.coach.dto.CoachVO;
import com.ohc.groupclass.coach.service.CoachService;
import com.ohc.groupclass.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coaches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCoachController {

    private final CoachService coachService;

    @GetMapping
    public Result<List<CoachVO>> list() {
        return Result.ok(coachService.list());
    }

    @PostMapping
    public Result<CoachVO> create(@Valid @RequestBody CoachCreateRequest request) {
        return Result.ok(coachService.create(request));
    }

    @PutMapping("/{id}")
    public Result<CoachVO> update(@PathVariable Long id,
                                  @Valid @RequestBody CoachUpdateRequest request) {
        return Result.ok(coachService.update(id, request));
    }
}