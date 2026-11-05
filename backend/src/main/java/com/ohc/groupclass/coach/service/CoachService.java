package com.ohc.groupclass.coach.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.coach.dto.CoachCreateRequest;
import com.ohc.groupclass.coach.dto.CoachUpdateRequest;
import com.ohc.groupclass.coach.dto.CoachVO;
import com.ohc.groupclass.coach.entity.CoachProfile;
import com.ohc.groupclass.coach.mapper.CoachProfileMapper;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.user.entity.SysUser;
import com.ohc.groupclass.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachService {

    public static final String ROLE_COACH = "COACH";

    private final CoachProfileMapper coachProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public List<CoachVO> list() {
        List<CoachProfile> profiles = coachProfileMapper.selectList(
                Wrappers.<CoachProfile>lambdaQuery().orderByDesc(CoachProfile::getId));
        if (profiles.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = profiles.stream().map(CoachProfile::getUserId).toList();
        Map<Long, SysUser> userMap = sysUserMapper.selectList(
                        Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIds))
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return profiles.stream()
                .map(profile -> toVO(profile, userMap.get(profile.getUserId())))
                .toList();
    }

    @Transactional
    public CoachVO create(CoachCreateRequest request) {
        if (sysUserMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "账号已存在");
        }
        if (sysUserMapper.countByPhone(request.getPhone()) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "手机号已注册");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setNickname(isBlank(request.getNickname()) ? request.getRealName() : request.getNickname());
        user.setRole(ROLE_COACH);
        user.setStatus(1);
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CONFLICT, "账号或手机号已存在");
        }

        CoachProfile profile = new CoachProfile();
        profile.setUserId(user.getId());
        profile.setRealName(request.getRealName());
        profile.setUnitPrice(request.getUnitPrice());
        profile.setIntro(request.getIntro());
        profile.setStatus(1);
        coachProfileMapper.insert(profile);

        return toVO(profile, user);
    }

    @Transactional
    public CoachVO update(Long id, CoachUpdateRequest request) {
        CoachProfile profile = coachProfileMapper.selectById(id);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "教练档案不存在");
        }
        profile.setRealName(request.getRealName());
        profile.setUnitPrice(request.getUnitPrice());
        profile.setIntro(request.getIntro());
        if (request.getStatus() != null) {
            if (request.getStatus() != 0 && request.getStatus() != 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0 或 1");
            }
            profile.setStatus(request.getStatus());
        }
        coachProfileMapper.updateById(profile);

        SysUser user = sysUserMapper.selectById(profile.getUserId());
        if (user != null && request.getStatus() != null) {
            user.setStatus(request.getStatus());
            sysUserMapper.updateById(user);
        }
        return toVO(profile, user);
    }

    public CoachVO getByUserId(Long userId) {
        CoachProfile profile = coachProfileMapper.selectOne(
                Wrappers.<CoachProfile>lambdaQuery().eq(CoachProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "当前账号还没有教练档案");
        }
        return toVO(profile, sysUserMapper.selectById(userId));
    }

    public CoachProfile getEntityByUserId(Long userId) {
        return coachProfileMapper.selectOne(
                Wrappers.<CoachProfile>lambdaQuery().eq(CoachProfile::getUserId, userId));
    }

    private CoachVO toVO(CoachProfile profile, SysUser user) {
        return new CoachVO(
                profile.getId(),
                profile.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getPhone(),
                profile.getRealName(),
                profile.getUnitPrice(),
                profile.getIntro(),
                profile.getStatus());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}