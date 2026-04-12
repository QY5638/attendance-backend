package com.quyong.attendance.module.face.support;

import java.time.Duration;

public interface FaceLivenessStore {

    void storeSession(FaceLivenessSession session, Duration ttl);

    FaceLivenessSession getSession(String sessionId);

    void deleteSession(String sessionId);

    void storeProof(FaceLivenessProof proof, Duration ttl);

    FaceLivenessProof getProof(String token);

    void deleteProof(String token);
}
