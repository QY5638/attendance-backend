package com.quyong.attendance.module.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.role.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT id, code, name, description, status FROM `role` WHERE id = #{id}")
    Role selectRoleById(@Param("id") Long id);
}
