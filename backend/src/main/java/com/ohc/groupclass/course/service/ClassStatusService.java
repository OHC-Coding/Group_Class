package com.ohc.groupclass.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.entity.ClassSession;
import com.ohc.groupclass.course.enums.ClassStatus;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.course.mapper.ClassSessionMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClassStatusService {

    private static final Logger log = LoggerFactory.getLogger(ClassStatusService.class);

    private static final String SESSION_NORMAL = "NORMAL";
    private static final String SESSION_FINISHED = "FINISHED";
    private static final String SESSION_CANCELLED = "CANCELLED";

    private final ClassInfoMapper classInfoMapper;
    private final ClassSessionMapper classSessionMapper;

    @Transactional
    public ClassInfo transition(Long classId, ClassStatus target) {
        ClassInfo entity = classInfoMapper.selectById(classId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        ClassStatus current = ClassStatus.of(entity.getStatus());
        current.checkTransitionTo(target);

        int affected = classInfoMapper.update(null, Wrappers.<ClassInfo>lambdaUpdate()
                .eq(ClassInfo::getId, classId)
                .eq(ClassInfo::getStatus, current.name())
                .set(ClassInfo::getStatus, target.name()));
        if (affected == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "班级状态已被其他操作修改，请刷新后重试");
        }

        if (target == ClassStatus.CANCELLED) {
            classSessionMapper.update(null, Wrappers.<ClassSession>lambdaUpdate()
                    .eq(ClassSession::getClassId, classId)
                    .eq(ClassSession::getStatus, SESSION_NORMAL)
                    .set(ClassSession::getStatus, SESSION_CANCELLED));
        }

        entity.setStatus(target.name());
        return entity;
    }

    @Transactional
    public void autoAdvance() {
        LocalDateTime now = LocalDateTime.now();

        int closed = classInfoMapper.update(null, Wrappers.<ClassInfo>lambdaUpdate()
                .eq(ClassInfo::getStatus, ClassStatus.OPEN.name())
                .le(ClassInfo::getEnrollCloseTime, now)
                .set(ClassInfo::getStatus, ClassStatus.CLOSED.name()));

        int finished = classInfoMapper.update(null, Wrappers.<ClassInfo>lambdaUpdate()
                .eq(ClassInfo::getStatus, ClassStatus.CLOSED.name())
                .lt(ClassInfo::getEndDate, LocalDate.now())
                .set(ClassInfo::getStatus, ClassStatus.FINISHED.name()));

        int sessions = classSessionMapper.update(null, Wrappers.<ClassSession>lambdaUpdate()
                .eq(ClassSession::getStatus, SESSION_NORMAL)
                .lt(ClassSession::getEndTime, now)
                .set(ClassSession::getStatus, SESSION_FINISHED));

        if (closed > 0 || finished > 0 || sessions > 0) {
            log.info("[班级状态] 自动流转：关闭报名 {} 个，结课 {} 个，课次完结 {} 条", closed, finished, sessions);
        }
    }
}
