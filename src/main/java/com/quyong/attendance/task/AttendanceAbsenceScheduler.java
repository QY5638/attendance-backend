package com.quyong.attendance.task;

import com.quyong.attendance.module.warning.service.WarningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttendanceAbsenceScheduler {

    private static final Logger log = LoggerFactory.getLogger(AttendanceAbsenceScheduler.class);

    private final WarningService warningService;

    public AttendanceAbsenceScheduler(WarningService warningService) {
        this.warningService = warningService;
    }

    @Scheduled(fixedDelayString = "${app.attendance.absence-check-fixed-delay-ms:3600000}",
            initialDelayString = "${app.attendance.absence-check-fixed-delay-ms:3600000}")
    public void run() {
        try {
            int createdCount = warningService.runAbsenceCheck();
            if (createdCount > 0) {
                log.info("absence-check created {} records", Integer.valueOf(createdCount));
            }
        } catch (RuntimeException exception) {
            log.warn("absence-check skipped: {}", exception.getMessage());
        }
    }
}
