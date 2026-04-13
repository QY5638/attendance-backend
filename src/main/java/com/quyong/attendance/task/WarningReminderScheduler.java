package com.quyong.attendance.task;

import com.quyong.attendance.module.warning.service.WarningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WarningReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarningReminderScheduler.class);

    private final WarningService warningService;

    public WarningReminderScheduler(WarningService warningService) {
        this.warningService = warningService;
    }

    @Scheduled(fixedDelayString = "${app.notification.reminder-fixed-delay-ms:300000}",
            initialDelayString = "${app.notification.reminder-fixed-delay-ms:300000}")
    public void run() {
        try {
            int createdCount = warningService.runReminderCheck();
            if (createdCount > 0) {
                log.info("warning-reminder created {} notifications", Integer.valueOf(createdCount));
            }
        } catch (RuntimeException exception) {
            log.warn("warning-reminder skipped: {}", exception.getMessage());
        }
    }
}
