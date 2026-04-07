package com.quyong.attendance.module.review.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.review.dto.ExceptionTypeQueryDTO;
import com.quyong.attendance.module.review.dto.ExceptionTypeUpdateDTO;
import com.quyong.attendance.module.review.service.ExceptionTypeService;
import com.quyong.attendance.module.review.vo.ExceptionTypeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/exception-type")
public class ExceptionTypeController {

    private final ExceptionTypeService exceptionTypeService;

    public ExceptionTypeController(ExceptionTypeService exceptionTypeService) {
        this.exceptionTypeService = exceptionTypeService;
    }

    @GetMapping("/list")
    public Result<PageResult<ExceptionTypeVO>> list(ExceptionTypeQueryDTO dto) {
        return Result.success(exceptionTypeService.list(dto));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody(required = false) ExceptionTypeUpdateDTO dto) {
        exceptionTypeService.update(dto);
        return Result.success(null);
    }
}
