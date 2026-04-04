package com.quyong.attendance.module.warning.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.warning.dto.RiskLevelQueryDTO;
import com.quyong.attendance.module.warning.dto.RiskLevelUpdateDTO;
import com.quyong.attendance.module.warning.dto.WarningQueryDTO;
import com.quyong.attendance.module.warning.dto.WarningReevaluateDTO;
import com.quyong.attendance.module.warning.vo.RiskLevelConfigVO;
import com.quyong.attendance.module.warning.vo.WarningAdviceVO;
import com.quyong.attendance.module.warning.vo.WarningVO;

public interface WarningService {

    PageResult<WarningVO> list(WarningQueryDTO queryDTO);

    WarningAdviceVO getAdvice(Long id);

    WarningVO reEvaluate(WarningReevaluateDTO dto);

    PageResult<RiskLevelConfigVO> listRiskLevels(RiskLevelQueryDTO queryDTO);

    void updateRiskLevel(RiskLevelUpdateDTO dto);
}
