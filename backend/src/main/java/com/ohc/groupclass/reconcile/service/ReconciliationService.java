package com.ohc.groupclass.reconcile.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.enums.ClassStatus;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.enrollment.entity.Enrollment;
import com.ohc.groupclass.enrollment.enums.EnrollmentStatus;
import com.ohc.groupclass.enrollment.mapper.EnrollmentMapper;
import com.ohc.groupclass.enrollment.service.StockService;
import com.ohc.groupclass.order.entity.PaymentOrder;
import com.ohc.groupclass.order.mapper.PaymentOrderMapper;
import com.ohc.groupclass.reconcile.dto.ReconciliationVO;
import com.ohc.groupclass.reconcile.entity.ReconciliationRecord;
import com.ohc.groupclass.reconcile.mapper.ReconciliationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private static final int DETAIL_LIMIT = 1000;

    private final ReconciliationRecordMapper reconciliationRecordMapper;
    private final ClassInfoMapper classInfoMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final StockService stockService;

    /**
     * 以数据库为准核对 Redis：逐个班级比较余量，有偏差就重建，最后落一条当天的对账记录。
     */
    public ReconciliationVO run(LocalDate bizDate) {
        if (bizDate == null) {
            bizDate = LocalDate.now();
        }
        List<ClassInfo> classes = classInfoMapper.selectList(Wrappers.<ClassInfo>lambdaQuery()
                .in(ClassInfo::getStatus, ClassStatus.OPEN.name(), ClassStatus.CLOSED.name()));
        int diff = 0;
        int redisRemainTotal = 0;
        StringBuilder detail = new StringBuilder();
        try {
            for (ClassInfo classInfo : classes) {
                Integer before = stockService.remaining(classInfo.getId());
                long occupied = enrollmentMapper.countOccupied(classInfo.getId());
                int dbRemain = (int) Math.max(0, classInfo.getCapacity() - occupied);
                if (before == null || before != dbRemain) {
                    diff++;
                    if (detail.length() < DETAIL_LIMIT) {
                        detail.append(classInfo.getId()).append(':')
                                .append(before == null ? "null" : before)
                                .append("->").append(dbRemain).append(' ');
                    }
                }
                redisRemainTotal += stockService.rebuild(classInfo.getId());
            }
        } catch (Exception e) {
            log.error("[对账] 执行失败 bizDate={}", bizDate, e);
            save(bizDate, 0, 0, 0, 0, diff, "FAIL",
                    trim("执行失败：" + e.getMessage() + " " + detail));
            throw new BusinessException(ResultCode.ERROR, "对账执行失败：" + e.getMessage());
        }

        long enrollCount = countEnrollments(bizDate);
        long paidCount = countPaid(bizDate);
        long occupiedTotal = enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .in(Enrollment::getStatus, EnrollmentStatus.PENDING_PAYMENT.name(),
                        EnrollmentStatus.CONFIRMED.name()));
        ReconciliationRecord record = save(bizDate,
                (int) enrollCount, (int) paidCount, (int) occupiedTotal,
                redisRemainTotal, diff, diff == 0 ? "PASS" : "REPAIRED", trim(detail.toString()));
        if (diff > 0) {
            log.warn("[对账] {} 修复 {} 个班级的库存偏差：{}", bizDate, diff, detail);
        } else {
            log.info("[对账] {} 全部一致，共 {} 个班级", bizDate, classes.size());
        }
        return toVO(record);
    }

    public List<ReconciliationVO> list(int limit) {
        return reconciliationRecordMapper.selectList(Wrappers.<ReconciliationRecord>lambdaQuery()
                        .orderByDesc(ReconciliationRecord::getBizDate)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 100))))
                .stream().map(this::toVO).toList();
    }

    private long countEnrollments(LocalDate bizDate) {
        LocalDateTime start = bizDate.atStartOfDay();
        return enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .ge(Enrollment::getCreatedAt, start)
                .lt(Enrollment::getCreatedAt, start.plusDays(1)));
    }

    private long countPaid(LocalDate bizDate) {
        LocalDateTime start = bizDate.atStartOfDay();
        return paymentOrderMapper.selectCount(Wrappers.<PaymentOrder>lambdaQuery()
                .eq(PaymentOrder::getStatus, "PAID")
                .ge(PaymentOrder::getPayTime, start)
                .lt(PaymentOrder::getPayTime, start.plusDays(1)));
    }

    private ReconciliationRecord save(LocalDate bizDate, int enrollCount, int paidCount,
                                      int occupiedCount, int redisRemain, int diff,
                                      String result, String detail) {
        ReconciliationRecord existing = reconciliationRecordMapper.selectOne(
                Wrappers.<ReconciliationRecord>lambdaQuery()
                        .eq(ReconciliationRecord::getBizDate, bizDate));
        if (existing == null) {
            ReconciliationRecord record = new ReconciliationRecord();
            record.setBizDate(bizDate);
            record.setEnrollCount(enrollCount);
            record.setPaidCount(paidCount);
            record.setOccupiedCount(occupiedCount);
            record.setRedisRemain(redisRemain);
            record.setDiffCount(diff);
            record.setResult(result);
            record.setDetail(detail);
            reconciliationRecordMapper.insert(record);
            return record;
        }
        existing.setEnrollCount(enrollCount);
        existing.setPaidCount(paidCount);
        existing.setOccupiedCount(occupiedCount);
        existing.setRedisRemain(redisRemain);
        existing.setDiffCount(diff);
        existing.setResult(result);
        existing.setDetail(detail);
        reconciliationRecordMapper.updateById(existing);
        return existing;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= DETAIL_LIMIT ? value : value.substring(0, DETAIL_LIMIT);
    }

    private ReconciliationVO toVO(ReconciliationRecord record) {
        return new ReconciliationVO(
                record.getId(),
                record.getBizDate(),
                record.getEnrollCount(),
                record.getPaidCount(),
                record.getOccupiedCount(),
                record.getRedisRemain(),
                record.getDiffCount(),
                record.getResult(),
                record.getDetail(),
                record.getCreatedAt());
    }
}
