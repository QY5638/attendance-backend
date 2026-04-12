package com.quyong.attendance.module.face.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.face")
public class FaceEngineProperties {

    private String provider = "compreface";
    private String comprefaceBaseUrl = "http://127.0.0.1:8000";
    private String comprefaceApiKey;
    private String comprefaceSubjectPrefix = "attendance-user-";
    private BigDecimal comprefaceDetectProbThreshold = new BigDecimal("0.80");
    private Integer comprefaceConnectTimeoutMs = Integer.valueOf(5000);
    private Integer comprefaceReadTimeoutMs = Integer.valueOf(15000);
    private BigDecimal matchScoreThreshold = new BigDecimal("85.00");
    private BigDecimal qualityScoreThreshold = new BigDecimal("70.00");
    private BigDecimal livenessThreshold = new BigDecimal("0.80");
    private Boolean requireLiveness = Boolean.TRUE;
    private Long livenessSessionTtlSeconds = Long.valueOf(180L);
    private Long livenessProofTtlSeconds = Long.valueOf(180L);
    private Integer livenessActionCount = Integer.valueOf(3);
    private Long livenessMinDurationMs = Long.valueOf(3000L);
    private Long livenessMaxDurationMs = Long.valueOf(45000L);
    private Integer livenessMinSampleCount = Integer.valueOf(12);
    private BigDecimal livenessBlinkScoreThreshold = new BigDecimal("0.65");
    private BigDecimal livenessMouthOpenScoreThreshold = new BigDecimal("0.65");
    private BigDecimal livenessTurnScoreThreshold = new BigDecimal("0.70");
    private Long maxFaceNum = Long.valueOf(1L);
    private String metadataSecret;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getComprefaceBaseUrl() {
        return comprefaceBaseUrl;
    }

    public void setComprefaceBaseUrl(String comprefaceBaseUrl) {
        this.comprefaceBaseUrl = comprefaceBaseUrl;
    }

    public String getComprefaceApiKey() {
        return comprefaceApiKey;
    }

    public void setComprefaceApiKey(String comprefaceApiKey) {
        this.comprefaceApiKey = comprefaceApiKey;
    }

    public String getComprefaceSubjectPrefix() {
        return comprefaceSubjectPrefix;
    }

    public void setComprefaceSubjectPrefix(String comprefaceSubjectPrefix) {
        this.comprefaceSubjectPrefix = comprefaceSubjectPrefix;
    }

    public BigDecimal getComprefaceDetectProbThreshold() {
        return comprefaceDetectProbThreshold;
    }

    public void setComprefaceDetectProbThreshold(BigDecimal comprefaceDetectProbThreshold) {
        this.comprefaceDetectProbThreshold = comprefaceDetectProbThreshold;
    }

    public Integer getComprefaceConnectTimeoutMs() {
        return comprefaceConnectTimeoutMs;
    }

    public void setComprefaceConnectTimeoutMs(Integer comprefaceConnectTimeoutMs) {
        this.comprefaceConnectTimeoutMs = comprefaceConnectTimeoutMs;
    }

    public Integer getComprefaceReadTimeoutMs() {
        return comprefaceReadTimeoutMs;
    }

    public void setComprefaceReadTimeoutMs(Integer comprefaceReadTimeoutMs) {
        this.comprefaceReadTimeoutMs = comprefaceReadTimeoutMs;
    }

    public BigDecimal getMatchScoreThreshold() {
        return matchScoreThreshold;
    }

    public void setMatchScoreThreshold(BigDecimal matchScoreThreshold) {
        this.matchScoreThreshold = matchScoreThreshold;
    }

    public BigDecimal getQualityScoreThreshold() {
        return qualityScoreThreshold;
    }

    public void setQualityScoreThreshold(BigDecimal qualityScoreThreshold) {
        this.qualityScoreThreshold = qualityScoreThreshold;
    }

    public BigDecimal getLivenessThreshold() {
        return livenessThreshold;
    }

    public void setLivenessThreshold(BigDecimal livenessThreshold) {
        this.livenessThreshold = livenessThreshold;
    }

    public Boolean getRequireLiveness() {
        return requireLiveness;
    }

    public void setRequireLiveness(Boolean requireLiveness) {
        this.requireLiveness = requireLiveness;
    }

    public Long getLivenessSessionTtlSeconds() {
        return livenessSessionTtlSeconds;
    }

    public void setLivenessSessionTtlSeconds(Long livenessSessionTtlSeconds) {
        this.livenessSessionTtlSeconds = livenessSessionTtlSeconds;
    }

    public Long getLivenessProofTtlSeconds() {
        return livenessProofTtlSeconds;
    }

    public void setLivenessProofTtlSeconds(Long livenessProofTtlSeconds) {
        this.livenessProofTtlSeconds = livenessProofTtlSeconds;
    }

    public Integer getLivenessActionCount() {
        return livenessActionCount;
    }

    public void setLivenessActionCount(Integer livenessActionCount) {
        this.livenessActionCount = livenessActionCount;
    }

    public Long getLivenessMinDurationMs() {
        return livenessMinDurationMs;
    }

    public void setLivenessMinDurationMs(Long livenessMinDurationMs) {
        this.livenessMinDurationMs = livenessMinDurationMs;
    }

    public Long getLivenessMaxDurationMs() {
        return livenessMaxDurationMs;
    }

    public void setLivenessMaxDurationMs(Long livenessMaxDurationMs) {
        this.livenessMaxDurationMs = livenessMaxDurationMs;
    }

    public Integer getLivenessMinSampleCount() {
        return livenessMinSampleCount;
    }

    public void setLivenessMinSampleCount(Integer livenessMinSampleCount) {
        this.livenessMinSampleCount = livenessMinSampleCount;
    }

    public BigDecimal getLivenessBlinkScoreThreshold() {
        return livenessBlinkScoreThreshold;
    }

    public void setLivenessBlinkScoreThreshold(BigDecimal livenessBlinkScoreThreshold) {
        this.livenessBlinkScoreThreshold = livenessBlinkScoreThreshold;
    }

    public BigDecimal getLivenessMouthOpenScoreThreshold() {
        return livenessMouthOpenScoreThreshold;
    }

    public void setLivenessMouthOpenScoreThreshold(BigDecimal livenessMouthOpenScoreThreshold) {
        this.livenessMouthOpenScoreThreshold = livenessMouthOpenScoreThreshold;
    }

    public BigDecimal getLivenessTurnScoreThreshold() {
        return livenessTurnScoreThreshold;
    }

    public void setLivenessTurnScoreThreshold(BigDecimal livenessTurnScoreThreshold) {
        this.livenessTurnScoreThreshold = livenessTurnScoreThreshold;
    }

    public Long getMaxFaceNum() {
        return maxFaceNum;
    }

    public void setMaxFaceNum(Long maxFaceNum) {
        this.maxFaceNum = maxFaceNum;
    }

    public String getMetadataSecret() {
        return metadataSecret;
    }

    public void setMetadataSecret(String metadataSecret) {
        this.metadataSecret = metadataSecret;
    }
}
