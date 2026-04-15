package com.quyong.attendance.task;

import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.exceptiondetect.entity.Rule;
import com.quyong.attendance.module.exceptiondetect.service.RuleService;
import com.quyong.attendance.module.warning.service.WarningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class AttendanceAbsenceScheduler {

    private static final Logger log = LoggerFactory.getLogger(AttendanceAbsenceScheduler.class);

    private final WarningService warningService;
    private final RuleService ruleService;
    private final Clock clock;

    private volatile LocalDate lastAbsenceHandledDate;
    private volatile LocalDate lastMissingCheckoutHandledDate;

    public AttendanceAbsenceScheduler(WarningService warningService,
                                      RuleService ruleService,
                                      Clock clock) {
        this.warningService = warningService;
        this.ruleService = ruleService;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void compensateTodayOnStartup() {
        triggerAbsenceIfDue("startup");
        triggerMissingCheckoutIfDue("startup");
    }

    @Scheduled(cron = "${app.attendance.absence-check-cron:*/10 * * * * *}")
    public void run() {
        triggerAbsenceIfDue("schedule");
        triggerMissingCheckoutIfDue("schedule");
    }

    private void triggerAbsenceIfDue(String triggerSource) {
        try {
            if (!isAbsenceDueNow()) {
                return;
            }
            LocalDate today = LocalDate.now(clock);
            synchronized (this) {
                // 启动补偿与定时轮询共用同一入口，避免当天重复补判。
                if (!isAbsenceDueNow() || today.equals(lastAbsenceHandledDate)) {
                    return;
                }
                int createdCount = warningService.runAbsenceCheck();
                lastAbsenceHandledDate = today;
                if (createdCount > 0) {
                    log.info("absence-check {} created {} records", triggerSource, Integer.valueOf(createdCount));
                }
            }
        } catch (RuntimeException exception) {
            log.warn("absence-check {} skipped: {}", triggerSource, exception.getMessage());
        }
    }

    private void triggerMissingCheckoutIfDue(String triggerSource) {
        try {
            LocalDate targetDate = resolveMissingCheckoutTargetDate();
            if (targetDate == null || !isMissingCheckoutDueNow()) {
                return;
            }
            synchronized (this) {
                LocalDate currentTargetDate = resolveMissingCheckoutTargetDate();
                if (currentTargetDate == null
                        || !isMissingCheckoutDueNow()
                        || currentTargetDate.equals(lastMissingCheckoutHandledDate)) {
                    return;
                }
                int createdCount = warningService.runMissingCheckoutCheck();
                lastMissingCheckoutHandledDate = currentTargetDate;
                if (createdCount > 0) {
                    log.info("missing-checkout-check {} settled {} created {} records",
                            triggerSource,
                            currentTargetDate,
                            Integer.valueOf(createdCount));
                }
            }
        } catch (RuntimeException exception) {
            log.warn("missing-checkout-check {} skipped: {}", triggerSource, exception.getMessage());
        }
    }

    private boolean isAbsenceDueNow() {
        Rule rule = resolveEnabledRule();
        if (rule == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!isWorkday(now)) {
            return false;
        }
        LocalTime cutoffTime = rule.getStartTime().plusMinutes(rule.getLateThreshold().longValue());
        return !now.toLocalTime().isBefore(cutoffTime);
    }

    private boolean isMissingCheckoutDueNow() {
        Rule rule = resolveEnabledRule();
        if (rule == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate targetDate = now.toLocalDate().minusDays(1L);
        if (!isWorkday(targetDate)) {
            return false;
        }
        return !now.toLocalTime().isBefore(LocalTime.MIDNIGHT);
    }

    private LocalDate resolveMissingCheckoutTargetDate() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate targetDate = now.toLocalDate().minusDays(1L);
        if (!isWorkday(targetDate)) {
            return null;
        }
        return targetDate;
    }

    private Rule resolveEnabledRule() {
        try {
            return ruleService.getEnabledRule();
        } catch (BusinessException exception) {
            return null;
        }
    }

    private boolean isWorkday(LocalDateTime now) {
        return now != null && isWorkday(now.toLocalDate());
    }

    private boolean isWorkday(LocalDate date) {
        if (date == null) {
            return false;
        }
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
