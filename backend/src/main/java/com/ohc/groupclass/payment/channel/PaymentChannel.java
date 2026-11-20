package com.ohc.groupclass.payment.channel;

import com.ohc.groupclass.order.entity.PaymentOrder;
import com.ohc.groupclass.payment.dto.PrepayVO;
import com.ohc.groupclass.payment.enums.ChannelPayState;

import java.math.BigDecimal;

public interface PaymentChannel {

    String code();

    PrepayVO prepay(PaymentOrder order);

    boolean verify(String rawBody, String signature);

    ChannelPayState query(PaymentOrder order);

    RefundResult refund(PaymentOrder order, BigDecimal amount);

    record RefundResult(String refundNo, boolean success) {
    }
}
