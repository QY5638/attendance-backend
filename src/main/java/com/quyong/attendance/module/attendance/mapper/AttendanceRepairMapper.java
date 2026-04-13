package com.quyong.attendance.module.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.attendance.entity.AttendanceRepair;
import com.quyong.attendance.module.attendance.vo.AttendanceRepairVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AttendanceRepairMapper extends BaseMapper<AttendanceRepair> {

    @Select("SELECT COUNT(*) FROM attendanceRepair WHERE userId = #{userId} AND checkType = #{checkType} AND checkTime = #{checkTime} AND status = 'PENDING'")
    long countPendingRepair(@Param("userId") Long userId,
                            @Param("checkType") String checkType,
                            @Param("checkTime") LocalDateTime checkTime);

    @Select("SELECT COUNT(*) FROM attendanceRepair WHERE recordId = #{recordId} AND status = 'PENDING'")
    long countPendingByRecordId(@Param("recordId") Long recordId);

    @Select("SELECT id,userId,checkType,checkTime,repairReason,status,recordId,createTime FROM attendanceRepair WHERE id = #{id} FOR UPDATE")
    AttendanceRepair selectByIdForUpdate(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM attendanceRepair ar",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "WHERE 1 = 1",
            "<if test='keyword != null and keyword != \"\"'> AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.realName LIKE CONCAT('%', #{keyword}, '%'))</if>",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='checkType != null and checkType != \"\"'> AND ar.checkType = #{checkType}</if>",
            "<if test='status != null and status != \"\"'> AND ar.status = #{status}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countByQuery(@Param("keyword") String keyword,
                      @Param("userId") Long userId,
                      @Param("deptId") Long deptId,
                      @Param("checkType") String checkType,
                      @Param("status") String status,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT ar.id, ar.userId, u.realName, d.name AS deptName, ar.checkType, ar.checkTime, ar.repairReason, ar.status, ar.recordId, ar.createTime",
            "FROM attendanceRepair ar",
            "LEFT JOIN `user` u ON ar.userId = u.id",
            "LEFT JOIN department d ON u.deptId = d.id",
            "WHERE 1 = 1",
            "<if test='keyword != null and keyword != \"\"'> AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.realName LIKE CONCAT('%', #{keyword}, '%'))</if>",
            "<if test='userId != null'> AND ar.userId = #{userId}</if>",
            "<if test='deptId != null'> AND u.deptId = #{deptId}</if>",
            "<if test='checkType != null and checkType != \"\"'> AND ar.checkType = #{checkType}</if>",
            "<if test='status != null and status != \"\"'> AND ar.status = #{status}</if>",
            "<if test='startTime != null'> AND ar.checkTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ar.checkTime &lt;= #{endTime}</if>",
            "ORDER BY ar.createTime DESC, ar.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<AttendanceRepairVO> selectPageByQuery(@Param("keyword") String keyword,
                                               @Param("userId") Long userId,
                                               @Param("deptId") Long deptId,
                                               @Param("checkType") String checkType,
                                               @Param("status") String status,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);
}
