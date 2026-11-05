package com.ohc.groupclass.user.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.security.LoginUser;
import com.ohc.groupclass.user.dto.AuthResponse;
import com.ohc.groupclass.user.dto.LoginRequest;
import com.ohc.groupclass.user.dto.RegisterRequest;
import com.ohc.groupclass.user.dto.UserInfo;
import com.ohc.groupclass.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserInfo> me(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(authService.getInfo(loginUser.getId()));
    }
}
