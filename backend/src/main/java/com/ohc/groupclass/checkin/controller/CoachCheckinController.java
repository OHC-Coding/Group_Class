package com.ohc.groupclass.checkin.controller;

import com.ohc.groupclass.checkin.dto.CheckinCodeVO;
import com.ohc.groupclass.checkin.dto.CheckinResultVO;
import com.ohc.groupclass.checkin.dto.CoachCheckinRequest;
import com.ohc.groupclass.checkin.dto.RosterVO;
import com.ohc.groupclass.checkin.service.CheckinService;
import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coach/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
public class CoachCheckinController {

    private final CheckinService checkinService;

    @GetMapping("/{id}/checkin-code")
    public Result<CheckinCodeVO> code(@PathVariable Long id,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(checkinService.issueCode(id, loginUser.getId()));
    }

    @GetMapping("/{id}/roster")
    public Result<RosterVO> roster(@PathVariable Long id,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(checkinService.roster(id, loginUser.getId()));
    }

    @PostMapping("/{id}/checkin")
    public Result<List<CheckinResultVO>> checkin(@PathVariable Long id,
                                                 @Valid @RequestBody CoachCheckinRequest request,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(checkinService.manualCheckin(id, request.getMemberIds(), loginUser.getId()));
    }
}
