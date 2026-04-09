package com.quyong.attendance.module.model.gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.module.model.gateway.config.ModelGatewayProperties;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeRequest;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "app.llm", name = "provider", havingValue = "qwen")
public class QwenModelGateway implements ModelGateway {

    private static final String JSON_PREFIX = "```json";
    private static final String CODE_FENCE = "```";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ModelGatewayProperties properties;

    @Autowired
    public QwenModelGateway(ObjectMapper objectMapper, ModelGatewayProperties properties) {
        this(createRestTemplate(properties), objectMapper, properties);
    }

    QwenModelGateway(RestTemplate restTemplate, ObjectMapper objectMapper, ModelGatewayProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public ModelInvokeResponse invoke(ModelInvokeRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(requireText(properties.getApiKey(), "模型 API Key 未配置"));

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                buildChatCompletionsUrl(),
                new HttpEntity<Object>(buildRequestBody(request), headers),
                String.class
        );

        String responseBody = responseEntity.getBody();
        String jsonContent = extractJsonContent(extractMessageContent(responseBody));
        JsonNode resultNode = readJson(jsonContent);

        ModelInvokeResponse response = new ModelInvokeResponse();
        response.setConclusion(requireText(readText(resultNode, "conclusion"), "模型返回缺少 conclusion"));
        response.setRiskLevel(requireText(readText(resultNode, "riskLevel"), "模型返回缺少 riskLevel"));
        response.setConfidenceScore(readDecimal(resultNode, "confidenceScore"));
        response.setDecisionReason(requireText(readText(resultNode, "decisionReason"), "模型返回缺少 decisionReason"));
        response.setReasonSummary(readText(resultNode, "reasonSummary"));
        response.setActionSuggestion(readText(resultNode, "actionSuggestion"));
        response.setSimilarCaseSummary(readText(resultNode, "similarCaseSummary"));
        response.setRawResponse(jsonContent);
        return response;
    }

    private static RestTemplate createRestTemplate(ModelGatewayProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeout = properties.getTimeoutMs() == null ? 10000 : properties.getTimeoutMs().intValue();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return new RestTemplate(requestFactory);
    }

    private Map<String, Object> buildRequestBody(ModelInvokeRequest request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", requireText(properties.getModel(), "模型名称未配置"));
        body.put("messages", buildMessages(request));

        Map<String, Object> responseFormat = new LinkedHashMap<String, Object>();
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);
        return body;
    }

    private List<Map<String, String>> buildMessages(ModelInvokeRequest request) {
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        messages.add(buildMessage("system", ensureJsonInstruction(request.getPromptContent())));
        messages.add(buildMessage("user", request.getInputSummary()));
        return messages;
    }

    private String ensureJsonInstruction(String promptContent) {
        String content = requireText(promptContent, "模型请求内容不能为空");
        StringBuilder builder = new StringBuilder(content);

        if (!content.toLowerCase().contains("json")) {
            builder.append("\n\n请仅返回合法 JSON 对象，不要输出任何 JSON 以外的说明文字。");
        }

        builder.append("\n\n返回 JSON 必须包含以下字段：")
                .append(" conclusion")
                .append(", riskLevel")
                .append(", confidenceScore")
                .append(", decisionReason")
                .append(", reasonSummary")
                .append(", actionSuggestion")
                .append(", similarCaseSummary。")
                .append(" 其中 riskLevel 仅允许 HIGH、MEDIUM、LOW。")
                .append(" 所有字段都必须返回，缺失时请返回空字符串或 0，不要省略字段。");

        return builder.toString();
    }

    private Map<String, String> buildMessage(String role, String content) {
        Map<String, String> message = new LinkedHashMap<String, String>();
        message.put("role", role);
        message.put("content", requireText(content, "模型请求内容不能为空"));
        return message;
    }

    private String buildChatCompletionsUrl() {
        String baseUrl = requireText(properties.getBaseUrl(), "模型服务地址未配置");
        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat/completions";
        }
        return baseUrl + "/chat/completions";
    }

    private String extractMessageContent(String responseBody) {
        JsonNode rootNode = readJson(responseBody);
        JsonNode contentNode = rootNode.path("choices").path(0).path("message").path("content");
        String content = contentNode.isMissingNode() || contentNode.isNull() ? null : contentNode.asText();
        return requireText(content, "模型返回内容为空");
    }

    private String extractJsonContent(String content) {
        String normalized = requireText(content, "模型返回内容为空").trim();
        if (normalized.startsWith(JSON_PREFIX)) {
            normalized = normalized.substring(JSON_PREFIX.length()).trim();
        } else if (normalized.startsWith(CODE_FENCE)) {
            normalized = normalized.substring(CODE_FENCE.length()).trim();
        }
        if (normalized.endsWith(CODE_FENCE)) {
            normalized = normalized.substring(0, normalized.length() - CODE_FENCE.length()).trim();
        }

        int startIndex = normalized.indexOf('{');
        int endIndex = normalized.lastIndexOf('}');
        if (startIndex < 0 || endIndex < startIndex) {
            throw new IllegalStateException("模型返回内容不是合法 JSON");
        }
        return normalized.substring(startIndex, endIndex + 1);
    }

    private JsonNode readJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (IOException exception) {
            throw new IllegalStateException("模型返回 JSON 解析失败", exception);
        }
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName) {
        String text = readText(node, fieldName);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return new BigDecimal(text.trim());
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        return fieldNode.asText();
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }
}
