package com.quyong.attendance.module.map.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.module.map.config.MapProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class MapDistanceServiceTest {

    @Test
    void shouldApplyConfiguredTimeoutWhenCreatingDefaultRestTemplate() throws Exception {
        MapProperties properties = buildAmapProperties();
        properties.setTimeoutMs(Integer.valueOf(1500));
        MapDistanceService service = new MapDistanceService(new ObjectMapper(), properties);

        Field restTemplateField = MapDistanceService.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) restTemplateField.get(service);
        SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();

        Field connectTimeoutField = SimpleClientHttpRequestFactory.class.getDeclaredField("connectTimeout");
        connectTimeoutField.setAccessible(true);
        Field readTimeoutField = SimpleClientHttpRequestFactory.class.getDeclaredField("readTimeout");
        readTimeoutField.setAccessible(true);

        assertEquals(1500, connectTimeoutField.getInt(requestFactory));
        assertEquals(1500, readTimeoutField.getInt(requestFactory));
    }

    @Test
    void shouldRequestDistanceFromAmapWhenProviderConfigured() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        MapDistanceService service = new MapDistanceService(restTemplate, new ObjectMapper(), buildAmapProperties());

        server.expect(requestTo(containsString("https://restapi.amap.com/v3/distance")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(requestTo(containsString("origins=116.397128,39.916527")))
                .andExpect(requestTo(containsString("destination=121.473701,31.230416")))
                .andExpect(requestTo(containsString("key=test-map-key")))
                .andRespond(withSuccess("{\"status\":\"1\",\"results\":[{\"distance\":\"1068507\",\"duration\":\"0\"}],\"info\":\"OK\",\"infocode\":\"10000\"}", MediaType.APPLICATION_JSON));

        BigDecimal distance = service.calculateDistanceMeters(
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                new BigDecimal("121.473701"),
                new BigDecimal("31.230416")
        );

        assertEquals(new BigDecimal("1068507"), distance);
        server.verify();
    }

    @Test
    void shouldFallbackToGeodesicDistanceWhenAmapRequestFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        MapDistanceService service = new MapDistanceService(restTemplate, new ObjectMapper(), buildAmapProperties());

        server.expect(requestTo(containsString("https://restapi.amap.com/v3/distance")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        BigDecimal distance = service.calculateDistanceMeters(
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                new BigDecimal("121.473701"),
                new BigDecimal("31.230416")
        );

        assertTrue(distance.compareTo(new BigDecimal("1000000")) > 0);
        assertTrue(distance.compareTo(new BigDecimal("1200000")) < 0);
        server.verify();
    }

    @Test
    void shouldFallbackToGeodesicDistanceWhenMapProviderNotConfigured() {
        MapProperties properties = new MapProperties();
        MapDistanceService service = new MapDistanceService(new RestTemplate(), new ObjectMapper(), properties);

        BigDecimal distance = service.calculateDistanceMeters(
                new BigDecimal("116.397128"),
                new BigDecimal("39.916527"),
                new BigDecimal("121.473701"),
                new BigDecimal("31.230416")
        );

        assertTrue(distance.compareTo(new BigDecimal("1000000")) > 0);
        assertTrue(distance.compareTo(new BigDecimal("1200000")) < 0);
    }

    private MapProperties buildAmapProperties() {
        MapProperties properties = new MapProperties();
        properties.setProvider("amap");
        properties.setBaseUrl("https://restapi.amap.com/v3");
        properties.setApiKey("test-map-key");
        properties.setTimeoutMs(Integer.valueOf(3000));
        properties.setMultiLocationWindowMinutes(30);
        properties.setMultiLocationDistanceMeters(new BigDecimal("3000"));
        return properties;
    }
}
