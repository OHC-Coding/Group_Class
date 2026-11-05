package com.ohc.groupclass.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.security.JwtTokenProvider;
import com.ohc.groupclass.user.dto.AuthResponse;
import com.ohc.groupclass.user.dto.LoginRequest;
import com.ohc.groupclass.user.dto.RegisterRequest;
import com.ohc.groupclass.user.dto.UserInfo;
import com.ohc.groupclass.user.entity.SysUser;
import com.ohc.groupclass.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ROLE_MEMBER = "MEMBER";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (sysUserMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "\u8d26\u53f7\u5df2\u5b58\u5728");
        }
        if (sysUserMapper.countByPhone(request.getPhone()) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "\u624b\u673a\u53f7\u5df2\u6ce8\u518c");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname() == null || request.getNickname().isBlank()
                ? request.getUsername() : request.getNickname());
        user.setRole(ROLE_MEMBER);
        user.setStatus(1);
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CONFLICT, "\u8d26\u53f7\u6216\u624b\u673a\u53f7\u5df2\u5b58\u5728");
        }
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "\u8d26\u53f7\u6216\u5bc6\u7801\u9519\u8bef");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "\u8d26\u53f7\u5df2\u7981\u7528");
        }
        return buildResponse(user);
    }

    public UserInfo getInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "\u7528\u6237\u4e0d\u5b58\u5728");
        }
        return toInfo(user);
    }

    private AuthResponse buildResponse(SysUser user) {
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, "Bearer",
                jwtTokenProvider.getExpireMillis() / 1000, toInfo(user));
    }

    private UserInfo toInfo(SysUser user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getNickname(),
                user.getPhone(), user.getRole());
    }
}
