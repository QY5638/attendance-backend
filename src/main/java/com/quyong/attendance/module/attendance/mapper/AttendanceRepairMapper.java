package com.quyong.attendance.module.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.attendance.entity.AttendanceRepair;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface AttendanceRepairMapper extends BaseMapper<AttendanceRepair> {

    @Select("SELECT COUNT(*) FROM attendanceRepair WHERE userId = #{userId} AND checkType = #{checkType} AND checkTime = #{checkTime} AND status = 'PENDING'")
    long countPendingRepair(@Param("userId") Long userId,
                            @Param("checkType") String checkType,
                            @Param("checkTime") LocalDateTime checkTime);
}
