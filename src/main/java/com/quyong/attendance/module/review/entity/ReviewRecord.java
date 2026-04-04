package com.quyong.attendance.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("reviewRecord")
public class ReviewRecord {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("exceptionId")
    private Long exceptionId;

    @TableField("reviewUserId")
    private Long reviewUserId;

    @TableField("result")
    private String result;

    @TableField("comment")
    private String comment;

    @TableField("aiReviewSuggestion")
    private String aiReviewSuggestion;

    @TableField("similarCaseSummary")
    private String similarCaseSummary;

    @TableField("feedbackTag")
    private String feedbackTag;

    @TableField("strategyFeedback")
    private String strategyFeedback;

    @TableField("reviewTime")
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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
