package com.quyong.attendance.module.exceptiondetect.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.exceptiondetect.dto.RuleQueryDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleSaveDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleStatusDTO;
import com.quyong.attendance.module.exceptiondetect.service.RuleService;
import com.quyong.attendance.module.exceptiondetect.vo.RuleVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rule")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/list")
    public Result<PageResult<RuleVO>> list(RuleQueryDTO queryDTO) {
        return Result.success(ruleService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody(required = false) RuleSaveDTO saveDTO) {
        ruleService.add(saveDTO);
        return Result.success(null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody(required = false) RuleSaveDTO saveDTO) {
        ruleService.update(saveDTO);
        return Result.success(null);
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody(required = false) RuleStatusDTO statusDTO) {
        ruleService.updateStatus(statusDTO);
        return Result.success(null);
    }
}
