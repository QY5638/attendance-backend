package com.quyong.attendance.module.model.log.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.model.log.dto.ModelCallLogQueryDTO;
import com.quyong.attendance.module.model.log.vo.ModelCallLogVO;

public interface ModelCallLogService {

    void logSuccess(String businessType,
                    Long businessId,
                    Long promptTemplateId,
                    String inputSummary,
                    String outputSummary,
                    Integer latencyMs);

    void logFailure(String businessType,
                    Long businessId,
                    Long promptTemplateId,
                    String inputSummary,
                    String errorMessage,
                    Integer latencyMs);

    PageResult<ModelCallLogVO> list(ModelCallLogQueryDTO dto);
}
