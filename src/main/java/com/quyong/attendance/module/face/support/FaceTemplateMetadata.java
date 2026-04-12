package com.quyong.attendance.module.face.support;

import java.math.BigDecimal;

public class FaceTemplateMetadata {

    private String provider;
    private Integer version;
    private String regionId;
    private String dbName;
    private String entityId;
    private String faceId;
    private String imageId;
    private String subject;
    private String requestId;
    private BigDecimal qualityScore;
    private BigDecimal livenessScore;
    private String referenceDigest;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getFaceId() {
        return faceId;
    }

    public void setFaceId(String faceId) {
        this.faceId = faceId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
    }

    public BigDecimal getLivenessScore() {
        return livenessScore;
    }

    public void setLivenessScore(BigDecimal livenessScore) {
        this.livenessScore = livenessScore;
    }

    public String getReferenceDigest() {
        return referenceDigest;
    }

    public void setReferenceDigest(String referenceDigest) {
        this.referenceDigest = referenceDigest;
    }
}
