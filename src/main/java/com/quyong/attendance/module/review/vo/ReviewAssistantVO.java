package com.quyong.attendance.module.review.vo;

import java.math.BigDecimal;

public class ReviewAssistantVO {

    private String aiReviewSuggestion;
    private String similarCaseSummary;
    private String decisionReason;
    private BigDecimal confidenceScore;

    public String getAiReviewSuggestion() {
        return aiReviewSuggestion;
    }

    public void setAiReviewSuggestion(String aiReviewSuggestion) {
        this.aiReviewSuggestion = aiReviewSuggestion;
    }

    public String getSimilarCaseSummary() {
        return similarCaseSummary;
    }

    public void setSimilarCaseSummary(String similarCaseSummary) {
        this.similarCaseSummary = similarCaseSummary;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
