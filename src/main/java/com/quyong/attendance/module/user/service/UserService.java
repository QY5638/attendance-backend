package com.quyong.attendance.module.user.service;

import com.quyong.attendance.module.user.dto.UserQueryDTO;
import com.quyong.attendance.module.user.dto.UserSaveDTO;
import com.quyong.attendance.module.user.vo.UserVO;

import java.util.List;

public interface UserService {

    List<UserVO> list(UserQueryDTO queryDTO);

    UserVO add(UserSaveDTO saveDTO);

    UserVO update(UserSaveDTO saveDTO);

    void delete(Long id);
}
