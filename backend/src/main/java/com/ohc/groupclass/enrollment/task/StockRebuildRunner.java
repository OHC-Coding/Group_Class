package com.ohc.groupclass.enrollment.task;

import com.ohc.groupclass.enrollment.service.StockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockRebuildRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockRebuildRunner.class);

    private final StockService stockService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int count = stockService.rebuildAll();
            log.info("[库存] 启动重建完成，共 {} 个报名中或已关单的班级", count);
        } catch (Exception e) {
            log.warn("[库存] 启动重建失败，Redis 可用后调管理端重建接口：{}", e.getMessage());
        }
    }
}
