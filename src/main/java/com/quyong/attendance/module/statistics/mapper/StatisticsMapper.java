package com.quyong.attendance.module.statistics.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM attendanceRecord ar",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countRecords(@Param("userId") Long userId,
                      @Param("deptId") Long deptId,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countExceptions(@Param("userId") Long userId,
                         @Param("deptId") Long deptId,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN exceptionAnalysis ea ON ea.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countAnalyses(@Param("userId") Long userId,
                       @Param("deptId") Long deptId,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN warningRecord wr ON wr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countWarnings(@Param("userId") Long userId,
                       @Param("deptId") Long deptId,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN reviewRecord rr ON rr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countReviews(@Param("userId") Long userId,
                      @Param("deptId") Long deptId,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COUNT(DISTINCT ae.id)",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN warningRecord wr ON wr.exceptionId = ae.id",
            "INNER JOIN reviewRecord rr ON rr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countClosedLoops(@Param("userId") Long userId,
                          @Param("deptId") Long deptId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT ae.type AS label, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY ae.type",
            "ORDER BY COUNT(*) DESC, ae.type ASC",
            "</script>"
    })
    List<Map<String, Object>> selectExceptionTypeDistribution(@Param("userId") Long userId,
                                                              @Param("deptId") Long deptId,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT ae.riskLevel AS label, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY ae.riskLevel",
            "ORDER BY COUNT(*) DESC, ae.riskLevel ASC",
            "</script>"
    })
    List<Map<String, Object>> selectRiskLevelDistribution(@Param("userId") Long userId,
                                                          @Param("deptId") Long deptId,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT wr.status AS label, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN warningRecord wr ON wr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY wr.status",
            "ORDER BY COUNT(*) DESC, wr.status ASC",
            "</script>"
    })
    List<Map<String, Object>> selectWarningStatusDistribution(@Param("userId") Long userId,
                                                              @Param("deptId") Long deptId,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT rr.result AS label, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN reviewRecord rr ON rr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY rr.result",
            "ORDER BY COUNT(*) DESC, rr.result ASC",
            "</script>"
    })
    List<Map<String, Object>> selectReviewResultDistribution(@Param("userId") Long userId,
                                                             @Param("deptId") Long deptId,
                                                             @Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT CAST(ar.checkTime AS DATE) AS bucket, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY CAST(ar.checkTime AS DATE)",
            "ORDER BY CAST(ar.checkTime AS DATE) ASC",
            "</script>"
    })
    List<Map<String, Object>> selectRecordTrendRows(@Param("deptId") Long deptId,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT CAST(ar.checkTime AS DATE) AS bucket, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY CAST(ar.checkTime AS DATE)",
            "ORDER BY CAST(ar.checkTime AS DATE) ASC",
            "</script>"
    })
    List<Map<String, Object>> selectExceptionTrendRows(@Param("deptId") Long deptId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT CAST(ar.checkTime AS DATE) AS bucket, ae.type AS type, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY CAST(ar.checkTime AS DATE), ae.type",
            "ORDER BY CAST(ar.checkTime AS DATE) ASC, COUNT(*) DESC, ae.type ASC",
            "</script>"
    })
    List<Map<String, Object>> selectExceptionTypeTrendRows(@Param("deptId") Long deptId,
                                                           @Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT CAST(ar.checkTime AS DATE) AS bucket, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN exceptionAnalysis ea ON ea.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY CAST(ar.checkTime AS DATE)",
            "ORDER BY CAST(ar.checkTime AS DATE) ASC",
            "</script>"
    })
    List<Map<String, Object>> selectAnalysisTrendRows(@Param("deptId") Long deptId,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT CAST(ar.checkTime AS DATE) AS bucket, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN warningRecord wr ON wr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY CAST(ar.checkTime AS DATE)",
            "ORDER BY CAST(ar.checkTime AS DATE) ASC",
            "</script>"
    })
    List<Map<String, Object>> selectWarningTrendRows(@Param("deptId") Long deptId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT CAST(ar.checkTime AS DATE) AS bucket, COUNT(*) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN reviewRecord rr ON rr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY CAST(ar.checkTime AS DATE)",
            "ORDER BY CAST(ar.checkTime AS DATE) ASC",
            "</script>"
    })
    List<Map<String, Object>> selectReviewTrendRows(@Param("deptId") Long deptId,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT CAST(ar.checkTime AS DATE) AS bucket, COUNT(DISTINCT ae.id) AS total",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "INNER JOIN warningRecord wr ON wr.exceptionId = ae.id",
            "INNER JOIN reviewRecord rr ON rr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "GROUP BY CAST(ar.checkTime AS DATE)",
            "ORDER BY CAST(ar.checkTime AS DATE) ASC",
            "</script>"
    })
    List<Map<String, Object>> selectClosedLoopTrendRows(@Param("deptId") Long deptId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN decisionTrace dt ON dt.businessType = 'ATTENDANCE_EXCEPTION' AND dt.businessId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE dt.id IS NULL",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countMissingDecisionTrace(@Param("userId") Long userId,
                                   @Param("deptId") Long deptId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN modelCallLog ml ON ml.businessType = 'EXCEPTION_ANALYSIS' AND ml.businessId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE ae.sourceType IN ('MODEL', 'MODEL_FALLBACK') AND ml.id IS NULL",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countMissingModelLog(@Param("userId") Long userId,
                              @Param("deptId") Long deptId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COALESCE(NULLIF(wr.aiSummary, ''), NULLIF(ea.reasonSummary, ''), NULLIF(ea.inputSummary, ''), NULLIF(ae.description, ''))",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN exceptionAnalysis ea ON ea.exceptionId = ae.id",
            "LEFT JOIN warningRecord wr ON wr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "AND (NULLIF(wr.aiSummary, '') IS NOT NULL OR NULLIF(ea.reasonSummary, '') IS NOT NULL OR NULLIF(ea.inputSummary, '') IS NOT NULL OR NULLIF(ae.description, '') IS NOT NULL)",
            "ORDER BY COALESCE(wr.sendTime, ea.createTime, ae.createTime) DESC, ae.id DESC",
            "LIMIT 1",
            "</script>"
    })
    String selectLatestSummaryText(@Param("userId") Long userId,
                                   @Param("deptId") Long deptId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COALESCE(NULLIF(ae.description, ''), NULLIF(ea.reasonSummary, ''), NULLIF(wr.aiSummary, ''))",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN exceptionAnalysis ea ON ea.exceptionId = ae.id",
            "LEFT JOIN warningRecord wr ON wr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "AND (NULLIF(ae.description, '') IS NOT NULL OR NULLIF(ea.reasonSummary, '') IS NOT NULL OR NULLIF(wr.aiSummary, '') IS NOT NULL)",
            "ORDER BY COALESCE(wr.sendTime, ea.createTime, ae.createTime) DESC, ae.id DESC",
            "LIMIT 1",
            "</script>"
    })
    String selectLatestHighlightText(@Param("userId") Long userId,
                                     @Param("deptId") Long deptId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT COALESCE(NULLIF(rr.aiReviewSuggestion, ''), NULLIF(wr.disposeSuggestion, ''), NULLIF(ea.actionSuggestion, ''), NULLIF(ea.suggestion, ''))",
            "FROM attendanceRecord ar",
            "INNER JOIN attendanceException ae ON ae.recordId = ar.id",
            "LEFT JOIN exceptionAnalysis ea ON ea.exceptionId = ae.id",
            "LEFT JOIN warningRecord wr ON wr.exceptionId = ae.id",
            "LEFT JOIN reviewRecord rr ON rr.exceptionId = ae.id",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "AND (NULLIF(rr.aiReviewSuggestion, '') IS NOT NULL OR NULLIF(wr.disposeSuggestion, '') IS NOT NULL OR NULLIF(ea.actionSuggestion, '') IS NOT NULL OR NULLIF(ea.suggestion, '') IS NOT NULL)",
            "ORDER BY COALESCE(rr.reviewTime, wr.sendTime, ea.createTime, ae.createTime) DESC, ae.id DESC",
            "LIMIT 1",
            "</script>"
    })
    String selectLatestManageSuggestionText(@Param("userId") Long userId,
                                            @Param("deptId") Long deptId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
}
