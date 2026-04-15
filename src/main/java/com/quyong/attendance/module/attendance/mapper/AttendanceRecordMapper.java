package com.quyong.attendance.module.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.attendance.entity.AttendanceRecord;
import com.quyong.attendance.module.attendance.vo.AttendanceRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {

    @Select("SELECT id,userId,checkTime,checkType,deviceId,deviceInfo,terminalId,ipAddr,location,clientLongitude,clientLatitude,longitude,latitude,faceScore,status,createTime FROM attendanceRecord WHERE userId = #{userId} AND (checkTime < #{checkTime} OR (checkTime = #{checkTime} AND id <> #{recordId})) ORDER BY checkTime DESC, id DESC LIMIT 1")
    AttendanceRecord selectLatestBefore(@Param("userId") Long userId,
                                        @Param("checkTime") LocalDateTime checkTime,
                                        @Param("recordId") Long recordId);

    @Select("SELECT COUNT(*) FROM attendanceRecord WHERE userId = #{userId} AND checkType = #{checkType} AND checkTime = #{checkTime}")
    long countSameRecord(@Param("userId") Long userId,
                         @Param("checkType") String checkType,
                         @Param("checkTime") LocalDateTime checkTime);

    @Select("SELECT id FROM attendanceRecord WHERE userId = #{userId} AND checkType = #{checkType} AND checkTime = #{checkTime} ORDER BY id DESC LIMIT 1")
    Long selectSameRecordId(@Param("userId") Long userId,
                            @Param("checkType") String checkType,
                            @Param("checkTime") LocalDateTime checkTime);

    @Select("SELECT id,userId,checkTime,checkType,deviceId,deviceInfo,terminalId,ipAddr,location,clientLongitude,clientLatitude,longitude,latitude,faceScore,status,createTime FROM attendanceRecord WHERE userId = #{userId} ORDER BY checkTime DESC, id DESC LIMIT 1")
    AttendanceRecord selectLatestByUser(@Param("userId") Long userId);

    @Select({
            "<script>",
            "SELECT (",
            "  (SELECT COUNT(*) FROM attendanceRecord ar",
            "   WHERE ar.userId = #{userId}",
            "   <if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "   <if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "  )",
            "  +",
            "  (SELECT COUNT(*) FROM attendanceException ae",
            "   WHERE ae.userId = #{userId}",
            "     AND ae.type = 'ABSENT'",
            "     AND ae.recordId IS NULL",
            "     AND (ae.processStatus &lt;&gt; 'REVIEWED' OR EXISTS (SELECT 1 FROM reviewRecord rr WHERE rr.exceptionId = ae.id))",
            "   <if test='startTime != null'> AND ae.createTime &gt;= #{startTime}</if>",
            "   <if test='endTime != null'> AND ae.createTime &lt;= #{endTime}</if>",
            "  )",
            "  +",
            "  (SELECT COUNT(*) FROM attendanceException ae",
            "   WHERE ae.userId = #{userId}",
            "     AND ae.type = 'MISSING_CHECKOUT'",
            "     AND ae.recordId IS NULL",
            "     AND (ae.processStatus &lt;&gt; 'REVIEWED' OR EXISTS (SELECT 1 FROM reviewRecord rr WHERE rr.exceptionId = ae.id))",
            "   <if test='startTime != null'> AND ae.createTime &gt;= #{startTime}</if>",
            "   <if test='endTime != null'> AND ae.createTime &lt;= #{endTime}</if>",
            "  )",
            ")",
            "</script>"
    })
    long countPersonalRecords(@Param("userId") Long userId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT merged.id, merged.userId, merged.realName, merged.checkTime, merged.checkType, merged.deviceId, merged.deviceInfo, merged.terminalId, merged.location, merged.faceScore, merged.status, merged.exceptionType, merged.exceptionProcessStatus, merged.repairStatus, merged.reviewResult",
            "FROM (",
            "  SELECT ar.id, ar.userId, u.realName, ar.checkTime, ar.checkType, ar.deviceId, ar.deviceInfo, ar.terminalId, ar.location, ar.faceScore, CASE WHEN (SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = (SELECT ae.id FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) = 'REJECTED' THEN 'NORMAL' WHEN (SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = (SELECT ae.id FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) = 'CONFIRMED' THEN 'ABNORMAL' ELSE ar.status END AS status,",
            "         (SELECT ae.type FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) AS exceptionType,",
            "         (SELECT ae.processStatus FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) AS exceptionProcessStatus,",
            "         (SELECT apr.status FROM attendanceRepair apr WHERE apr.recordId = ar.id ORDER BY apr.id DESC LIMIT 1) AS repairStatus,",
            "         (SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = (SELECT ae.id FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) AS reviewResult",
            "  FROM attendanceRecord ar",
            "  LEFT JOIN `user` u ON ar.userId = u.id",
            "  WHERE ar.userId = #{userId}",
            "  <if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "  <if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "  UNION ALL",
            "  SELECT ae.id, ae.userId, u.realName, ae.createTime AS checkTime, 'IN' AS checkType, NULL AS deviceId, '系统缺勤判定' AS deviceInfo, NULL AS terminalId, NULL AS location, NULL AS faceScore, CASE WHEN ae.processStatus = 'REVIEWED' THEN CASE WHEN (SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = ae.id ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) = 'REJECTED' THEN 'NORMAL' ELSE 'ABNORMAL' END ELSE 'ABSENT' END AS status,",
            "         'ABSENT' AS exceptionType,",
            "         ae.processStatus AS exceptionProcessStatus,",
            "         (SELECT apr.status FROM attendanceRepair apr",
            "          WHERE apr.userId = ae.userId",
            "            AND apr.checkType = 'IN'",
            "            AND DATE(apr.checkTime) = DATE(ae.createTime)",
            "          ORDER BY apr.createTime DESC, apr.id DESC",
            "          LIMIT 1) AS repairStatus,",
            "         (SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = ae.id ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) AS reviewResult",
            "  FROM attendanceException ae",
            "  LEFT JOIN `user` u ON ae.userId = u.id",
            "  WHERE ae.userId = #{userId}",
            "    AND ae.type = 'ABSENT'",
            "    AND ae.recordId IS NULL",
            "    AND (ae.processStatus &lt;&gt; 'REVIEWED' OR EXISTS (SELECT 1 FROM reviewRecord rr WHERE rr.exceptionId = ae.id))",
            "  <if test='startTime != null'> AND ae.createTime &gt;= #{startTime}</if>",
            "  <if test='endTime != null'> AND ae.createTime &lt;= #{endTime}</if>",
            "  UNION ALL",
            "  SELECT ae.id, ae.userId, u.realName, ae.createTime AS checkTime, 'OUT' AS checkType, NULL AS deviceId, '系统下班缺卡判定' AS deviceInfo, NULL AS terminalId, NULL AS location, NULL AS faceScore, CASE WHEN ae.processStatus = 'REVIEWED' THEN CASE WHEN (SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = ae.id ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) = 'REJECTED' THEN 'NORMAL' ELSE 'ABNORMAL' END ELSE 'MISSING_CHECKOUT' END AS status,",
            "         'MISSING_CHECKOUT' AS exceptionType,",
            "         ae.processStatus AS exceptionProcessStatus,",
            "         (SELECT apr.status FROM attendanceRepair apr",
            "          WHERE apr.userId = ae.userId",
            "            AND apr.checkType = 'OUT'",
            "            AND DATE(apr.checkTime) = DATE(ae.createTime)",
            "          ORDER BY apr.createTime DESC, apr.id DESC",
            "          LIMIT 1) AS repairStatus,",
            "         (SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = ae.id ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) AS reviewResult",
            "  FROM attendanceException ae",
            "  LEFT JOIN `user` u ON ae.userId = u.id",
            "  WHERE ae.userId = #{userId}",
            "    AND ae.type = 'MISSING_CHECKOUT'",
            "    AND ae.recordId IS NULL",
            "    AND (ae.processStatus &lt;&gt; 'REVIEWED' OR EXISTS (SELECT 1 FROM reviewRecord rr WHERE rr.exceptionId = ae.id))",
            "  <if test='startTime != null'> AND ae.createTime &gt;= #{startTime}</if>",
            "  <if test='endTime != null'> AND ae.createTime &lt;= #{endTime}</if>",
            ") merged",
            "ORDER BY merged.checkTime DESC, merged.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<AttendanceRecordVO> selectPersonalRecords(@Param("userId") Long userId,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime,
                                                   @Param("limit") int limit,
                                                   @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM attendanceRecord ar",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='checkType != null and checkType != \"\"'> AND ar.checkType = #{checkType}</if>",
            "<if test='status != null and status != \"\"'> AND ar.status = #{status}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countAdminRecords(@Param("userId") Long userId,
                           @Param("deptId") Long deptId,
                           @Param("checkType") String checkType,
                           @Param("status") String status,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT ar.id, ar.userId, u.realName, ar.checkTime, ar.checkType, ar.deviceId, ar.deviceInfo, ar.terminalId, ar.location, ar.faceScore, CASE WHEN (SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = (SELECT ae.id FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) = 'REJECTED' THEN 'NORMAL' WHEN (SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = (SELECT ae.id FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) = 'CONFIRMED' THEN 'ABNORMAL' ELSE ar.status END AS status,",
            "(SELECT ae.type FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) AS exceptionType,",
            "(SELECT ae.processStatus FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) AS exceptionProcessStatus,",
            "(SELECT apr.status FROM attendanceRepair apr WHERE apr.recordId = ar.id ORDER BY apr.id DESC LIMIT 1) AS repairStatus,",
            "(SELECT rr.result FROM reviewRecord rr WHERE rr.exceptionId = (SELECT ae.id FROM attendanceException ae WHERE ae.recordId = ar.id ORDER BY ae.id DESC LIMIT 1) ORDER BY rr.reviewTime DESC, rr.id DESC LIMIT 1) AS reviewResult",
            "FROM attendanceRecord ar",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='checkType != null and checkType != \"\"'> AND ar.checkType = #{checkType}</if>",
            "<if test='status != null and status != \"\"'> AND ar.status = #{status}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "ORDER BY ar.checkTime DESC, ar.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<AttendanceRecordVO> selectAdminRecords(@Param("userId") Long userId,
                                                @Param("deptId") Long deptId,
                                                @Param("checkType") String checkType,
                                                @Param("status") String status,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("limit") int limit,
                                                @Param("offset") int offset);
}
