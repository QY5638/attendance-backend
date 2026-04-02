package com.quyong.attendance.module.auth.service;

import com.quyong.attendance.module.auth.dto.LoginDTO;
import com.quyong.attendance.module.auth.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO loginDTO);
}
