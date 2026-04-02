package com.quyong.attendance.module.user.support;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.department.mapper.DepartmentMapper;
import com.quyong.attendance.module.role.mapper.RoleMapper;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserValidationSupport {

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;

    public UserValidationSupport(UserMapper userMapper,
                                 DepartmentMapper departmentMapper,
                                 RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
    }

    public UserSaveDTO validateForCreate(UserSaveDTO saveDTO) {
        UserSaveDTO target = saveDTO == null ? new UserSaveDTO() : saveDTO;

        String username = normalize(target.getUsername());
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }

        String realName = normalize(target.getRealName());
        if (!StringUtils.hasText(realName)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "姓名不能为空");
        }

        ensureUsernameUnique(username);
        ensureDepartmentExists(target.getDeptId());
        ensureRoleExists(target.getRoleId());

        target.setUsername(username);
        target.setRealName(realName);
        target.setPhone(normalize(target.getPhone()));
        target.setStatus(resolveStatus(target.getStatus()));
        return target;
    }

    public UserSaveDTO validateForUpdate(UserSaveDTO saveDTO, Long currentUserId) {
        UserSaveDTO target = saveDTO == null ? new UserSaveDTO() : saveDTO;

        String username = normalize(target.getUsername());
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }

        String realName = normalize(target.getRealName());
        if (!StringUtils.hasText(realName)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "姓名不能为空");
        }

        ensureUsernameUnique(username, currentUserId);
        ensureDepartmentExists(target.getDeptId());
        ensureRoleExists(target.getRoleId());

        target.setId(currentUserId);
        target.setUsername(username);
        target.setRealName(realName);
        target.setPhone(normalize(target.getPhone()));
        target.setStatus(requireValidStatus(target.getStatus()));
        return target;
    }

    public void ensureUsernameUnique(String username) {
        ensureUsernameUnique(username, null);
    }

    public void ensureUsernameUnique(String username, Long currentUserId) {
        User user = userMapper.selectByUsername(username);
        if (user != null && !user.getId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");
        }
    }

    public User requireExistingUser(Long id) {
        User user = id == null ? null : userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户不存在");
        }
        return user;
    }

    public Integer requireValidStatus(Integer status) {
        if (status == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户状态不合法");
        }
        return validateStatus(status);
    }

    public void ensureDepartmentExists(Long deptId) {
        if (deptId == null || departmentMapper.selectById(deptId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门不存在");
        }
    }

    public void ensureRoleExists(Long roleId) {
        if (roleId == null || roleMapper.selectRoleById(roleId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色不存在");
        }
    }

    private Integer resolveStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        return requireValidStatus(status);
    }

    private Integer validateStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户状态不合法");
        }
        return status;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
