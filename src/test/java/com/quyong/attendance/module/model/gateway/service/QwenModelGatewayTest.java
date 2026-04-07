package com.quyong.attendance.module.model.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.module.model.gateway.config.ModelGatewayProperties;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeRequest;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenModelGatewayTest {

    @Test
    void shouldParseStructuredQwenResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenModelGateway gateway = new QwenModelGateway(restTemplate, new ObjectMapper(), buildProperties());

        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(content().string(containsString("\"model\":\"qwen-plus\"")))
                .andExpect(content().string(containsString("系统提示词内容")))
                .andExpect(content().string(containsString("recordId=2006")))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"conclusion\\\":\\\"PROXY_CHECKIN\\\",\\\"riskLevel\\\":\\\"HIGH\\\",\\\"confidenceScore\\\":92.5,\\\"decisionReason\\\":\\\"设备异常、地点异常且行为模式偏离历史规律\\\",\\\"reasonSummary\\\":\\\"设备与地点异常共同提升风险\\\",\\\"actionSuggestion\\\":\\\"建议优先人工复核\\\",\\\"similarCaseSummary\\\":\\\"存在相似设备异常与低分值组合案例\\\"}\"}}]}", MediaType.APPLICATION_JSON));

        ModelInvokeResponse response = gateway.invoke(buildRequest());

        assertEquals("PROXY_CHECKIN", response.getConclusion());
        assertEquals("HIGH", response.getRiskLevel());
        assertEquals(new BigDecimal("92.5"), response.getConfidenceScore());
        assertEquals("设备异常、地点异常且行为模式偏离历史规律", response.getDecisionReason());
        assertEquals("设备与地点异常共同提升风险", response.getReasonSummary());
        assertEquals("建议优先人工复核", response.getActionSuggestion());
        assertEquals("存在相似设备异常与低分值组合案例", response.getSimilarCaseSummary());
        assertEquals("{\"conclusion\":\"PROXY_CHECKIN\",\"riskLevel\":\"HIGH\",\"confidenceScore\":92.5,\"decisionReason\":\"设备异常、地点异常且行为模式偏离历史规律\",\"reasonSummary\":\"设备与地点异常共同提升风险\",\"actionSuggestion\":\"建议优先人工复核\",\"similarCaseSummary\":\"存在相似设备异常与低分值组合案例\"}", response.getRawResponse());
        server.verify();
    }

    @Test
    void shouldExtractJsonFromMarkdownFenceResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenModelGateway gateway = new QwenModelGateway(restTemplate, new ObjectMapper(), buildProperties());

        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"```json\\n{\\\"conclusion\\\":\\\"PROXY_CHECKIN\\\",\\\"riskLevel\\\":\\\"HIGH\\\",\\\"confidenceScore\\\":91.2,\\\"decisionReason\\\":\\\"模型识别异常\\\",\\\"reasonSummary\\\":\\\"存在跨设备风险\\\",\\\"actionSuggestion\\\":\\\"建议人工确认\\\",\\\"similarCaseSummary\\\":\\\"存在相似案例\\\"}\\n```\"}}]}", MediaType.APPLICATION_JSON));

        ModelInvokeResponse response = gateway.invoke(buildRequest());

        assertEquals("PROXY_CHECKIN", response.getConclusion());
        assertEquals("HIGH", response.getRiskLevel());
        assertEquals(new BigDecimal("91.2"), response.getConfidenceScore());
        assertEquals("模型识别异常", response.getDecisionReason());
        assertEquals("存在跨设备风险", response.getReasonSummary());
        assertEquals("建议人工确认", response.getActionSuggestion());
        assertEquals("存在相似案例", response.getSimilarCaseSummary());
        server.verify();
    }

    @Test
    void shouldThrowWhenModelContentIsNotJson() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenModelGateway gateway = new QwenModelGateway(restTemplate, new ObjectMapper(), buildProperties());

        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"这不是 JSON\"}}]}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> gateway.invoke(buildRequest()));
        server.verify();
    }

    private ModelInvokeRequest buildRequest() {
        ModelInvokeRequest request = new ModelInvokeRequest();
        request.setSceneType("EXCEPTION_ANALYSIS");
        request.setBusinessId(2006L);
        request.setPromptTemplateId(8001L);
        request.setPromptVersion("v1.0");
        request.setPromptContent("系统提示词内容");
        request.setInputSummary("recordId=2006, userId=1002");
        return request;
    }

    private ModelGatewayProperties buildProperties() {
        ModelGatewayProperties properties = new ModelGatewayProperties();
        properties.setProvider("qwen");
        properties.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        properties.setModel("qwen-plus");
        properties.setApiKey("test-api-key");
        properties.setTimeoutMs(10000);
        return properties;
    }
}
