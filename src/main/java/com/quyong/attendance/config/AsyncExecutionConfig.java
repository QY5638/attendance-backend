package com.quyong.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncExecutionConfig {

    @Bean(name = "attendanceClosedLoopExecutor")
    @Profile("test")
    public TaskExecutor attendanceClosedLoopSyncExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean(name = "attendanceClosedLoopExecutor")
    @Profile("!test")
    public TaskExecutor attendanceClosedLoopExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("attendance-closed-loop-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
