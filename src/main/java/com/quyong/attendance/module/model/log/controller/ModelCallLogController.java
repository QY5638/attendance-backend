package com.quyong.attendance.module.model.log.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.model.log.dto.ModelCallLogQueryDTO;
import com.quyong.attendance.module.model.log.service.ModelCallLogService;
import com.quyong.attendance.module.model.log.vo.ModelCallLogVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/model-log")
public class ModelCallLogController {

    private final ModelCallLogService modelCallLogService;

    public ModelCallLogController(ModelCallLogService modelCallLogService) {
        this.modelCallLogService = modelCallLogService;
    }

    @GetMapping("/list")
    public Result<PageResult<ModelCallLogVO>> list(ModelCallLogQueryDTO dto) {
        return Result.success(modelCallLogService.list(dto));
    }
}
