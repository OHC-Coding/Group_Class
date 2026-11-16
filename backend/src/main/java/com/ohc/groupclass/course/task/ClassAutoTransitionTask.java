package com.ohc.groupclass.course.task;

import com.ohc.groupclass.course.service.ClassStatusService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassAutoTransitionTask {

    private static final Logger log = LoggerFactory.getLogger(ClassAutoTransitionTask.class);

    private final ClassStatusService classStatusService;

    @Scheduled(fixedDelayString = "${group-class.schedule.auto-transition-ms:60000}", initialDelay = 10000)
    public void autoAdvance() {
        try {
            classStatusService.autoAdvance();
        } catch (Exception e) {
            log.error("[班级状态] 自动流转失败", e);
        }
    }
}
