package com.ohc.groupclass.order.task;

import com.ohc.groupclass.enrollment.service.EnrollmentService;
import com.ohc.groupclass.order.entity.PaymentOrder;
import com.ohc.groupclass.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    private static final int BATCH_LIMIT = 500;

    private final OrderService orderService;
    private final EnrollmentService enrollmentService;

    @Scheduled(fixedDelayString = "${group-class.schedule.order-timeout-ms:60000}", initialDelay = 20000)
    public void closeExpired() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<PaymentOrder> expired = orderService.findExpired(now, BATCH_LIMIT);
            int closed = 0;
            for (PaymentOrder order : expired) {
                if (orderService.closeExpired(order.getId(), now) == 1) {
                    enrollmentService.cancelExpired(order.getEnrollmentId(), order.getMemberId());
                    closed++;
                }
            }
            if (closed > 0) {
                log.info("[订单] 超时关单 {} 笔，已释放对应名额", closed);
            }
        } catch (Exception e) {
            log.error("[订单] 超时关单失败", e);
        }
    }
}
