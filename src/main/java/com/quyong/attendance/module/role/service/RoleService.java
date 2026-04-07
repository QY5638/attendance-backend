package com.quyong.attendance.module.role.service;

import com.quyong.attendance.module.role.dto.RoleQueryDTO;
import com.quyong.attendance.module.role.dto.RoleSaveDTO;
import com.quyong.attendance.module.role.vo.RoleVO;

import java.util.List;

public interface RoleService {

    List<RoleVO> list(RoleQueryDTO queryDTO);

    RoleVO add(RoleSaveDTO saveDTO);

    RoleVO update(RoleSaveDTO saveDTO);

    void delete(Long id);
}
