package com.ohc.groupclass.course.enums;

import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;

import java.util.Map;
import java.util.Set;

public enum ClassStatus {

    DRAFT,
    OPEN,
    CLOSED,
    FINISHED,
    CANCELLED;

    private static final Map<ClassStatus, Set<ClassStatus>> ALLOWED = Map.of(
            DRAFT, Set.of(OPEN, CANCELLED),
            OPEN, Set.of(CLOSED, CANCELLED),
            CLOSED, Set.of(FINISHED, CANCELLED),
            FINISHED, Set.of(),
            CANCELLED, Set.of()
    );

    public static ClassStatus of(String value) {
        try {
            return ClassStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未知的班级状态：" + value);
        }
    }

    public void checkTransitionTo(ClassStatus target) {
        if (!ALLOWED.get(this).contains(target)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "班级状态不能从 " + this + " 变为 " + target);
        }
    }

    public boolean isDraft() {
        return this == DRAFT;
    }

    public boolean isOpen() {
        return this == OPEN;
    }
}
