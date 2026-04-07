package com.quyong.attendance.module.statistics.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.statistics.dto.OperationLogQueryDTO;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.statistics.vo.OperationLogVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/log/operation")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/list")
    public Result<PageResult<OperationLogVO>> list(OperationLogQueryDTO dto) {
        return Result.success(operationLogService.list(dto));
    }
}
