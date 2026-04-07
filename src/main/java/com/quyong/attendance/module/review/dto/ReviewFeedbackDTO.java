package com.quyong.attendance.module.review.dto;

public class ReviewFeedbackDTO {

    private Long reviewId;
    private String feedbackTag;
    private String strategyFeedback;

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
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
}
