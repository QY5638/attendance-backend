package com.quyong.attendance.module.map.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.module.map.config.MapProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MapDistanceService {

    private static final double EARTH_RADIUS_METERS = 6371000D;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MapProperties properties;

    @Autowired
    public MapDistanceService(ObjectMapper objectMapper, MapProperties properties) {
        this(createRestTemplate(properties), objectMapper, properties);
    }

    MapDistanceService(RestTemplate restTemplate, ObjectMapper objectMapper, MapProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public BigDecimal calculateDistanceMeters(BigDecimal fromLongitude,
                                              BigDecimal fromLatitude,
                                              BigDecimal toLongitude,
                                              BigDecimal toLatitude) {
        if (shouldUseAmap()) {
            try {
                return requestAmapDistance(fromLongitude, fromLatitude, toLongitude, toLatitude);
            } catch (Exception ignored) {
                // 地图服务不可用时回退到本地球面距离，避免主链中断。
            }
        }
        return calculateGeodesicDistance(fromLongitude, fromLatitude, toLongitude, toLatitude);
    }

    private boolean shouldUseAmap() {
        return "amap".equalsIgnoreCase(trim(properties.getProvider()))
                && StringUtils.hasText(properties.getBaseUrl())
                && StringUtils.hasText(properties.getApiKey());
    }

    private BigDecimal requestAmapDistance(BigDecimal fromLongitude,
                                           BigDecimal fromLatitude,
                                           BigDecimal toLongitude,
                                           BigDecimal toLatitude) {
        String responseBody = restTemplate.getForObject(buildDistanceUrl(fromLongitude, fromLatitude, toLongitude, toLatitude), String.class);
        JsonNode rootNode = readJson(responseBody);
        if (!"1".equals(rootNode.path("status").asText())) {
            throw new IllegalStateException("地图服务返回失败状态");
        }

        String distance = rootNode.path("results").path(0).path("distance").asText();
        if (!StringUtils.hasText(distance)) {
            throw new IllegalStateException("地图服务返回距离为空");
        }
        return new BigDecimal(distance.trim());
    }

    private String buildDistanceUrl(BigDecimal fromLongitude,
                                    BigDecimal fromLatitude,
                                    BigDecimal toLongitude,
                                    BigDecimal toLatitude) {
        String baseUrl = trim(properties.getBaseUrl());
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return UriComponentsBuilder.fromHttpUrl(baseUrl + "/distance")
                .queryParam("origins", fromLongitude.toPlainString() + "," + fromLatitude.toPlainString())
                .queryParam("destination", toLongitude.toPlainString() + "," + toLatitude.toPlainString())
                .queryParam("key", trim(properties.getApiKey()))
                .queryParam("type", 0)
                .queryParam("output", "json")
                .build(false)
                .toUriString();
    }

    private BigDecimal calculateGeodesicDistance(BigDecimal fromLongitude,
                                                 BigDecimal fromLatitude,
                                                 BigDecimal toLongitude,
                                                 BigDecimal toLatitude) {
        double fromLatRadians = Math.toRadians(fromLatitude.doubleValue());
        double toLatRadians = Math.toRadians(toLatitude.doubleValue());
        double latDiff = toLatRadians - fromLatRadians;
        double lonDiff = Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());
        double haversine = Math.sin(latDiff / 2D) * Math.sin(latDiff / 2D)
                + Math.cos(fromLatRadians) * Math.cos(toLatRadians) * Math.sin(lonDiff / 2D) * Math.sin(lonDiff / 2D);
        double arc = 2D * Math.atan2(Math.sqrt(haversine), Math.sqrt(1D - haversine));
        return BigDecimal.valueOf(EARTH_RADIUS_METERS * arc).setScale(0, RoundingMode.HALF_UP);
    }

    private JsonNode readJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (IOException exception) {
            throw new IllegalStateException("地图服务响应解析失败", exception);
        }
    }

    private static RestTemplate createRestTemplate(MapProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeout = properties.getTimeoutMs() == null ? 3000 : properties.getTimeoutMs().intValue();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return new RestTemplate(requestFactory);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
