package com.quyong.attendance.module.face.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("faceFeature")
public class FaceFeature {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("userId")
    private Long userId;

    @TableField("featureData")
    private String featureData;

    @TableField("featureHash")
    private String featureHash;

    @TableField("encryptFlag")
    private Integer encryptFlag;

    @TableField("createTime")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFeatureData() {
        return featureData;
    }

    public void setFeatureData(String featureData) {
        this.featureData = featureData;
    }

    public String getFeatureHash() {
        return featureHash;
    }

    public void setFeatureHash(String featureHash) {
        this.featureHash = featureHash;
    }

    public Integer getEncryptFlag() {
        return encryptFlag;
    }

    public void setEncryptFlag(Integer encryptFlag) {
        this.encryptFlag = encryptFlag;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
