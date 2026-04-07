package com.quyong.attendance.module.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.module.role.dto.RoleQueryDTO;
import com.quyong.attendance.module.role.dto.RoleSaveDTO;
import com.quyong.attendance.module.role.entity.Role;
import com.quyong.attendance.module.role.mapper.RoleMapper;
import com.quyong.attendance.module.role.service.RoleService;
import com.quyong.attendance.module.role.support.RoleValidationSupport;
import com.quyong.attendance.module.role.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RoleValidationSupport roleValidationSupport;

    public RoleServiceImpl(RoleMapper roleMapper,
                           RoleValidationSupport roleValidationSupport) {
        this.roleMapper = roleMapper;
        this.roleValidationSupport = roleValidationSupport;
    }

    @Override
    public List<RoleVO> list(RoleQueryDTO queryDTO) {
        String keyword = normalize(queryDTO == null ? null : queryDTO.getKeyword());

        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<Role>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(Role::getCode, keyword)
                    .or()
                    .like(Role::getName, keyword)
                    .or()
                    .like(Role::getDescription, keyword));
        }
        if (queryDTO != null && queryDTO.getStatus() != null) {
            queryWrapper.eq(Role::getStatus, queryDTO.getStatus());
        }
        queryWrapper.orderByAsc(Role::getId);

        return roleMapper.selectList(queryWrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleVO add(RoleSaveDTO saveDTO) {
        RoleSaveDTO validatedSaveDTO = roleValidationSupport.validateForCreate(saveDTO);

        Role role = new Role();
        role.setCode(validatedSaveDTO.getCode());
        role.setName(validatedSaveDTO.getName());
        role.setDescription(validatedSaveDTO.getDescription());
        role.setStatus(validatedSaveDTO.getStatus());
        roleMapper.insert(role);
        return toVO(roleMapper.selectById(role.getId()));
    }

    @Override
    public RoleVO update(RoleSaveDTO saveDTO) {
        RoleSaveDTO validatedSaveDTO = roleValidationSupport.validateForUpdate(saveDTO);
        Role role = roleValidationSupport.requireExistingRole(validatedSaveDTO.getId());

        role.setCode(validatedSaveDTO.getCode());
        role.setName(validatedSaveDTO.getName());
        role.setDescription(validatedSaveDTO.getDescription());
        role.setStatus(validatedSaveDTO.getStatus());
        roleMapper.updateById(role);
        return toVO(roleMapper.selectById(role.getId()));
    }

    @Override
    public void delete(Long id) {
        Role role = roleValidationSupport.requireDeletableRole(id);
        roleMapper.deleteById(role.getId());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private RoleVO toVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        return vo;
    }
}
