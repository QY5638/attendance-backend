package com.quyong.attendance.module.face.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@Profile("!test")
public class RedisFaceLivenessStore implements FaceLivenessStore {

    private static final String SESSION_PREFIX = "face:liveness:session:";
    private static final String PROOF_PREFIX = "face:liveness:proof:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisFaceLivenessStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void storeSession(FaceLivenessSession session, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(buildSessionKey(session.getSessionId()), objectMapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("活体会话序列化失败", exception);
        }
    }

    @Override
    public FaceLivenessSession getSession(String sessionId) {
        String value = stringRedisTemplate.opsForValue().get(buildSessionKey(sessionId));
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, FaceLivenessSession.class);
        } catch (IOException exception) {
            stringRedisTemplate.delete(buildSessionKey(sessionId));
            return null;
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        if (sessionId != null) {
            stringRedisTemplate.delete(buildSessionKey(sessionId));
        }
    }

    @Override
    public void storeProof(FaceLivenessProof proof, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(buildProofKey(proof.getToken()), objectMapper.writeValueAsString(proof), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("活体证明序列化失败", exception);
        }
    }

    @Override
    public FaceLivenessProof getProof(String token) {
        String value = stringRedisTemplate.opsForValue().get(buildProofKey(token));
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, FaceLivenessProof.class);
        } catch (IOException exception) {
            stringRedisTemplate.delete(buildProofKey(token));
            return null;
        }
    }

    @Override
    public void deleteProof(String token) {
        if (token != null) {
            stringRedisTemplate.delete(buildProofKey(token));
        }
    }

    private String buildSessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private String buildProofKey(String token) {
        return PROOF_PREFIX + token;
    }
}
