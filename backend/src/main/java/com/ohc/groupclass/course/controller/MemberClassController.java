package com.ohc.groupclass.course.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.course.dto.MemberClassVO;
import com.ohc.groupclass.course.service.MemberClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class MemberClassController {

    private final MemberClassService memberClassService;

    @GetMapping
    public Result<List<MemberClassVO>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(memberClassService.listOpen(keyword));
    }
}
