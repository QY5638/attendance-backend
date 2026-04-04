package com.quyong.attendance.module.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.warning.entity.WarningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WarningRecordMapper extends BaseMapper<WarningRecord> {

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM warningRecord",
            "WHERE 1 = 1",
            "<if test='level != null and level != \"\"'> AND level = #{level}</if>",
            "<if test='status != null and status != \"\"'> AND status = #{status}</if>",
            "<if test='type != null and type != \"\"'> AND type = #{type}</if>",
            "</script>"
    })
    long countByQuery(@Param("level") String level,
                      @Param("status") String status,
                      @Param("type") String type);

    @Select({
            "<script>",
            "SELECT id, exceptionId, type, level, status, priorityScore, aiSummary, disposeSuggestion, decisionSource, sendTime",
            "FROM warningRecord",
            "WHERE 1 = 1",
            "<if test='level != null and level != \"\"'> AND level = #{level}</if>",
            "<if test='status != null and status != \"\"'> AND status = #{status}</if>",
            "<if test='type != null and type != \"\"'> AND type = #{type}</if>",
            "ORDER BY sendTime DESC, id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<WarningRecord> selectPageByQuery(@Param("level") String level,
                                          @Param("status") String status,
                                          @Param("type") String type,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    @Select("SELECT id, exceptionId, type, level, status, priorityScore, aiSummary, disposeSuggestion, decisionSource, sendTime FROM warningRecord WHERE exceptionId = #{exceptionId} LIMIT 1")
    WarningRecord selectByExceptionId(@Param("exceptionId") Long exceptionId);
}
