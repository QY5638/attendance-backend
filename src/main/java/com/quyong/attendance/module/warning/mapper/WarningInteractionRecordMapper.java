package com.quyong.attendance.module.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.warning.entity.WarningInteractionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WarningInteractionRecordMapper extends BaseMapper<WarningInteractionRecord> {

    @Select("SELECT id, warningId, exceptionId, senderUserId, senderRole, messageType, content, attachmentsJson, createTime FROM warningInteractionRecord WHERE warningId = #{warningId} ORDER BY createTime ASC, id ASC")
    List<WarningInteractionRecord> selectByWarningId(@Param("warningId") Long warningId);
}
