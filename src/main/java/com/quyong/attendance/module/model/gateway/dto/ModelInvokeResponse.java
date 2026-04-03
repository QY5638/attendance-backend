package com.quyong.attendance.module.model.gateway.dto;

import java.math.BigDecimal;

public class ModelInvokeResponse {

    private String conclusion;
    private String riskLevel;
    private BigDecimal confidenceScore;
    private String decisionReason;
    private String reasonSummary;
    private String actionSuggestion;
    private String similarCaseSummary;
    private String rawResponse;

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public String getReasonSummary() {
        return reasonSummary;
    }

    public void setReasonSummary(String reasonSummary) {
        this.reasonSummary = reasonSummary;
    }

    public String getActionSuggestion() {
        return actionSuggestion;
    }

    public void setActionSuggestion(String actionSuggestion) {
        this.actionSuggestion = actionSuggestion;
    }

    public String getSimilarCaseSummary() {
        return similarCaseSummary;
    }

    public void setSimilarCaseSummary(String similarCaseSummary) {
        this.similarCaseSummary = similarCaseSummary;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
