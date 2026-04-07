package com.quyong.attendance.module.map.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.map")
public class MapProperties {

    private String provider;
    private String baseUrl;
    private String apiKey;
    private Integer timeoutMs = Integer.valueOf(3000);
    private Integer multiLocationWindowMinutes = Integer.valueOf(30);
    private BigDecimal multiLocationDistanceMeters = new BigDecimal("3000");

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getMultiLocationWindowMinutes() {
        return multiLocationWindowMinutes;
    }

    public void setMultiLocationWindowMinutes(Integer multiLocationWindowMinutes) {
        this.multiLocationWindowMinutes = multiLocationWindowMinutes;
    }

    public BigDecimal getMultiLocationDistanceMeters() {
        return multiLocationDistanceMeters;
    }

    public void setMultiLocationDistanceMeters(BigDecimal multiLocationDistanceMeters) {
        this.multiLocationDistanceMeters = multiLocationDistanceMeters;
    }
}
