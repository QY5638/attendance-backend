package com.quyong.attendance;

import com.quyong.attendance.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:dev-startup;MODE=MySQL;NON_KEYWORDS=USER,ROLE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.hikari.initialization-fail-timeout=0"
        }
)
@ActiveProfiles("dev")
class DevProfileContextStartupTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldCreateUserMapperBeanWhenDevProfileHasDatasource() {
        assertNotNull(userMapper);
    }
}
