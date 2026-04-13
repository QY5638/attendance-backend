package com.quyong.attendance.module.warning.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.warning.dto.WarningQueryDTO;
import com.quyong.attendance.module.warning.dto.WarningReplyDTO;
import com.quyong.attendance.module.warning.dto.WarningRequestExplanationDTO;
import com.quyong.attendance.module.warning.dto.WarningReevaluateDTO;
import com.quyong.attendance.module.warning.service.WarningService;
import com.quyong.attendance.module.warning.vo.WarningAdviceVO;
import com.quyong.attendance.module.warning.vo.WarningDashboardVO;
import com.quyong.attendance.module.warning.vo.WarningInteractionVO;
import com.quyong.attendance.module.warning.vo.WarningVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/dashboard")
    public Result<WarningDashboardVO> dashboard() {
        return Result.success(warningService.dashboard());
    }

    @GetMapping("/{id}/advice")
    public Result<WarningAdviceVO> getAdvice(@PathVariable("id") Long id) {
        return Result.success(warningService.getAdvice(id));
    }

    @GetMapping("/{id}/interactions")
    public Result<List<WarningInteractionVO>> interactions(@PathVariable("id") Long id) {
        return Result.success(warningService.listInteractions(id));
    }

    @PostMapping("/re-evaluate")
    public Result<WarningVO> reEvaluate(@RequestBody(required = false) WarningReevaluateDTO dto) {
        return Result.success(warningService.reEvaluate(dto));
    }

    @PostMapping("/{id}/request-explanation")
    public Result<Void> requestExplanation(@PathVariable("id") Long id,
                                           @RequestBody(required = false) WarningRequestExplanationDTO dto) {
        requireAdmin(currentAuthUser());
        warningService.requestExplanation(id, dto);
        return Result.success(null);
    }

    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable("id") Long id,
                              @RequestBody(required = false) WarningReplyDTO dto) {
        warningService.reply(id, dto);
        return Result.success(null);
    }

    @PostMapping("/run-absence-check")
    public Result<Integer> runAbsenceCheck() {
        requireAdmin(currentAuthUser());
        return Result.success(Integer.valueOf(warningService.runAbsenceCheck()));
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }

    private void requireAdmin(AuthUser authUser) {
        if (authUser == null || !"ADMIN".equals(authUser.getRoleCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage());
        }
    }
}
