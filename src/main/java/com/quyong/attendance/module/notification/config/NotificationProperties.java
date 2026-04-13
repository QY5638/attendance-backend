package com.quyong.attendance.module.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private Long streamTimeoutMs = Long.valueOf(600000L);
    private Integer defaultReplyTimeoutHours = Integer.valueOf(24);
    private Integer reminderCooldownMinutes = Integer.valueOf(240);

    public Long getStreamTimeoutMs() {
        return streamTimeoutMs;
    }

    public void setStreamTimeoutMs(Long streamTimeoutMs) {
        this.streamTimeoutMs = streamTimeoutMs;
    }

    public Integer getDefaultReplyTimeoutHours() {
        return defaultReplyTimeoutHours;
    }

    public void setDefaultReplyTimeoutHours(Integer defaultReplyTimeoutHours) {
        this.defaultReplyTimeoutHours = defaultReplyTimeoutHours;
    }

    public Integer getReminderCooldownMinutes() {
        return reminderCooldownMinutes;
    }

    public void setReminderCooldownMinutes(Integer reminderCooldownMinutes) {
        this.reminderCooldownMinutes = reminderCooldownMinutes;
    }
}
