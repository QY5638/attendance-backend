package com.quyong.attendance.module.role.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.role.dto.RoleQueryDTO;
import com.quyong.attendance.module.role.dto.RoleSaveDTO;
import com.quyong.attendance.module.role.service.RoleService;
import com.quyong.attendance.module.role.vo.RoleVO;
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
@RequestMapping("/api/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/list")
    public Result<List<RoleVO>> list(RoleQueryDTO queryDTO) {
        return Result.success(roleService.list(queryDTO));
    }

    @PostMapping("/add")
    public Result<RoleVO> add(@RequestBody(required = false) RoleSaveDTO saveDTO) {
        return Result.success(roleService.add(saveDTO));
    }

    @PutMapping("/update")
    public Result<RoleVO> update(@RequestBody(required = false) RoleSaveDTO saveDTO) {
        return Result.success(roleService.update(saveDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success(null);
    }
}
