package com.quyong.attendance.module.face.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.face.config.FaceEngineProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "app.face", name = "provider", havingValue = "compreface")
public class CompreFaceRecognitionProvider implements FaceRecognitionProvider {

    private static final String PROVIDER_CODE = "COMPREFACE";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FaceEngineProperties properties;
    private final FaceTemplateCodec faceTemplateCodec;

    @Autowired
    public CompreFaceRecognitionProvider(ObjectMapper objectMapper,
                                         FaceEngineProperties properties,
                                         FaceTemplateCodec faceTemplateCodec) {
        this(createRestTemplate(properties), objectMapper, properties, faceTemplateCodec);
    }

    CompreFaceRecognitionProvider(RestTemplate restTemplate,
                                  ObjectMapper objectMapper,
                                  FaceEngineProperties properties,
                                  FaceTemplateCodec faceTemplateCodec) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.faceTemplateCodec = faceTemplateCodec;
    }

    @Override
    public FaceRegistrationResult register(Long userId, String imageData, String previousFeatureData) {
        ensureConfigured();

        String normalizedImage = normalizeBase64(imageData);
        DetectedFace detectedFace = detectSingleFace(normalizedImage);
        String subject = buildSubject(userId);
        JsonNode responseNode = postJson(buildAddFaceUrl(subject), buildImagePayload(normalizedImage));
        String imageId = requireText(readText(responseNode, "image_id"), "CompreFace 录入未返回 image_id");

        FaceTemplateMetadata metadata = new FaceTemplateMetadata();
        metadata.setProvider(PROVIDER_CODE);
        metadata.setVersion(Integer.valueOf(2));
        metadata.setEntityId(String.valueOf(userId));
        metadata.setSubject(subject);
        metadata.setImageId(imageId);
        metadata.setQualityScore(detectedFace.getQualityScore());
        metadata.setReferenceDigest(sha256Hex(normalizedImage));

        deletePreviousImage(faceTemplateCodec.decode(previousFeatureData));

        FaceRegistrationResult result = new FaceRegistrationResult();
        result.setFeatureData(faceTemplateCodec.encode(metadata));
        result.setEncryptFlag(Integer.valueOf(1));
        result.setProvider(PROVIDER_CODE);
        result.setQualityScore(detectedFace.getQualityScore());
        result.setLivenessScore(null);
        result.setLivenessPassed(null);
        return result;
    }

    @Override
    public FaceVerificationResult verify(Long userId, String imageData, String storedFeatureData) {
        FaceVerificationResult result = new FaceVerificationResult();
        result.setProvider(PROVIDER_CODE);
        result.setThreshold(defaultMatchThreshold());
        result.setMatched(Boolean.FALSE);

        FaceTemplateMetadata metadata = faceTemplateCodec.decode(storedFeatureData);
        if (metadata == null || !StringUtils.hasText(metadata.getImageId())) {
            result.setMessage("当前人脸档案版本过旧，请先重新录入人脸");
            return result;
        }

        ensureConfigured();

        try {
            VerifiedFace verifiedFace = verifySingleFace(metadata.getImageId(), normalizeBase64(imageData));
            BigDecimal faceScore = verifiedFace.getSimilarityScore();
            boolean matched = faceScore.compareTo(defaultMatchThreshold()) >= 0;

            result.setFaceScore(faceScore);
            result.setQualityScore(verifiedFace.getQualityScore());
            result.setLivenessScore(null);
            result.setLivenessPassed(null);
            result.setMatched(Boolean.valueOf(matched));
            result.setMessage(matched ? "人脸验证通过" : "人脸验证未通过");
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                result.setMessage("CompreFace 中不存在当前档案，请重新录入人脸");
                return result;
            }
            throw new IllegalStateException("CompreFace 人脸验证失败: " + extractRemoteMessage(exception, "远端未返回详细信息"), exception);
        }
    }

    private static RestTemplate createRestTemplate(FaceEngineProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int connectTimeout = properties.getComprefaceConnectTimeoutMs() == null ? 5000 : properties.getComprefaceConnectTimeoutMs().intValue();
        int readTimeout = properties.getComprefaceReadTimeoutMs() == null ? 15000 : properties.getComprefaceReadTimeoutMs().intValue();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return new RestTemplate(requestFactory);
    }

    private DetectedFace detectSingleFace(String normalizedImage) {
        JsonNode responseNode = postJson(buildRecognizeUrl(), buildImagePayload(normalizedImage));
        JsonNode resultArray = responseNode.path("result");
        if (!resultArray.isArray() || resultArray.size() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未检测到人脸，请正对摄像头重新采集");
        }
        if (resultArray.size() > 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "检测到多张人脸，请仅保留当前用户单人照片");
        }

        JsonNode faceNode = resultArray.get(0);
        return new DetectedFace(toPercent(readDecimal(faceNode.path("box"), "probability")));
    }

    private VerifiedFace verifySingleFace(String imageId, String normalizedImage) {
        JsonNode responseNode = postJson(buildVerifyUrl(imageId), buildImagePayload(normalizedImage));
        JsonNode resultArray = responseNode.path("result");
        if (!resultArray.isArray() || resultArray.size() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未检测到人脸，请正对摄像头重新采集");
        }
        if (resultArray.size() > 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "检测到多张人脸，请仅保留当前用户单人照片");
        }

        JsonNode faceNode = resultArray.get(0);
        BigDecimal similarity = toPercent(readDecimal(faceNode, "similarity"));
        BigDecimal qualityScore = toPercent(readDecimal(faceNode.path("box"), "probability"));
        if (similarity == null) {
            similarity = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new VerifiedFace(similarity, qualityScore);
    }

    private JsonNode postJson(String url, Map<String, Object> body) {
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    url,
                    new HttpEntity<Map<String, Object> >(body, buildHeaders()),
                    String.class
            );
            return readJson(responseEntity.getBody());
        } catch (BusinessException exception) {
            throw exception;
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), extractRemoteMessage(exception, "CompreFace 请求失败"));
        } catch (Exception exception) {
            throw new IllegalStateException("CompreFace 请求失败", exception);
        }
    }

    private void deletePreviousImage(FaceTemplateMetadata metadata) {
        if (metadata == null || !PROVIDER_CODE.equals(metadata.getProvider()) || !StringUtils.hasText(metadata.getImageId())) {
            return;
        }
        try {
            restTemplate.exchange(
                    buildDeleteFaceUrl(metadata.getImageId()),
                    HttpMethod.DELETE,
                    new HttpEntity<Object>(buildHeaders()),
                    String.class
            );
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() != 404) {
                // 历史样本清理失败不阻断本次换脸，避免用户档案丢失。
            }
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", requireText(properties.getComprefaceApiKey(), "CompreFace API Key 未配置"));
        return headers;
    }

    private Map<String, Object> buildImagePayload(String normalizedImage) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("file", normalizedImage);
        return body;
    }

    private String buildAddFaceUrl(String subject) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl())
                .path("/api/v1/recognition/faces")
                .queryParam("subject", subject)
                .queryParam("det_prob_threshold", properties.getComprefaceDetectProbThreshold())
                .build()
                .toUriString();
    }

    private String buildRecognizeUrl() {
        return UriComponentsBuilder.fromHttpUrl(baseUrl())
                .path("/api/v1/recognition/recognize")
                .queryParam("limit", Integer.valueOf(0))
                .queryParam("prediction_count", Integer.valueOf(1))
                .queryParam("status", Boolean.FALSE)
                .queryParam("detect_faces", Boolean.TRUE)
                .queryParam("det_prob_threshold", properties.getComprefaceDetectProbThreshold())
                .build()
                .toUriString();
    }

    private String buildVerifyUrl(String imageId) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl())
                .path("/api/v1/recognition/faces/{imageId}/verify")
                .queryParam("limit", Integer.valueOf(0))
                .queryParam("status", Boolean.FALSE)
                .queryParam("det_prob_threshold", properties.getComprefaceDetectProbThreshold())
                .buildAndExpand(imageId)
                .toUriString();
    }

    private String buildDeleteFaceUrl(String imageId) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl())
                .path("/api/v1/recognition/faces/{imageId}")
                .buildAndExpand(imageId)
                .toUriString();
    }

    private String baseUrl() {
        String baseUrl = requireText(properties.getComprefaceBaseUrl(), "CompreFace 服务地址未配置");
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private BigDecimal defaultMatchThreshold() {
        BigDecimal threshold = properties.getMatchScoreThreshold();
        if (threshold == null) {
            return new BigDecimal("85.00");
        }
        return threshold.setScale(2, RoundingMode.HALF_UP);
    }

    private void ensureConfigured() {
        requireText(properties.getComprefaceBaseUrl(), "CompreFace 服务地址未配置");
        requireText(properties.getComprefaceApiKey(), "CompreFace API Key 未配置");
        requireText(properties.getComprefaceSubjectPrefix(), "CompreFace subject 前缀未配置");
        requireText(properties.getMetadataSecret(), "人脸模板加密密钥未配置");
    }

    private String buildSubject(Long userId) {
        return requireText(properties.getComprefaceSubjectPrefix(), "CompreFace subject 前缀未配置") + userId;
    }

    private String normalizeBase64(String imageData) {
        String normalized = requireText(imageData, "人脸图像不能为空").replaceAll("\\s+", "");
        int separatorIndex = normalized.indexOf(',');
        if (separatorIndex >= 0) {
            normalized = normalized.substring(separatorIndex + 1);
        }
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "人脸图像不能为空");
        }
        return normalized;
    }

    private JsonNode readJson(String content) {
        try {
            return objectMapper.readTree(content == null ? "{}" : content);
        } catch (IOException exception) {
            throw new IllegalStateException("CompreFace 返回内容解析失败", exception);
        }
    }

    private String extractRemoteMessage(HttpStatusCodeException exception, String fallbackMessage) {
        String responseBody = exception.getResponseBodyAsString();
        if (!StringUtils.hasText(responseBody)) {
            return fallbackMessage;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            String message = readText(rootNode, "message");
            if (StringUtils.hasText(message)) {
                return message.trim();
            }
            message = readText(rootNode, "error");
            if (StringUtils.hasText(message)) {
                return message.trim();
            }
        } catch (IOException ignored) {
        }
        return responseBody.trim();
    }

    private String readText(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        String value = valueNode.asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isNumber()) {
            return new BigDecimal(valueNode.asText()).setScale(4, RoundingMode.HALF_UP);
        }
        String value = valueNode.asText(null);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return new BigDecimal(value.trim()).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal toPercent(BigDecimal rawScore) {
        if (rawScore == null) {
            return null;
        }
        return rawScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digestBytes = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digestBytes.length * 2);
            for (byte digestByte : digestBytes) {
                builder.append(String.format("%02x", digestByte));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("CompreFace 模板摘要生成失败", exception);
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private static final class DetectedFace {

        private final BigDecimal qualityScore;

        private DetectedFace(BigDecimal qualityScore) {
            this.qualityScore = qualityScore;
        }

        private BigDecimal getQualityScore() {
            return qualityScore;
        }
    }

    private static final class VerifiedFace {

        private final BigDecimal similarityScore;
        private final BigDecimal qualityScore;

        private VerifiedFace(BigDecimal similarityScore, BigDecimal qualityScore) {
            this.similarityScore = similarityScore;
            this.qualityScore = qualityScore;
        }

        private BigDecimal getSimilarityScore() {
            return similarityScore;
        }

        private BigDecimal getQualityScore() {
            return qualityScore;
        }
    }
}
