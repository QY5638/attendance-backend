package com.quyong.attendance;

import com.quyong.attendance.common.exception.GlobalExceptionHandler;
import com.quyong.attendance.config.MybatisPlusConfig;
import com.quyong.attendance.config.RedisConfig;
import com.quyong.attendance.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AttendanceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldLoadApplicationContextAndCoreBeans() {
        assertNotNull(applicationContext.getBean(AttendanceApplication.class));
        assertNotNull(applicationContext.getBean(SecurityConfig.class));
        assertNotNull(applicationContext.getBean(MybatisPlusConfig.class));
        assertNotNull(applicationContext.getBean(RedisConfig.class));
        assertNotNull(applicationContext.getBean(GlobalExceptionHandler.class));
    }
}
