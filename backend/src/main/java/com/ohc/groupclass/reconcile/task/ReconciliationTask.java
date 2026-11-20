package com.ohc.groupclass.reconcile.task;

import com.ohc.groupclass.reconcile.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ReconciliationTask {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationTask.class);

    private final ReconciliationService reconciliationService;

    @Scheduled(cron = "${group-class.schedule.reconcile-cron:0 0 2 * * *}")
    public void daily() {
        try {
            reconciliationService.run(LocalDate.now());
        } catch (Exception e) {
            log.error("[对账] 定时任务失败", e);
        }
    }
}
