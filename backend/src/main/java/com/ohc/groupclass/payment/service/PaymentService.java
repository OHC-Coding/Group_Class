package com.ohc.groupclass.payment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.enrollment.service.EnrollmentService;
import com.ohc.groupclass.order.entity.PaymentOrder;
import com.ohc.groupclass.order.enums.OrderStatus;
import com.ohc.groupclass.order.mapper.PaymentOrderMapper;
import com.ohc.groupclass.order.service.OrderService;
import com.ohc.groupclass.payment.channel.MockPaymentChannel;
import com.ohc.groupclass.payment.dto.CallbackResultVO;
import com.ohc.groupclass.payment.dto.PaymentStatusVO;
import com.ohc.groupclass.payment.dto.PrepayVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final OrderService orderService;
    private final EnrollmentService enrollmentService;
    private final MockPaymentChannel mockPaymentChannel;
    private final PaymentCallbackService paymentCallbackService;

    public PrepayVO prepay(Long orderId, Long memberId) {
        PaymentOrder order = requireOwnedOrder(orderId, memberId);
        if (!OrderStatus.PAYING.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    OrderStatus.PAID.name().equals(order.getStatus()) ? "订单已支付" : "订单已关闭");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(order.getExpireTime())) {
            releaseExpired(order, now);
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单已过期，请重新报名");
        }
        return mockPaymentChannel.prepay(order);
    }

    public PaymentStatusVO status(Long orderId, Long memberId) {
        PaymentOrder order = requireOwnedOrder(orderId, memberId);
        long remainSeconds = Math.max(0,
                Duration.between(LocalDateTime.now(), order.getExpireTime()).getSeconds());
        return new PaymentStatusVO(order.getId(), order.getOrderNo(), order.getStatus(),
                mockPaymentChannel.query(order).name(), order.getPayTime(), remainSeconds);
    }

    /**
     * 模拟收银台点「确认支付」：校验支付令牌后，由 Mock 渠道生成签名回调，走真实回调入口入账。
     */
    public CallbackResultVO mockPay(String orderNo, String payToken) {
        PaymentOrder order = paymentOrderMapper.selectOne(Wrappers.<PaymentOrder>lambdaQuery()
                .eq(PaymentOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!mockPaymentChannel.verifyPayToken(order, payToken)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "支付令牌无效，请重新发起支付");
        }
        if (!OrderStatus.PAYING.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单当前不可支付");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(order.getExpireTime())) {
            releaseExpired(order, now);
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单已过期，请重新报名");
        }
        return submit(order);
    }

    /**
     * 管理端重推回调，报文与首次回调完全一致，用于验证幂等。
     */
    public CallbackResultVO repush(Long orderId) {
        PaymentOrder order = orderService.requireById(orderId);
        return submit(order);
    }

    private CallbackResultVO submit(PaymentOrder order) {
        MockPaymentChannel.SignedNotification notification = mockPaymentChannel.buildNotification(order);
        return paymentCallbackService.handle(MockPaymentChannel.CODE,
                notification.body(), notification.signature());
    }

    private void releaseExpired(PaymentOrder order, LocalDateTime now) {
        if (orderService.closeExpired(order.getId(), now) == 1) {
            enrollmentService.cancelExpired(order.getEnrollmentId(), order.getMemberId());
        }
    }

    private PaymentOrder requireOwnedOrder(Long orderId, Long memberId) {
        PaymentOrder order = orderService.requireById(orderId);
        if (!Objects.equals(order.getMemberId(), memberId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }
}
