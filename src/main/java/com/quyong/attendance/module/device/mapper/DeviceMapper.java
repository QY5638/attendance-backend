package com.quyong.attendance.module.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.device.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    @Select("SELECT COUNT(*) FROM attendanceRecord WHERE deviceId = #{deviceId}")
    long countAttendanceRecordByDeviceId(@Param("deviceId") String deviceId);

    @Select("SELECT COUNT(*) FROM attendanceRecord WHERE location = #{location}")
    long countAttendanceRecordByLocation(@Param("location") String location);
}
