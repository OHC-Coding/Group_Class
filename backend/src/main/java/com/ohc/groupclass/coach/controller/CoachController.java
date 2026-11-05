package com.ohc.groupclass.coach.controller;

import com.ohc.groupclass.coach.dto.CoachVO;
import com.ohc.groupclass.coach.service.CoachService;
import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
public class CoachController {

    private final CoachService coachService;

    @GetMapping("/profile")
    public Result<CoachVO> profile(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(coachService.getByUserId(loginUser.getId()));
    }
}