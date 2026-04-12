package com.quyong.attendance.module.face.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class FaceLivenessCompleteDTO {

    private String sessionId;
    private String imageData;
    private Long startedAt;
    private Long completedAt;
    private Integer sampleCount;
    private Integer stableFaceFrames;
    private List<String> completedActions;
    private Map<String, BigDecimal> actionScores;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public Integer getStableFaceFrames() {
        return stableFaceFrames;
    }

    public void setStableFaceFrames(Integer stableFaceFrames) {
        this.stableFaceFrames = stableFaceFrames;
    }

    public List<String> getCompletedActions() {
        return completedActions;
    }

    public void setCompletedActions(List<String> completedActions) {
        this.completedActions = completedActions;
    }

    public Map<String, BigDecimal> getActionScores() {
        return actionScores;
    }

    public void setActionScores(Map<String, BigDecimal> actionScores) {
        this.actionScores = actionScores;
    }
}
