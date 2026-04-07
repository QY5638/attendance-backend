package com.quyong.attendance.module.department.service;

import com.quyong.attendance.module.department.dto.DepartmentQueryDTO;
import com.quyong.attendance.module.department.dto.DepartmentSaveDTO;
import com.quyong.attendance.module.department.vo.DepartmentVO;

import java.util.List;

public interface DepartmentService {

    List<DepartmentVO> list(DepartmentQueryDTO queryDTO);

    DepartmentVO add(DepartmentSaveDTO saveDTO);

    DepartmentVO update(DepartmentSaveDTO saveDTO);

    void delete(Long id);
}
