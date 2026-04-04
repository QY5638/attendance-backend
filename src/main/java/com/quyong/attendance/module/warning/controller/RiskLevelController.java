package com.quyong.attendance.module.warning.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.warning.dto.RiskLevelQueryDTO;
import com.quyong.attendance.module.warning.dto.RiskLevelUpdateDTO;
import com.quyong.attendance.module.warning.service.WarningService;
import com.quyong.attendance.module.warning.vo.RiskLevelConfigVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/risk-level")
public class RiskLevelController {

    private final WarningService warningService;

    public RiskLevelController(WarningService warningService) {
        this.warningService = warningService;
    }

    @GetMapping("/list")
    public Result<PageResult<RiskLevelConfigVO>> list(RiskLevelQueryDTO queryDTO) {
        return Result.success(warningService.listRiskLevels(queryDTO));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody(required = false) RiskLevelUpdateDTO dto) {
        warningService.updateRiskLevel(dto);
        return Result.success(null);
    }
}
