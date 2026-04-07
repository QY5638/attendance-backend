package com.quyong.attendance;

import com.quyong.attendance.module.model.gateway.service.MockModelGateway;
import com.quyong.attendance.module.model.gateway.service.ModelGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(properties = {"spring.config.import=", "app.llm.provider="})
@ActiveProfiles("test")
class ModelGatewayFallbackBeanTest {

    @Autowired
    private ModelGateway modelGateway;

    @Test
    void shouldLoadMockGatewayWhenLocalLlmConfigIsAbsent() {
        assertInstanceOf(MockModelGateway.class, modelGateway);
    }
}
