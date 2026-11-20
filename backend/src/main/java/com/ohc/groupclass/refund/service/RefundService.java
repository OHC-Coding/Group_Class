package com.ohc.groupclass.refund.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.checkin.entity.CheckinRecord;
import com.ohc.groupclass.checkin.mapper.CheckinRecordMapper;
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
import com.ohc.groupclass.payment.channel.PaymentChannel;
import com.ohc.groupclass.refund.dto.RefundVO;
import com.ohc.groupclass.refund.entity.RefundOrder;
import com.ohc.groupclass.refund.mapper.RefundOrderMapper;
import com.ohc.groupclass.user.entity.SysUser;
import com.ohc.groupclass.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private static final String REFUND_APPLYING = "APPLYING";
    private static final String REFUND_REJECTED = "REJECTED";
    private static final String REFUND_REFUNDED = "REFUNDED";
    private static final DateTimeFormatter REFUND_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final RefundOrderMapper refundOrderMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final ClassInfoMapper classInfoMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final StockService stockService;
    private final List<PaymentChannel> channels;
    private final TransactionTemplate transactionTemplate;

    /**
     * 退款金额 = 实付金额 − 课次单价 × 已核销课次，单价按总课次均摊、保留两位。
     */
    public RefundVO apply(Long enrollmentId, Long memberId, String reason) {
        Enrollment enrollment = enrollmentMapper.selectById(enrollmentId);
        if (enrollment == null || !Objects.equals(enrollment.getMemberId(), memberId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名不存在");
        }
        if (!EnrollmentStatus.CONFIRMED.name().equals(enrollment.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有已确认的报名可以申请退款");
        }
        ClassInfo classInfo = classInfoMapper.selectById(enrollment.getClassId());
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        if (ClassStatus.FINISHED.name().equals(classInfo.getStatus())
                || ClassStatus.CANCELLED.name().equals(classInfo.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "课程已结束或已取消，无法申请退款");
        }
        boolean activeRefund = refundOrderMapper.exists(Wrappers.<RefundOrder>lambdaQuery()
                .eq(RefundOrder::getEnrollmentId, enrollmentId)
                .in(RefundOrder::getStatus, REFUND_APPLYING, REFUND_REFUNDED));
        if (activeRefund) {
            throw new BusinessException(ResultCode.CONFLICT, "该报名已有退款申请或已退款");
        }

        PaymentOrder paidOrder = paymentOrderMapper.selectOne(Wrappers.<PaymentOrder>lambdaQuery()
                .eq(PaymentOrder::getEnrollmentId, enrollmentId)
                .eq(PaymentOrder::getStatus, "PAID")
                .orderByDesc(PaymentOrder::getId)
                .last("LIMIT 1"));
        if (paidOrder == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "没有可退款的支付记录");
        }

        int totalSessions = classInfo.getTotalSessions();
        long checked = checkinRecordMapper.selectCount(Wrappers.<CheckinRecord>lambdaQuery()
                .eq(CheckinRecord::getEnrollmentId, enrollmentId));
        if (checked >= totalSessions) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "课时已全部核销，无法退款");
        }
        BigDecimal unitPrice = paidOrder.getAmount()
                .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
        BigDecimal refundAmount = paidOrder.getAmount()
                .subtract(unitPrice.multiply(BigDecimal.valueOf(checked)))
                .setScale(2, RoundingMode.HALF_UP);
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "可退金额为 0，无法退款");
        }

        RefundOrder refund = new RefundOrder();
        refund.setRefundNo(nextRefundNo());
        refund.setEnrollmentId(enrollmentId);
        refund.setOrderId(paidOrder.getId());
        refund.setAmount(refundAmount);
        refund.setTotalSessions(totalSessions);
        refund.setCheckedSessions((int) checked);
        refund.setReason(reason);
        refund.setStatus(REFUND_APPLYING);
        refundOrderMapper.insert(refund);
        return toVOs(List.of(refund)).get(0);
    }

    /**
     * 审批通过：先走渠道退款，再在同一事务里把退款单、报名、支付单一起改掉。
     */
    public RefundVO approve(Long refundId) {
        RefundOrder refund = requireRefund(refundId);
        if (!REFUND_APPLYING.equals(refund.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "退款单状态已变化");
        }
        PaymentOrder order = paymentOrderMapper.selectById(refund.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付单不存在");
        }
        PaymentChannel channel = resolveChannel(order.getChannel());
        PaymentChannel.RefundResult result = channel.refund(order, refund.getAmount());
        if (!result.success()) {
            throw new BusinessException(ResultCode.ERROR, "渠道退款失败，请稍后重试");
        }

        Enrollment enrollment = enrollmentMapper.selectById(refund.getEnrollmentId());
        transactionTemplate.executeWithoutResult(status -> {
            int updated = refundOrderMapper.update(null, Wrappers.<RefundOrder>lambdaUpdate()
                    .eq(RefundOrder::getId, refundId)
                    .eq(RefundOrder::getStatus, REFUND_APPLYING)
                    .set(RefundOrder::getStatus, REFUND_REFUNDED)
                    .set(RefundOrder::getRefundTime, LocalDateTime.now()));
            if (updated == 0) {
                throw new BusinessException(ResultCode.CONFLICT, "退款单状态已变化");
            }
            int enrollmentUpdated = enrollmentMapper.update(null, Wrappers.<Enrollment>lambdaUpdate()
                    .eq(Enrollment::getId, refund.getEnrollmentId())
                    .eq(Enrollment::getStatus, EnrollmentStatus.CONFIRMED.name())
                    .set(Enrollment::getStatus, EnrollmentStatus.REFUNDED.name()));
            if (enrollmentUpdated == 0) {
                throw new BusinessException(ResultCode.CONFLICT, "报名状态已变化");
            }
            paymentOrderMapper.update(null, Wrappers.<PaymentOrder>lambdaUpdate()
                    .eq(PaymentOrder::getId, order.getId())
                    .eq(PaymentOrder::getStatus, "PAID")
                    .set(PaymentOrder::getStatus, "REFUNDED"));
        });

        ClassInfo classInfo = enrollment == null ? null : classInfoMapper.selectById(enrollment.getClassId());
        if (classInfo != null && enrollment != null
                && ClassStatus.OPEN.name().equals(classInfo.getStatus())) {
            stockService.release(classInfo.getId(), enrollment.getMemberId());
        }
        log.info("[退款] 单号 {} 金额 {} 已退款", refund.getRefundNo(), refund.getAmount());
        return toVOs(List.of(requireRefund(refundId))).get(0);
    }

    public RefundVO reject(Long refundId, String reason) {
        int updated = refundOrderMapper.update(null, Wrappers.<RefundOrder>lambdaUpdate()
                .eq(RefundOrder::getId, refundId)
                .eq(RefundOrder::getStatus, REFUND_APPLYING)
                .set(RefundOrder::getStatus, REFUND_REJECTED)
                .set(RefundOrder::getAuditRemark, reason));
        if (updated == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "退款单不存在或状态已变化");
        }
        return toVOs(List.of(requireRefund(refundId))).get(0);
    }

    public List<RefundVO> listMine(Long memberId) {
        List<Long> enrollmentIds = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                        .eq(Enrollment::getMemberId, memberId))
                .stream().map(Enrollment::getId).toList();
        if (enrollmentIds.isEmpty()) {
            return List.of();
        }
        return toVOs(refundOrderMapper.selectList(Wrappers.<RefundOrder>lambdaQuery()
                .in(RefundOrder::getEnrollmentId, enrollmentIds)
                .orderByDesc(RefundOrder::getId)));
    }

    public List<RefundVO> listByStatus(String status) {
        List<RefundOrder> refunds = refundOrderMapper.selectList(Wrappers.<RefundOrder>lambdaQuery()
                .eq(status != null && !status.isBlank(), RefundOrder::getStatus, status)
                .orderByDesc(RefundOrder::getId));
        return toVOs(refunds);
    }

    private RefundOrder requireRefund(Long refundId) {
        RefundOrder refund = refundOrderMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款单不存在");
        }
        return refund;
    }

    private PaymentChannel resolveChannel(String code) {
        return channels.stream()
                .filter(channel -> channel.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "不支持的支付渠道"));
    }

    private List<RefundVO> toVOs(List<RefundOrder> refunds) {
        if (refunds.isEmpty()) {
            return List.of();
        }
        List<Long> enrollmentIds = refunds.stream().map(RefundOrder::getEnrollmentId).distinct().toList();
        Map<Long, Enrollment> enrollmentMap = enrollmentMapper.selectBatchIds(enrollmentIds).stream()
                .collect(Collectors.toMap(Enrollment::getId, Function.identity(), (a, b) -> a));
        Set<Long> classIds = new HashSet<>();
        Set<Long> memberIds = new HashSet<>();
        enrollmentMap.values().forEach(enrollment -> {
            classIds.add(enrollment.getClassId());
            memberIds.add(enrollment.getMemberId());
        });
        Map<Long, ClassInfo> classMap = classIds.isEmpty() ? Map.of()
                : classInfoMapper.selectBatchIds(classIds).stream()
                        .collect(Collectors.toMap(ClassInfo::getId, Function.identity()));
        Map<Long, SysUser> userMap = memberIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(memberIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        List<RefundVO> result = new ArrayList<>(refunds.size());
        for (RefundOrder refund : refunds) {
            Enrollment enrollment = enrollmentMap.get(refund.getEnrollmentId());
            ClassInfo classInfo = enrollment == null ? null : classMap.get(enrollment.getClassId());
            SysUser user = enrollment == null ? null : userMap.get(enrollment.getMemberId());
            result.add(new RefundVO(
                    refund.getId(),
                    refund.getRefundNo(),
                    refund.getEnrollmentId(),
                    classInfo == null ? null : classInfo.getId(),
                    classInfo == null ? null : classInfo.getTitle(),
                    enrollment == null ? null : enrollment.getMemberId(),
                    user == null ? null : user.getNickname(),
                    refund.getAmount(),
                    refund.getTotalSessions(),
                    refund.getCheckedSessions(),
                    refund.getReason(),
                    refund.getStatus(),
                    refund.getAuditRemark(),
                    refund.getRefundTime(),
                    refund.getCreatedAt()));
        }
        return result;
    }

    private String nextRefundNo() {
        return "R" + LocalDateTime.now().format(REFUND_NO_FORMAT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
