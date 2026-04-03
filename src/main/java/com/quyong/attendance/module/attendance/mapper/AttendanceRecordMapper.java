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

    @Select("SELECT COUNT(*) FROM attendanceRecord WHERE userId = #{userId} AND checkType = #{checkType} AND checkTime = #{checkTime}")
    long countSameRecord(@Param("userId") Long userId,
                         @Param("checkType") String checkType,
                         @Param("checkTime") LocalDateTime checkTime);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM attendanceRecord ar",
            "WHERE ar.userId = #{userId}",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countPersonalRecords(@Param("userId") Long userId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT ar.id, ar.userId, u.realName, ar.checkTime, ar.checkType, ar.deviceId, ar.location, ar.faceScore, ar.status",
            "FROM attendanceRecord ar",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE ar.userId = #{userId}",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "ORDER BY ar.checkTime DESC, ar.id DESC",
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
            "SELECT ar.id, ar.userId, u.realName, ar.checkTime, ar.checkType, ar.deviceId, ar.location, ar.faceScore, ar.status",
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
