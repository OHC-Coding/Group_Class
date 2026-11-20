package com.ohc.groupclass.checkin.controller;

import com.ohc.groupclass.checkin.dto.CheckinRequest;
import com.ohc.groupclass.checkin.dto.CheckinResultVO;
import com.ohc.groupclass.checkin.service.CheckinService;
import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class MemberCheckinController {

    private final CheckinService checkinService;

    @PostMapping("/{id}/checkin")
    @PreAuthorize("hasRole('MEMBER')")
    public Result<CheckinResultVO> checkin(@PathVariable Long id,
                                           @Valid @RequestBody CheckinRequest request,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(checkinService.checkinByCode(id, loginUser.getId(), request.getCode()));
    }
}
