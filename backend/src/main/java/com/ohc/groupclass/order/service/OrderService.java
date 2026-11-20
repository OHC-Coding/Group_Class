package com.ohc.groupclass.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.entity.ClassInfo;
import com.ohc.groupclass.course.mapper.ClassInfoMapper;
import com.ohc.groupclass.enrollment.entity.Enrollment;
import com.ohc.groupclass.enrollment.mapper.EnrollmentMapper;
import com.ohc.groupclass.order.dto.OrderVO;
import com.ohc.groupclass.order.entity.PaymentOrder;
import com.ohc.groupclass.order.mapper.PaymentOrderMapper;
import com.ohc.groupclass.user.entity.SysUser;
import com.ohc.groupclass.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String STATUS_PAYING = "PAYING";
    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PaymentOrderMapper paymentOrderMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final ClassInfoMapper classInfoMapper;
    private final SysUserMapper sysUserMapper;

    @Value("${group-class.enroll.pay-timeout-minutes:15}")
    private int payTimeoutMinutes;

    /**
     * 报名落库后紧接着生成支付单，由外层同一个事务包住，保证报名与订单同时存在。
     */
    @Transactional
    public PaymentOrder create(Long enrollmentId, Long memberId, BigDecimal amount) {
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(nextOrderNo());
        order.setEnrollmentId(enrollmentId);
        order.setMemberId(memberId);
        order.setAmount(amount);
        order.setChannel("MOCK");
        order.setStatus(STATUS_PAYING);
        order.setExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        paymentOrderMapper.insert(order);
        return order;
    }

    /**
     * 关单：只把 PAYING 改成 CLOSED，返回受影响行数，调用方靠它判断是否只有自己关成功。
     */
    public int closeByEnrollment(Long enrollmentId) {
        return paymentOrderMapper.update(null, Wrappers.<PaymentOrder>lambdaUpdate()
                .eq(PaymentOrder::getEnrollmentId, enrollmentId)
                .eq(PaymentOrder::getStatus, STATUS_PAYING)
                .set(PaymentOrder::getStatus, "CLOSED"));
    }

    public int closeExpired(Long orderId, LocalDateTime now) {
        return paymentOrderMapper.update(null, Wrappers.<PaymentOrder>lambdaUpdate()
                .eq(PaymentOrder::getId, orderId)
                .eq(PaymentOrder::getStatus, STATUS_PAYING)
                .le(PaymentOrder::getExpireTime, now)
                .set(PaymentOrder::getStatus, "CLOSED"));
    }

    public PaymentOrder requireById(Long orderId) {
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    public boolean isPaidByEnrollment(Long enrollmentId) {
        return paymentOrderMapper.exists(Wrappers.<PaymentOrder>lambdaQuery()
                .eq(PaymentOrder::getEnrollmentId, enrollmentId)
                .eq(PaymentOrder::getStatus, "PAID"));
    }

    public List<PaymentOrder> findExpired(LocalDateTime now, int limit) {
        return paymentOrderMapper.selectExpired(now, limit);
    }

    public List<OrderVO> listMine(Long memberId) {
        List<PaymentOrder> orders = paymentOrderMapper.selectList(Wrappers.<PaymentOrder>lambdaQuery()
                .eq(PaymentOrder::getMemberId, memberId)
                .orderByDesc(PaymentOrder::getId));
        return toVOs(orders, false);
    }

    public OrderVO getById(Long orderId, Long memberId) {
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getMemberId(), memberId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return toVOs(List.of(order), false).get(0);
    }

    public List<OrderVO> listByClass(Long classId) {
        List<Long> enrollmentIds = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                        .eq(Enrollment::getClassId, classId))
                .stream().map(Enrollment::getId).toList();
        if (enrollmentIds.isEmpty()) {
            return List.of();
        }
        List<PaymentOrder> orders = paymentOrderMapper.selectList(Wrappers.<PaymentOrder>lambdaQuery()
                .in(PaymentOrder::getEnrollmentId, enrollmentIds)
                .orderByDesc(PaymentOrder::getId));
        return toVOs(orders, true);
    }

    private List<OrderVO> toVOs(List<PaymentOrder> orders, boolean withMember) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> enrollmentIds = orders.stream().map(PaymentOrder::getEnrollmentId).distinct().toList();
        Map<Long, Long> classIdByEnrollment = enrollmentMapper.selectBatchIds(enrollmentIds).stream()
                .collect(Collectors.toMap(Enrollment::getId, Enrollment::getClassId, (a, b) -> a));
        Set<Long> classIds = new HashSet<>(classIdByEnrollment.values());
        Map<Long, String> classTitles = classIds.isEmpty() ? Map.of()
                : classInfoMapper.selectBatchIds(classIds).stream()
                        .collect(Collectors.toMap(ClassInfo::getId, ClassInfo::getTitle, (a, b) -> a));

        Map<Long, SysUser> userMap = Map.of();
        if (withMember) {
            List<Long> memberIds = orders.stream().map(PaymentOrder::getMemberId).distinct().toList();
            userMap = sysUserMapper.selectBatchIds(memberIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        }

        Map<Long, SysUser> finalUserMap = userMap;
        List<OrderVO> result = new ArrayList<>(orders.size());
        for (PaymentOrder order : orders) {
            Long classId = classIdByEnrollment.get(order.getEnrollmentId());
            SysUser user = finalUserMap.get(order.getMemberId());
            result.add(new OrderVO(
                    order.getId(),
                    order.getOrderNo(),
                    classId,
                    classId == null ? null : classTitles.get(classId),
                    order.getMemberId(),
                    user == null ? null : user.getNickname(),
                    user == null ? null : user.getPhone(),
                    order.getAmount(),
                    order.getChannel(),
                    order.getStatus(),
                    order.getCreatedAt(),
                    order.getExpireTime(),
                    order.getPayTime()));
        }
        return result;
    }

    private String nextOrderNo() {
        return "O" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
