package com.quyong.attendance.module.model.prompt.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateQueryDTO;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateSaveDTO;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateStatusDTO;
import com.quyong.attendance.module.model.prompt.service.PromptTemplateService;
import com.quyong.attendance.module.model.prompt.vo.PromptTemplateVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/prompt")
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    public PromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping("/list")
    public Result<PageResult<PromptTemplateVO>> list(PromptTemplateQueryDTO dto) {
        return Result.success(promptTemplateService.list(dto));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody(required = false) PromptTemplateSaveDTO dto) {
        promptTemplateService.add(dto);
        return Result.success(null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody(required = false) PromptTemplateSaveDTO dto) {
        promptTemplateService.update(dto);
        return Result.success(null);
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody(required = false) PromptTemplateStatusDTO dto) {
        promptTemplateService.updateStatus(dto);
        return Result.success(null);
    }
}
