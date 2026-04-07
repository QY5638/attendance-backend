package com.quyong.attendance.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import com.quyong.attendance.module.user.service.UserService;
import com.quyong.attendance.module.user.support.UserPasswordSupport;
import com.quyong.attendance.module.user.support.UserValidationSupport;
import com.quyong.attendance.module.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserValidationSupport userValidationSupport;
    private final UserPasswordSupport userPasswordSupport;

    public UserServiceImpl(UserMapper userMapper,
                           UserValidationSupport userValidationSupport,
                           UserPasswordSupport userPasswordSupport) {
        this.userMapper = userMapper;
        this.userValidationSupport = userValidationSupport;
        this.userPasswordSupport = userPasswordSupport;
    }

    @Override
    public List<UserVO> list(UserQueryDTO queryDTO) {
        String keyword = normalize(queryDTO == null ? null : queryDTO.getKeyword());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword));
        }
        if (queryDTO != null && queryDTO.getDeptId() != null) {
            queryWrapper.eq(User::getDeptId, queryDTO.getDeptId());
        }
        if (queryDTO != null && queryDTO.getStatus() != null) {
            queryWrapper.eq(User::getStatus, queryDTO.getStatus());
        }
        queryWrapper.orderByAsc(User::getId);

        return userMapper.selectList(queryWrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public UserVO add(UserSaveDTO saveDTO) {
        UserSaveDTO validatedSaveDTO = userValidationSupport.validateForCreate(saveDTO);

        User user = new User();
        user.setUsername(validatedSaveDTO.getUsername());
        user.setPassword(userPasswordSupport.encodeForCreate(validatedSaveDTO.getPassword()));
        user.setRealName(validatedSaveDTO.getRealName());
        user.setGender(validatedSaveDTO.getGender());
        user.setPhone(validatedSaveDTO.getPhone());
        user.setDeptId(validatedSaveDTO.getDeptId());
        user.setRoleId(validatedSaveDTO.getRoleId());
        user.setStatus(validatedSaveDTO.getStatus());
        userMapper.insert(user);

        return toVO(userMapper.selectById(user.getId()));
    }

    @Override
    public UserVO update(UserSaveDTO saveDTO) {
        User existingUser = userValidationSupport.requireExistingUser(saveDTO == null ? null : saveDTO.getId());
        UserSaveDTO validatedSaveDTO = userValidationSupport.validateForUpdate(saveDTO, existingUser.getId());

        existingUser.setUsername(validatedSaveDTO.getUsername());
        existingUser.setPassword(userPasswordSupport.resolvePasswordForUpdate(validatedSaveDTO.getPassword(), existingUser.getPassword()));
        existingUser.setRealName(validatedSaveDTO.getRealName());
        existingUser.setGender(validatedSaveDTO.getGender());
        existingUser.setPhone(validatedSaveDTO.getPhone());
        existingUser.setDeptId(validatedSaveDTO.getDeptId());
        existingUser.setRoleId(validatedSaveDTO.getRoleId());
        existingUser.setStatus(validatedSaveDTO.getStatus());
        userMapper.updateById(existingUser);

        return toVO(userMapper.selectById(existingUser.getId()));
    }

    @Override
    public void delete(Long id) {
        userValidationSupport.requireExistingUser(id);
        userMapper.deleteById(id);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setDeptId(user.getDeptId());
        vo.setRoleId(user.getRoleId());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
