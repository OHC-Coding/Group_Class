package com.ohc.groupclass.payment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.enrollment.entity.Enrollment;
import com.ohc.groupclass.enrollment.enums.EnrollmentStatus;
import com.ohc.groupclass.enrollment.mapper.EnrollmentMapper;
import com.ohc.groupclass.order.entity.PaymentOrder;
import com.ohc.groupclass.order.mapper.PaymentOrderMapper;
import com.ohc.groupclass.payment.channel.PaymentChannel;
import com.ohc.groupclass.payment.dto.CallbackResultVO;
import com.ohc.groupclass.payment.dto.PaymentNotification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCallbackService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackService.class);

    private static final String STATUS_PAYING = "PAYING";
    private static final String STATUS_PAID = "PAID";
    private static final String NOTIFY_SUCCESS = "SUCCESS";

    private final List<PaymentChannel> channels;
    private final ObjectMapper objectMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 回调处理顺序：验签 → 校验渠道与金额 → 条件更新订单 → 同一事务确认报名。
     * 订单状态是入账的唯一门闩，重复回调影响 0 行，返回 duplicate 而不是再确认一次。
     */
    public CallbackResultVO handle(String channelCode, String rawBody, String signature) {
        PaymentChannel channel = resolve(channelCode);
        if (!channel.verify(rawBody, signature)) {
            log.warn("[支付回调] 验签失败 channel={}", channelCode);
            throw new BusinessException(ResultCode.BAD_REQUEST, "回调验签失败");
        }
        PaymentNotification notification = parse(rawBody);
        if (!channelCode.equalsIgnoreCase(notification.getChannel())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "回调渠道与请求路径不一致");
        }
        if (!NOTIFY_SUCCESS.equalsIgnoreCase(notification.getStatus())) {
            log.info("[支付回调] 非成功状态，忽略 notifyNo={} status={}",
                    notification.getNotifyNo(), notification.getStatus());
            return new CallbackResultVO(false, false, null, "非支付成功回调，已忽略");
        }

        PaymentOrder order = paymentOrderMapper.selectOne(Wrappers.<PaymentOrder>lambdaQuery()
                .eq(PaymentOrder::getOrderNo, notification.getOrderNo()));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getAmount().compareTo(notification.getAmount()) != 0) {
            log.warn("[支付回调] 金额不一致 orderNo={} 订单={} 回调={}",
                    order.getOrderNo(), order.getAmount(), notification.getAmount());
            throw new BusinessException(ResultCode.BAD_REQUEST, "回调金额与订单金额不一致");
        }
        return transactionTemplate.execute(status -> apply(order, notification));
    }

    private CallbackResultVO apply(PaymentOrder order, PaymentNotification notification) {
        int paid = paymentOrderMapper.update(null, Wrappers.<PaymentOrder>lambdaUpdate()
                .eq(PaymentOrder::getId, order.getId())
                .eq(PaymentOrder::getStatus, STATUS_PAYING)
                .set(PaymentOrder::getStatus, STATUS_PAID)
                .set(PaymentOrder::getPayTime, LocalDateTime.now())
                .set(PaymentOrder::getNotifyNo, notification.getNotifyNo()));

        if (paid == 1) {
            int confirmed = enrollmentMapper.update(null, Wrappers.<Enrollment>lambdaUpdate()
                    .eq(Enrollment::getId, order.getEnrollmentId())
                    .eq(Enrollment::getStatus, EnrollmentStatus.PENDING_PAYMENT.name())
                    .set(Enrollment::getStatus, EnrollmentStatus.CONFIRMED.name()));
            if (confirmed == 0) {
                throw new BusinessException(ResultCode.CONFLICT, "报名状态已变化，支付无法入账");
            }
            log.info("[支付回调] 入账成功 orderNo={} enrollmentId={} notifyNo={}",
                    order.getOrderNo(), order.getEnrollmentId(), notification.getNotifyNo());
            return new CallbackResultVO(true, false, STATUS_PAID, "支付成功");
        }

        PaymentOrder latest = paymentOrderMapper.selectById(order.getId());
        if (STATUS_PAID.equals(latest.getStatus())) {
            log.info("[支付回调] 重复回调已忽略 orderNo={} notifyNo={}",
                    order.getOrderNo(), notification.getNotifyNo());
            return new CallbackResultVO(true, true, STATUS_PAID, "重复回调，已忽略");
        }
        log.warn("[支付回调] 订单已关闭，未入账 orderNo={} status={}",
                order.getOrderNo(), latest.getStatus());
        return new CallbackResultVO(false, false, latest.getStatus(), "订单已关闭，未入账");
    }

    private PaymentNotification parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, PaymentNotification.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "回调报文格式错误");
        }
    }

    private PaymentChannel resolve(String channelCode) {
        return channels.stream()
                .filter(channel -> channel.code().equalsIgnoreCase(channelCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "不支持的支付渠道"));
    }
}
