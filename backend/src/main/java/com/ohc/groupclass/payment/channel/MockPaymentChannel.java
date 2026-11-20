package com.ohc.groupclass.payment.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohc.groupclass.order.entity.PaymentOrder;
import com.ohc.groupclass.payment.dto.PaymentNotification;
import com.ohc.groupclass.payment.dto.PrepayVO;
import com.ohc.groupclass.payment.enums.ChannelPayState;
import com.ohc.groupclass.payment.support.PaymentSignature;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 模拟支付渠道：不依赖外部商户号，预下单只发本地令牌，回调报文由本渠道自己签名后交给回调入口。
 * 形态与真实渠道一致（预下单 → 收银台 → 异步回调），接微信支付时换实现即可。
 */
@Component
@RequiredArgsConstructor
public class MockPaymentChannel implements PaymentChannel {

    public static final String CODE = "MOCK";

    private final ObjectMapper objectMapper;

    @Value("${group-class.payment.mock.secret:group-class-mock-pay-secret}")
    private String secret;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public PrepayVO prepay(PaymentOrder order) {
        return new PrepayVO(CODE, order.getOrderNo(), order.getAmount(),
                order.getExpireTime(), payToken(order));
    }

    @Override
    public boolean verify(String rawBody, String signature) {
        return PaymentSignature.matches(secret, rawBody, signature);
    }

    /**
     * Mock 渠道没有独立账本，查单直接回读本系统订单状态；接真实渠道时换成主动查单请求。
     */
    @Override
    public ChannelPayState query(PaymentOrder order) {
        return switch (order.getStatus()) {
            case "PAID" -> ChannelPayState.SUCCESS;
            case "CLOSED", "REFUNDED" -> ChannelPayState.CLOSED;
            case "PAYING" -> ChannelPayState.NOTPAY;
            default -> ChannelPayState.UNKNOWN;
        };
    }

    @Override
    public RefundResult refund(PaymentOrder order, BigDecimal amount) {
        return new RefundResult("R" + order.getOrderNo(), true);
    }

    public boolean verifyPayToken(PaymentOrder order, String token) {
        return PaymentSignature.matches(secret, tokenSource(order), token);
    }

    /**
     * 回调编号按订单号生成，重复重推时报文完全一致，便于验证幂等。
     */
    public SignedNotification buildNotification(PaymentOrder order) {
        PaymentNotification notification = new PaymentNotification(
                "MOCK-" + order.getOrderNo(),
                order.getOrderNo(),
                order.getAmount(),
                CODE,
                "SUCCESS");
        try {
            String body = objectMapper.writeValueAsString(notification);
            return new SignedNotification(body, PaymentSignature.sign(secret, body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("构造模拟回调报文失败", e);
        }
    }

    private String payToken(PaymentOrder order) {
        return PaymentSignature.sign(secret, tokenSource(order));
    }

    private String tokenSource(PaymentOrder order) {
        return order.getOrderNo() + "|" + order.getAmount().toPlainString() + "|" + order.getExpireTime();
    }

    public record SignedNotification(String body, String signature) {
    }
}
