package com.quyong.attendance.module.role.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.role.dto.RoleSaveDTO;
import com.quyong.attendance.module.role.entity.Role;
import com.quyong.attendance.module.role.mapper.RoleMapper;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RoleValidationSupport {

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;

    public RoleValidationSupport(RoleMapper roleMapper,
                                 UserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
    }

    public RoleSaveDTO validateForCreate(RoleSaveDTO saveDTO) {
        RoleSaveDTO target = saveDTO == null ? new RoleSaveDTO() : saveDTO;

        String code = normalize(target.getCode());
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色编码不能为空");
        }

        String name = normalize(target.getName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色名称不能为空");
        }

        ensureCodeUnique(code, null);

        target.setCode(code);
        target.setName(name);
        target.setDescription(normalize(target.getDescription()));
        target.setStatus(resolveStatus(target.getStatus()));
        return target;
    }

    public RoleSaveDTO validateForUpdate(RoleSaveDTO saveDTO) {
        RoleSaveDTO target = saveDTO == null ? new RoleSaveDTO() : saveDTO;
        if (target.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色不存在");
        }

        requireExistingRole(target.getId());

        String code = normalize(target.getCode());
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色编码不能为空");
        }

        String name = normalize(target.getName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色名称不能为空");
        }

        ensureCodeUnique(code, target.getId());

        target.setCode(code);
        target.setName(name);
        target.setDescription(normalize(target.getDescription()));
        target.setStatus(requireValidStatus(target.getStatus()));
        return target;
    }

    public Role requireExistingRole(Long id) {
        Role role = id == null ? null : roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色不存在");
        }
        return role;
    }

    public Role requireDeletableRole(Long id) {
        Role role = requireExistingRole(id);
        if (userMapper.countByRoleId(id) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色下存在关联用户，不能删除");
        }
        return role;
    }

    public Integer requireValidStatus(Integer status) {
        if (status == null || (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色状态不合法");
        }
        return status;
    }

    private Integer resolveStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        return requireValidStatus(status);
    }

    private void ensureCodeUnique(String code, Long currentId) {
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<Role>();
        queryWrapper.eq(Role::getCode, code);
        if (currentId != null) {
            queryWrapper.ne(Role::getId, currentId);
        }
        if (roleMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色编码已存在");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
