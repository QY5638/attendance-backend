package com.quyong.attendance.module.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.warning.entity.WarningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WarningRecordMapper extends BaseMapper<WarningRecord> {

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM warningRecord wr",
            "LEFT JOIN attendanceException ae ON wr.exceptionId = ae.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ae.userId = #{userId}</if>",
            "<if test='level != null and level != \"\"'> AND wr.level = #{level}</if>",
            "<if test='status != null and status != \"\"'> AND wr.status = #{status}</if>",
            "<if test='type != null and type != \"\"'> AND wr.type = #{type}</if>",
            "</script>"
    })
    long countByQuery(@Param("userId") Long userId,
                      @Param("level") String level,
                      @Param("status") String status,
                      @Param("type") String type);

    @Select({
            "<script>",
            "SELECT wr.id, wr.exceptionId, wr.type, wr.level, wr.status, wr.priorityScore, wr.aiSummary, wr.disposeSuggestion, wr.decisionSource, wr.sendTime",
            "FROM warningRecord wr",
            "LEFT JOIN attendanceException ae ON wr.exceptionId = ae.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ae.userId = #{userId}</if>",
            "<if test='level != null and level != \"\"'> AND wr.level = #{level}</if>",
            "<if test='status != null and status != \"\"'> AND wr.status = #{status}</if>",
            "<if test='type != null and type != \"\"'> AND wr.type = #{type}</if>",
            "ORDER BY CASE WHEN wr.status = 'UNPROCESSED' AND wr.sendTime &lt;= #{overdueCutoff} THEN 0 ELSE 1 END ASC,",
            "CASE WHEN wr.status = 'UNPROCESSED' THEN 0 ELSE 1 END ASC,",
            "CASE wr.level WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,",
            "wr.priorityScore DESC, wr.sendTime DESC, wr.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<WarningRecord> selectPageByQuery(@Param("userId") Long userId,
                                          @Param("level") String level,
                                          @Param("status") String status,
                                          @Param("type") String type,
                                          @Param("overdueCutoff") LocalDateTime overdueCutoff,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    @Select("SELECT id, exceptionId, type, level, status, priorityScore, aiSummary, disposeSuggestion, decisionSource, sendTime FROM warningRecord WHERE exceptionId = #{exceptionId} LIMIT 1")
    WarningRecord selectByExceptionId(@Param("exceptionId") Long exceptionId);

    @Select("SELECT id, exceptionId, type, level, status, priorityScore, aiSummary, disposeSuggestion, decisionSource, sendTime FROM warningRecord WHERE sendTime >= #{startTime} ORDER BY sendTime ASC, id ASC")
    List<WarningRecord> selectBySendTimeSince(@Param("startTime") LocalDateTime startTime);
}
