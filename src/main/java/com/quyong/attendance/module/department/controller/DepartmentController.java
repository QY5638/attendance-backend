package com.quyong.attendance.module.department.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.department.dto.DepartmentQueryDTO;
import com.quyong.attendance.module.department.dto.DepartmentSaveDTO;
import com.quyong.attendance.module.department.service.DepartmentService;
import com.quyong.attendance.module.department.vo.DepartmentVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/list")
    public Result<List<DepartmentVO>> list(DepartmentQueryDTO queryDTO) {
        return Result.success(departmentService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<DepartmentVO> add(@RequestBody DepartmentSaveDTO saveDTO) {
        return Result.success(departmentService.add(saveDTO));
    }

    @PutMapping("/update")
    public Result<DepartmentVO> update(@RequestBody DepartmentSaveDTO saveDTO) {
        return Result.success(departmentService.update(saveDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.success(null);
    }
}
