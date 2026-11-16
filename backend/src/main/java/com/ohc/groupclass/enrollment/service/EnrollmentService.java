package com.ohc.groupclass.enrollment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.coach.entity.CoachProfile;
import com.ohc.groupclass.coach.mapper.CoachProfileMapper;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.entity.CourseTemplate;
import com.ohc.groupclass.course.enums.ClassStatus;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.course.mapper.CourseTemplateMapper;
import com.ohc.groupclass.enrollment.dto.ClassEnrollmentVO;
import com.ohc.groupclass.enrollment.dto.EnrollResultVO;
import com.ohc.groupclass.enrollment.dto.MyEnrollmentVO;
import com.ohc.groupclass.enrollment.dto.StockVO;
import com.ohc.groupclass.enrollment.entity.Enrollment;
import com.ohc.groupclass.enrollment.enums.EnrollmentStatus;
import com.ohc.groupclass.enrollment.mapper.EnrollmentMapper;
import com.ohc.groupclass.user.entity.SysUser;
import com.ohc.groupclass.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentMapper enrollmentMapper;
    private final ClassInfoMapper classInfoMapper;
    private final CourseTemplateMapper courseTemplateMapper;
    private final CoachProfileMapper coachProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final StockService stockService;

    /**
     * 抢位顺序：先校验 → Redis 原子扣减 → 落库；落库失败补偿 Redis。
     * 失败方向只会少卖，不会超卖；不套 @Transactional，避免唯一键冲突把事务标成只回滚。
     */
    public EnrollResultVO enroll(Long classId, Long memberId) {
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        if (!ClassStatus.OPEN.name().equals(classInfo.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "班级当前不在报名中");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(classInfo.getEnrollOpenTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报名尚未开始");
        }
        if (!now.isBefore(classInfo.getEnrollCloseTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报名已截止");
        }

        Enrollment existing = findByClassAndMember(classId, memberId);
        if (existing != null && isOccupying(existing.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "你已报名该班级");
        }

        StockService.StockResult stock;
        try {
            stock = stockService.tryEnroll(classId, memberId);
            if (stock.code() == StockService.CODE_NOT_READY) {
                stockService.rebuild(classId);
                stock = stockService.tryEnroll(classId, memberId);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[抢课] Redis 扣减异常 classId={} memberId={}", classId, memberId, e);
            throw new BusinessException(ResultCode.ERROR, "库存服务不可用，请稍后重试");
        }

        if (stock.code() == StockService.CODE_DUPLICATE) {
            throw new BusinessException(ResultCode.CONFLICT, "你已报名该班级");
        }
        if (stock.code() == StockService.CODE_SOLD_OUT) {
            throw new BusinessException(ResultCode.SOLD_OUT);
        }
        if (stock.code() == StockService.CODE_NOT_READY) {
            throw new BusinessException(ResultCode.ERROR, "库存未初始化，请稍后重试");
        }

        Enrollment saved;
        try {
            if (existing == null) {
                saved = new Enrollment();
                saved.setClassId(classId);
                saved.setMemberId(memberId);
                saved.setStatus(EnrollmentStatus.PENDING_PAYMENT.name());
                saved.setEnrollTime(now);
                enrollmentMapper.insert(saved);
            } else {
                int affected = enrollmentMapper.update(null, Wrappers.<Enrollment>lambdaUpdate()
                        .eq(Enrollment::getId, existing.getId())
                        .in(Enrollment::getStatus,
                                EnrollmentStatus.CANCELLED.name(),
                                EnrollmentStatus.REFUNDED.name())
                        .set(Enrollment::getStatus, EnrollmentStatus.PENDING_PAYMENT.name())
                        .set(Enrollment::getEnrollTime, now)
                        .set(Enrollment::getCancelTime, null));
                if (affected == 0) {
                    stockService.compensate(classId, memberId);
                    Enrollment latest = enrollmentMapper.selectById(existing.getId());
                    if (latest != null && isOccupying(latest.getStatus())) {
                        return toResult(latest);
                    }
                    throw new BusinessException(ResultCode.CONFLICT, "报名状态已变化，请刷新后重试");
                }
                saved = existing;
                saved.setStatus(EnrollmentStatus.PENDING_PAYMENT.name());
                saved.setEnrollTime(now);
                saved.setCancelTime(null);
            }
        } catch (DuplicateKeyException e) {
            stockService.compensate(classId, memberId);
            Enrollment latest = findByClassAndMember(classId, memberId);
            if (latest != null && isOccupying(latest.getStatus())) {
                return toResult(latest);
            }
            throw new BusinessException(ResultCode.CONFLICT, "报名冲突，请重试");
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            stockService.compensate(classId, memberId);
            throw e;
        }
        return toResult(saved);
    }

    /**
     * 取消顺序：先条件更新数据库，影响 1 行才回补 Redis，重复取消不会重复加名额。
     */
    public void cancel(Long enrollmentId, Long memberId) {
        Enrollment enrollment = enrollmentMapper.selectById(enrollmentId);
        if (enrollment == null || !Objects.equals(enrollment.getMemberId(), memberId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名不存在");
        }
        if (EnrollmentStatus.CANCELLED.name().equals(enrollment.getStatus())) {
            return;
        }
        if (!EnrollmentStatus.PENDING_PAYMENT.name().equals(enrollment.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有待支付的报名可以取消");
        }

        int affected = enrollmentMapper.update(null, Wrappers.<Enrollment>lambdaUpdate()
                .eq(Enrollment::getId, enrollmentId)
                .eq(Enrollment::getMemberId, memberId)
                .eq(Enrollment::getStatus, EnrollmentStatus.PENDING_PAYMENT.name())
                .set(Enrollment::getStatus, EnrollmentStatus.CANCELLED.name())
                .set(Enrollment::getCancelTime, LocalDateTime.now()));
        if (affected == 0) {
            Enrollment latest = enrollmentMapper.selectById(enrollmentId);
            if (latest != null && EnrollmentStatus.CANCELLED.name().equals(latest.getStatus())) {
                return;
            }
            throw new BusinessException(ResultCode.CONFLICT, "报名状态已变化，请刷新后重试");
        }
        stockService.release(enrollment.getClassId(), memberId);
    }

    public List<MyEnrollmentVO> listMine(Long memberId) {
        List<Enrollment> enrollments = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getMemberId, memberId)
                .orderByDesc(Enrollment::getId));
        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<Long> classIds = enrollments.stream().map(Enrollment::getClassId).distinct().toList();
        Map<Long, ClassInfo> classMap = classInfoMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(ClassInfo::getId, Function.identity()));
        Map<Long, CourseTemplate> templateMap = loadTemplates(classMap.values());
        Map<Long, String> coachNames = loadCoachNames(classMap.values());

        return enrollments.stream().map(enrollment -> {
            ClassInfo classInfo = classMap.get(enrollment.getClassId());
            CourseTemplate template = classInfo == null ? null : templateMap.get(classInfo.getTemplateId());
            return new MyEnrollmentVO(
                    enrollment.getId(),
                    enrollment.getClassId(),
                    classInfo == null ? null : classInfo.getTitle(),
                    template == null ? null : template.getSportType(),
                    classInfo == null ? null : coachNames.get(classInfo.getCoachId()),
                    classInfo == null ? null : classInfo.getStartDate(),
                    classInfo == null ? null : classInfo.getEndDate(),
                    classInfo == null ? null : classInfo.getStartTime(),
                    classInfo == null ? null : classInfo.getEndTime(),
                    classInfo == null ? null : classInfo.getTotalSessions(),
                    classInfo == null ? null : classInfo.getPrice(),
                    enrollment.getStatus(),
                    enrollment.getEnrollTime(),
                    enrollment.getCancelTime());
        }).toList();
    }

    public List<ClassEnrollmentVO> listByClass(Long classId) {
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        List<Enrollment> enrollments = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getClassId, classId)
                .orderByAsc(Enrollment::getId));
        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = enrollments.stream().map(Enrollment::getMemberId).distinct().toList();
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(memberIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return enrollments.stream().map(enrollment -> {
            SysUser user = userMap.get(enrollment.getMemberId());
            return new ClassEnrollmentVO(
                    enrollment.getId(),
                    enrollment.getMemberId(),
                    user == null ? null : user.getUsername(),
                    user == null ? null : user.getNickname(),
                    user == null ? null : user.getPhone(),
                    enrollment.getStatus(),
                    enrollment.getEnrollTime(),
                    enrollment.getCancelTime());
        }).toList();
    }

    public StockVO rebuildStock(Long classId) {
        int remaining = stockService.rebuild(classId);
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        int occupied = Math.max(0, classInfo.getCapacity() - remaining);
        return new StockVO(classId, classInfo.getCapacity(), occupied, remaining);
    }

    private EnrollResultVO toResult(Enrollment enrollment) {
        Integer remaining = null;
        try {
            remaining = stockService.remaining(enrollment.getClassId());
        } catch (Exception e) {
            log.warn("[抢课] 读取余量失败 classId={}：{}", enrollment.getClassId(), e.getMessage());
        }
        return new EnrollResultVO(
                enrollment.getId(),
                enrollment.getClassId(),
                enrollment.getStatus(),
                enrollment.getEnrollTime(),
                remaining);
    }

    private Enrollment findByClassAndMember(Long classId, Long memberId) {
        return enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getClassId, classId)
                .eq(Enrollment::getMemberId, memberId));
    }

    private boolean isOccupying(String status) {
        try {
            return EnrollmentStatus.valueOf(status).isOccupying();
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private Map<Long, CourseTemplate> loadTemplates(Collection<ClassInfo> classes) {
        List<Long> templateIds = classes.stream().map(ClassInfo::getTemplateId).distinct().toList();
        if (templateIds.isEmpty()) {
            return Map.of();
        }
        return courseTemplateMapper.selectBatchIds(templateIds).stream()
                .collect(Collectors.toMap(CourseTemplate::getId, Function.identity()));
    }

    private Map<Long, String> loadCoachNames(Collection<ClassInfo> classes) {
        List<Long> coachIds = classes.stream().map(ClassInfo::getCoachId).distinct().toList();
        if (coachIds.isEmpty()) {
            return Map.of();
        }
        return coachProfileMapper.selectList(Wrappers.<CoachProfile>lambdaQuery()
                        .in(CoachProfile::getUserId, coachIds))
                .stream()
                .collect(Collectors.toMap(CoachProfile::getUserId, CoachProfile::getRealName, (a, b) -> a));
    }
}
