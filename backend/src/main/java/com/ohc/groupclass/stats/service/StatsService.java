package com.ohc.groupclass.stats.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.checkin.entity.CheckinRecord;
import com.ohc.groupclass.checkin.mapper.CheckinRecordMapper;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.entity.ClassSession;
import com.ohc.groupclass.course.enums.ClassStatus;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.course.mapper.ClassSessionMapper;
import com.ohc.groupclass.enrollment.entity.Enrollment;
import com.ohc.groupclass.enrollment.enums.EnrollmentStatus;
import com.ohc.groupclass.enrollment.mapper.EnrollmentMapper;
import com.ohc.groupclass.order.entity.PaymentOrder;
import com.ohc.groupclass.order.mapper.PaymentOrderMapper;
import com.ohc.groupclass.refund.entity.RefundOrder;
import com.ohc.groupclass.refund.mapper.RefundOrderMapper;
import com.ohc.groupclass.stats.dto.ClassStatsVO;
import com.ohc.groupclass.stats.dto.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final String SESSION_FINISHED = "FINISHED";
    private static final String SESSION_CANCELLED = "CANCELLED";

    private final ClassInfoMapper classInfoMapper;
    private final ClassSessionMapper classSessionMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final RefundOrderMapper refundOrderMapper;

    public DashboardVO dashboard() {
        List<ClassInfo> classes = classInfoMapper.selectList(Wrappers.<ClassInfo>lambdaQuery()
                .in(ClassInfo::getStatus, ClassStatus.OPEN.name(), ClassStatus.CLOSED.name(),
                        ClassStatus.FINISHED.name()));
        Set<Long> classIds = classes.stream().map(ClassInfo::getId).collect(Collectors.toSet());

        int capacityTotal = classes.stream()
                .mapToInt(item -> item.getCapacity() == null ? 0 : item.getCapacity()).sum();
        long confirmed = countConfirmed(classIds);

        List<ClassSession> finishedSessions = classSessionMapper.selectList(
                Wrappers.<ClassSession>lambdaQuery().eq(ClassSession::getStatus, SESSION_FINISHED));
        List<Long> finishedIds = finishedSessions.stream().map(ClassSession::getId).toList();
        long checkedAttendance = finishedIds.isEmpty() ? 0
                : checkinRecordMapper.selectCount(Wrappers.<CheckinRecord>lambdaQuery()
                        .in(CheckinRecord::getSessionId, finishedIds));
        long expectedAttendance = expectedAttendance(finishedSessions, classIds);

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        long todaySessions = classSessionMapper.selectCount(Wrappers.<ClassSession>lambdaQuery()
                .ge(ClassSession::getStartTime, dayStart)
                .lt(ClassSession::getStartTime, dayEnd)
                .ne(ClassSession::getStatus, SESSION_CANCELLED));
        long todayChecked = checkinRecordMapper.selectCount(Wrappers.<CheckinRecord>lambdaQuery()
                .ge(CheckinRecord::getCheckinTime, dayStart)
                .lt(CheckinRecord::getCheckinTime, dayEnd));

        BigDecimal paidAmount = sumPaidAmount();
        BigDecimal refundedAmount = sumRefundedAmount();
        long pendingRefunds = refundOrderMapper.selectCount(Wrappers.<RefundOrder>lambdaQuery()
                .eq(RefundOrder::getStatus, "APPLYING"));

        BigDecimal attendanceRate = rate(checkedAttendance, expectedAttendance);
        BigDecimal noShowRate = expectedAttendance == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ONE.subtract(attendanceRate).setScale(2, RoundingMode.HALF_UP);

        return new DashboardVO(
                classes.size(),
                (int) classes.stream()
                        .filter(item -> ClassStatus.OPEN.name().equals(item.getStatus())).count(),
                capacityTotal,
                confirmed,
                rate(confirmed, capacityTotal),
                paidAmount,
                refundedAmount,
                paidAmount.subtract(refundedAmount),
                pendingRefunds,
                todaySessions,
                todayChecked,
                finishedSessions.size(),
                expectedAttendance,
                checkedAttendance,
                attendanceRate,
                noShowRate);
    }

    public ClassStatsVO classStats(Long classId) {
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        long confirmed = enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getClassId, classId)
                .eq(Enrollment::getStatus, EnrollmentStatus.CONFIRMED.name()));
        List<ClassSession> finishedSessions = classSessionMapper.selectList(
                Wrappers.<ClassSession>lambdaQuery()
                        .eq(ClassSession::getClassId, classId)
                        .eq(ClassSession::getStatus, SESSION_FINISHED));
        List<Long> finishedIds = finishedSessions.stream().map(ClassSession::getId).toList();
        long checked = finishedIds.isEmpty() ? 0
                : checkinRecordMapper.selectCount(Wrappers.<CheckinRecord>lambdaQuery()
                        .in(CheckinRecord::getSessionId, finishedIds));
        long expected = (long) finishedSessions.size() * confirmed;

        List<Long> enrollmentIds = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                        .eq(Enrollment::getClassId, classId))
                .stream().map(Enrollment::getId).toList();
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        if (!enrollmentIds.isEmpty()) {
            paid = paymentOrderMapper.selectList(Wrappers.<PaymentOrder>lambdaQuery()
                            .in(PaymentOrder::getEnrollmentId, enrollmentIds)
                            .eq(PaymentOrder::getStatus, "PAID"))
                    .stream().map(PaymentOrder::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            refunded = refundOrderMapper.selectList(Wrappers.<RefundOrder>lambdaQuery()
                            .in(RefundOrder::getEnrollmentId, enrollmentIds)
                            .eq(RefundOrder::getStatus, "REFUNDED"))
                    .stream().map(RefundOrder::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal attendance = rate(checked, expected);
        BigDecimal noShow = expected == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ONE.subtract(attendance).setScale(2, RoundingMode.HALF_UP);
        return new ClassStatsVO(
                classInfo.getId(),
                classInfo.getTitle(),
                classInfo.getStatus(),
                classInfo.getCapacity(),
                confirmed,
                rate(confirmed, classInfo.getCapacity() == null ? 0 : classInfo.getCapacity()),
                finishedSessions.size(),
                expected,
                checked,
                attendance,
                noShow,
                paid,
                refunded);
    }

    /**
     * 已完结课次的应到人数：按课次所属班级当前的已确认报名数估算。
     * 退款、换班会让这个值比历史真实值略低，论文里按「当前有效报名」口径说明。
     */
    private long expectedAttendance(List<ClassSession> finishedSessions, Set<Long> classIds) {
        if (finishedSessions.isEmpty()) {
            return 0;
        }
        Map<Long, Long> confirmedByClass = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                        .in(Enrollment::getClassId, classIds)
                        .eq(Enrollment::getStatus, EnrollmentStatus.CONFIRMED.name()))
                .stream().collect(Collectors.groupingBy(Enrollment::getClassId, Collectors.counting()));
        long expected = 0;
        for (ClassSession session : finishedSessions) {
            expected += confirmedByClass.getOrDefault(session.getClassId(), 0L);
        }
        return expected;
    }

    private long countConfirmed(Set<Long> classIds) {
        if (classIds.isEmpty()) {
            return 0;
        }
        return enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .in(Enrollment::getClassId, classIds)
                .eq(Enrollment::getStatus, EnrollmentStatus.CONFIRMED.name()));
    }

    private BigDecimal sumPaidAmount() {
        return paymentOrderMapper.selectList(Wrappers.<PaymentOrder>lambdaQuery()
                        .eq(PaymentOrder::getStatus, "PAID"))
                .stream().map(PaymentOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumRefundedAmount() {
        return refundOrderMapper.selectList(Wrappers.<RefundOrder>lambdaQuery()
                        .eq(RefundOrder::getStatus, "REFUNDED"))
                .stream().map(RefundOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
