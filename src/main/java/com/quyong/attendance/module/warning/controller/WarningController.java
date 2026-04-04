package com.quyong.attendance.module.warning.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.warning.dto.WarningQueryDTO;
import com.quyong.attendance.module.warning.dto.WarningReevaluateDTO;
import com.quyong.attendance.module.warning.service.WarningService;
import com.quyong.attendance.module.warning.vo.WarningAdviceVO;
import com.quyong.attendance.module.warning.vo.WarningVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warning")
public class WarningController {

    private final WarningService warningService;

    public WarningController(WarningService warningService) {
        this.warningService = warningService;
    }

    @GetMapping("/list")
    public Result<PageResult<WarningVO>> list(WarningQueryDTO queryDTO) {
        return Result.success(warningService.list(queryDTO));
    }

    @GetMapping("/{id}/advice")
    public Result<WarningAdviceVO> getAdvice(@PathVariable("id") Long id) {
        return Result.success(warningService.getAdvice(id));
    }

    @PostMapping("/re-evaluate")
    public Result<WarningVO> reEvaluate(@RequestBody(required = false) WarningReevaluateDTO dto) {
        return Result.success(warningService.reEvaluate(dto));
    }
}
