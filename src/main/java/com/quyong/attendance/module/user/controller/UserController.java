package com.quyong.attendance.module.user.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.dto.UserProfileUpdateDTO;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.service.UserService;
import com.quyong.attendance.module.user.vo.UserProfileVO;
import com.quyong.attendance.module.user.vo.UserVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<List<UserVO>> list(UserQueryDTO queryDTO) {
        return Result.success(userService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<UserVO> add(@RequestBody(required = false) UserSaveDTO saveDTO) {
        return Result.success(userService.add(saveDTO));
    }

    @PutMapping("/update")
    public Result<UserVO> update(@RequestBody(required = false) UserSaveDTO saveDTO) {
        return Result.success(userService.update(saveDTO));
    }

    @GetMapping("/me")
    public Result<UserProfileVO> me() {
        return Result.success(userService.currentProfile(currentAuthUser().getUserId()));
    }

    @PutMapping("/me")
    public Result<UserProfileVO> updateMe(@RequestBody(required = false) UserProfileUpdateDTO updateDTO) {
        return Result.success(userService.updateCurrentProfile(currentAuthUser().getUserId(), updateDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success(null);
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录状态已失效，请重新登录");
        }
        return (AuthUser) authentication.getPrincipal();
    }
}
