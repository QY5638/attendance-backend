package com.quyong.attendance.module.face.support;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryFaceLivenessStore implements FaceLivenessStore {

    private final Map<String, FaceLivenessSession> sessions = new ConcurrentHashMap<String, FaceLivenessSession>();
    private final Map<String, FaceLivenessProof> proofs = new ConcurrentHashMap<String, FaceLivenessProof>();

    @Override
    public void storeSession(FaceLivenessSession session, Duration ttl) {
        if (session.getExpireAt() == null) {
            session.setExpireAt(Instant.now().plus(ttl));
        }
        sessions.put(session.getSessionId(), session);
    }

    @Override
    public FaceLivenessSession getSession(String sessionId) {
        FaceLivenessSession session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        if (session.getExpireAt() == null || !session.getExpireAt().isAfter(Instant.now())) {
            sessions.remove(sessionId);
            return null;
        }
        return session;
    }

    @Override
    public void deleteSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    @Override
    public void storeProof(FaceLivenessProof proof, Duration ttl) {
        if (proof.getExpireAt() == null) {
            proof.setExpireAt(Instant.now().plus(ttl));
        }
        proofs.put(proof.getToken(), proof);
    }

    @Override
    public FaceLivenessProof getProof(String token) {
        FaceLivenessProof proof = proofs.get(token);
        if (proof == null) {
            return null;
        }
        if (proof.getExpireAt() == null || !proof.getExpireAt().isAfter(Instant.now())) {
            proofs.remove(token);
            return null;
        }
        return proof;
    }

    @Override
    public void deleteProof(String token) {
        if (token != null) {
            proofs.remove(token);
        }
    }
}
