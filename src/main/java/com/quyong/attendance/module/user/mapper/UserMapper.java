package com.quyong.attendance.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, username, password, realName, gender, phone, deptId, roleId, status, createTime FROM `user` WHERE username = #{username} LIMIT 1")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM `user` WHERE deptId = #{deptId}")
    long countByDeptId(@Param("deptId") Long deptId);
}
