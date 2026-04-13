package com.quyong.attendance.module.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("warningInteractionRecord")
public class WarningInteractionRecord {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("warningId")
    private Long warningId;

    @TableField("exceptionId")
    private Long exceptionId;

    @TableField("senderUserId")
    private Long senderUserId;

    @TableField("senderRole")
    private String senderRole;

    @TableField("messageType")
    private String messageType;

    @TableField("content")
    private String content;

    @TableField("attachmentsJson")
    private String attachmentsJson;

    @TableField("createTime")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarningId() {
        return warningId;
    }

    public void setWarningId(Long warningId) {
        this.warningId = warningId;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long exceptionId) {
        this.exceptionId = exceptionId;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(Long senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAttachmentsJson() {
        return attachmentsJson;
    }

    public void setAttachmentsJson(String attachmentsJson) {
        this.attachmentsJson = attachmentsJson;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
