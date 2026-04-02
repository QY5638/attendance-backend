package com.quyong.attendance.module.department.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.department.dto.DepartmentQueryDTO;
import com.quyong.attendance.module.department.dto.DepartmentSaveDTO;
import com.quyong.attendance.module.department.entity.Department;
import com.quyong.attendance.module.department.mapper.DepartmentMapper;
import com.quyong.attendance.module.department.service.DepartmentService;
import com.quyong.attendance.module.department.vo.DepartmentVO;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper, UserMapper userMapper) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<DepartmentVO> list(DepartmentQueryDTO queryDTO) {
        String keyword = queryDTO == null ? null : normalize(queryDTO.getKeyword());

        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<Department>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Department::getName, keyword);
        }
        queryWrapper.orderByAsc(Department::getId);

        return departmentMapper.selectList(queryWrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentVO add(DepartmentSaveDTO saveDTO) {
        String name = normalize(saveDTO == null ? null : saveDTO.getName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门名称不能为空");
        }

        ensureNameUnique(name, null);

        Department department = new Department();
        department.setName(name);
        department.setDescription(saveDTO == null ? null : saveDTO.getDescription());
        departmentMapper.insert(department);

        return toVO(department);
    }

    @Override
    public DepartmentVO update(DepartmentSaveDTO saveDTO) {
        Long id = saveDTO == null ? null : saveDTO.getId();
        Department department = id == null ? null : departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门不存在");
        }

        String name = normalize(saveDTO.getName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门名称不能为空");
        }

        ensureNameUnique(name, id);

        department.setName(name);
        department.setDescription(saveDTO.getDescription());
        departmentMapper.updateById(department);
        return toVO(department);
    }

    @Override
    public void delete(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门不存在");
        }

        if (userMapper.countByDeptId(id) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门下存在关联用户，不能删除");
        }

        departmentMapper.deleteById(id);
    }

    private void ensureNameUnique(String name, Long currentId) {
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<Department>();
        queryWrapper.eq(Department::getName, name);
        if (currentId != null) {
            queryWrapper.ne(Department::getId, currentId);
        }

        if (departmentMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门名称已存在");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private DepartmentVO toVO(Department department) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(department.getId());
        vo.setName(department.getName());
        vo.setDescription(department.getDescription());
        return vo;
    }
}
