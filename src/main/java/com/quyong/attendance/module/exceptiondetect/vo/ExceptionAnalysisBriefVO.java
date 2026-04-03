package com.quyong.attendance.module.exceptiondetect.vo;

import java.math.BigDecimal;

public class ExceptionAnalysisBriefVO {

    private String modelConclusion;
    private String reasonSummary;
    private String actionSuggestion;
    private String similarCaseSummary;
    private String promptVersion;
    private BigDecimal confidenceScore;

    public String getModelConclusion() {
        return modelConclusion;
    }

    public void setModelConclusion(String modelConclusion) {
        this.modelConclusion = modelConclusion;
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

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
