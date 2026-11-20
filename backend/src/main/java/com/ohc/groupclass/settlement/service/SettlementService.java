package com.ohc.groupclass.settlement.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.coach.entity.CoachProfile;
import com.ohc.groupclass.coach.mapper.CoachProfileMapper;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.entity.ClassSession;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.course.mapper.ClassSessionMapper;
import com.ohc.groupclass.settlement.dto.SettlementVO;
import com.ohc.groupclass.settlement.entity.SettlementBill;
import com.ohc.groupclass.settlement.mapper.SettlementBillMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private static final String SESSION_FINISHED = "FINISHED";
    private static final DateTimeFormatter BILL_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final SettlementBillMapper settlementBillMapper;
    private final ClassSessionMapper classSessionMapper;
    private final ClassInfoMapper classInfoMapper;
    private final CoachProfileMapper coachProfileMapper;

    /**
     * 按周期生成教练结算单：统计周期内已完结的课次，按课次的代课教练（没有则用班级教练）分组。
     * 同一教练同一周期唯一，重复生成直接跳过。
     */
    public List<SettlementVO> generate(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null || periodStart.isAfter(periodEnd)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结算周期不合法");
        }
        List<ClassSession> sessions = classSessionMapper.selectList(Wrappers.<ClassSession>lambdaQuery()
                .eq(ClassSession::getStatus, SESSION_FINISHED)
                .ge(ClassSession::getStartTime, periodStart.atStartOfDay())
                .lt(ClassSession::getStartTime, periodEnd.plusDays(1).atStartOfDay()));
        if (sessions.isEmpty()) {
            return List.of();
        }

        Set<Long> classIds = sessions.stream().map(ClassSession::getClassId).collect(Collectors.toSet());
        Map<Long, ClassInfo> classMap = classInfoMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(ClassInfo::getId, Function.identity()));

        Map<Long, CoachAccumulator> accumulatorMap = new HashMap<>();
        for (ClassSession session : sessions) {
            ClassInfo classInfo = classMap.get(session.getClassId());
            if (classInfo == null) {
                continue;
            }
            Long coachId = session.getCoachId() == null ? classInfo.getCoachId() : session.getCoachId();
            if (coachId == null) {
                continue;
            }
            BigDecimal price = classInfo.getCoachUnitPrice() == null
                    ? BigDecimal.ZERO : classInfo.getCoachUnitPrice();
            accumulatorMap.computeIfAbsent(coachId, key -> new CoachAccumulator())
                    .add(price);
        }

        List<SettlementBill> created = new ArrayList<>();
        for (Map.Entry<Long, CoachAccumulator> entry : accumulatorMap.entrySet()) {
            CoachAccumulator accumulator = entry.getValue();
            SettlementBill bill = new SettlementBill();
            bill.setBillNo(nextBillNo());
            bill.setCoachId(entry.getKey());
            bill.setPeriodStart(periodStart);
            bill.setPeriodEnd(periodEnd);
            bill.setSessionCount(accumulator.count);
            bill.setAmount(accumulator.amount.setScale(2, RoundingMode.HALF_UP));
            bill.setUnitPrice(accumulator.amount
                    .divide(BigDecimal.valueOf(accumulator.count), 2, RoundingMode.HALF_UP));
            bill.setStatus("PENDING");
            try {
                settlementBillMapper.insert(bill);
                created.add(bill);
            } catch (DuplicateKeyException e) {
                log.info("[结算] 教练 {} 周期 {}~{} 已存在结算单，跳过",
                        entry.getKey(), periodStart, periodEnd);
            }
        }
        log.info("[结算] 周期 {}~{} 生成 {} 张结算单", periodStart, periodEnd, created.size());
        return toVOs(created);
    }

    public List<SettlementVO> list(Long coachId, LocalDate periodStart, LocalDate periodEnd) {
        return toVOs(settlementBillMapper.selectList(Wrappers.<SettlementBill>lambdaQuery()
                .eq(coachId != null, SettlementBill::getCoachId, coachId)
                .ge(periodStart != null, SettlementBill::getPeriodStart, periodStart)
                .le(periodEnd != null, SettlementBill::getPeriodEnd, periodEnd)
                .orderByDesc(SettlementBill::getId)));
    }

    public SettlementVO confirm(Long billId) {
        int updated = settlementBillMapper.update(null, Wrappers.<SettlementBill>lambdaUpdate()
                .eq(SettlementBill::getId, billId)
                .eq(SettlementBill::getStatus, "PENDING")
                .set(SettlementBill::getStatus, "CONFIRMED"));
        if (updated == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "结算单不存在或已确认");
        }
        SettlementBill bill = settlementBillMapper.selectById(billId);
        return toVOs(List.of(bill)).get(0);
    }

    private List<SettlementVO> toVOs(List<SettlementBill> bills) {
        if (bills.isEmpty()) {
            return List.of();
        }
        Set<Long> coachIds = bills.stream().map(SettlementBill::getCoachId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> coachNames = coachIds.isEmpty() ? Map.of()
                : coachProfileMapper.selectList(Wrappers.<CoachProfile>lambdaQuery()
                        .in(CoachProfile::getUserId, coachIds))
                .stream().collect(Collectors.toMap(CoachProfile::getUserId,
                        CoachProfile::getRealName, (a, b) -> a));
        return bills.stream().map(bill -> new SettlementVO(
                bill.getId(),
                bill.getBillNo(),
                bill.getCoachId(),
                coachNames.get(bill.getCoachId()),
                bill.getPeriodStart(),
                bill.getPeriodEnd(),
                bill.getSessionCount(),
                bill.getUnitPrice(),
                bill.getAmount(),
                bill.getStatus(),
                bill.getCreatedAt())).toList();
    }

    private String nextBillNo() {
        return "S" + LocalDateTime.now().format(BILL_NO_FORMAT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private static final class CoachAccumulator {

        private int count;

        private BigDecimal amount = BigDecimal.ZERO;

        private void add(BigDecimal price) {
            count++;
            amount = amount.add(price);
        }
    }
}
