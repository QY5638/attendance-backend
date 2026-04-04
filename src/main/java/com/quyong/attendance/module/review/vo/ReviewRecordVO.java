package com.quyong.attendance.module.review.vo;

import java.time.LocalDateTime;

public class ReviewRecordVO {

    private Long id;
    private Long exceptionId;
    private Long reviewUserId;
    private String reviewResult;
    private String reviewComment;
    private String aiReviewSuggestion;
    private String similarCaseSummary;
    private String feedbackTag;
    private String strategyFeedback;
    private LocalDateTime reviewTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long exceptionId) {
        this.exceptionId = exceptionId;
    }

    public Long getReviewUserId() {
        return reviewUserId;
    }

    public void setReviewUserId(Long reviewUserId) {
        this.reviewUserId = reviewUserId;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

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

    public String getFeedbackTag() {
        return feedbackTag;
    }

    public void setFeedbackTag(String feedbackTag) {
        this.feedbackTag = feedbackTag;
    }

    public String getStrategyFeedback() {
        return strategyFeedback;
    }

    public void setStrategyFeedback(String strategyFeedback) {
        this.strategyFeedback = strategyFeedback;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }
}
