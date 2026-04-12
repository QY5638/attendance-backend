package com.quyong.attendance.module.statistics.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.statistics.dto.OperationLogQueryDTO;
import com.quyong.attendance.module.statistics.vo.OperationLogSummaryVO;
import com.quyong.attendance.module.statistics.vo.OperationLogVO;

import java.util.List;

public interface OperationLogService {

    void save(Long userId, String type, String content);

    PageResult<OperationLogVO> list(OperationLogQueryDTO dto);

    OperationLogSummaryVO summary(OperationLogQueryDTO dto);

    List<OperationLogVO> listAll(OperationLogQueryDTO dto);

    List<String> resolveTypes(OperationLogQueryDTO dto);
}
