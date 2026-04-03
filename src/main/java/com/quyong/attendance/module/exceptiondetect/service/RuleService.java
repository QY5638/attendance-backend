package com.quyong.attendance.module.exceptiondetect.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.exceptiondetect.dto.RuleQueryDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleSaveDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleStatusDTO;
import com.quyong.attendance.module.exceptiondetect.entity.Rule;
import com.quyong.attendance.module.exceptiondetect.vo.RuleVO;

public interface RuleService {

    PageResult<RuleVO> list(RuleQueryDTO queryDTO);

    void add(RuleSaveDTO saveDTO);

    void update(RuleSaveDTO saveDTO);

    void updateStatus(RuleStatusDTO statusDTO);

    Rule getEnabledRule();
}
