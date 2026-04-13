package com.quyong.attendance.module.exceptiondetect.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.exceptiondetect.dto.ComplexCheckDTO;
import com.quyong.attendance.module.exceptiondetect.dto.ExceptionQueryDTO;
import com.quyong.attendance.module.exceptiondetect.dto.RuleCheckDTO;
import com.quyong.attendance.module.exceptiondetect.service.ExceptionAnalysisOrchestrator;
import com.quyong.attendance.module.exceptiondetect.service.ExceptionQueryService;
import com.quyong.attendance.module.exceptiondetect.vo.AttendanceExceptionVO;
import com.quyong.attendance.module.exceptiondetect.vo.ExceptionDecisionVO;
import com.quyong.attendance.module.exceptiondetect.vo.ExceptionAnalysisBriefVO;
import com.quyong.attendance.module.model.trace.service.DecisionTraceService;
import com.quyong.attendance.module.model.trace.vo.DecisionTraceVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exception")
public class ExceptionController {

    private final ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator;
    private final ExceptionQueryService exceptionQueryService;
    private final DecisionTraceService decisionTraceService;

    public ExceptionController(ExceptionAnalysisOrchestrator exceptionAnalysisOrchestrator,
                               ExceptionQueryService exceptionQueryService,
                               DecisionTraceService decisionTraceService) {
        this.exceptionAnalysisOrchestrator = exceptionAnalysisOrchestrator;
        this.exceptionQueryService = exceptionQueryService;
        this.decisionTraceService = decisionTraceService;
    }

    @PostMapping("/rule-check")
    public Result<ExceptionDecisionVO> ruleCheck(@RequestBody(required = false) RuleCheckDTO dto) {
        return Result.success(exceptionAnalysisOrchestrator.ruleCheck(dto));
    }

    @PostMapping("/complex-check")
    public Result<ExceptionDecisionVO> complexCheck(@RequestBody(required = false) ComplexCheckDTO dto) {
        return Result.success(exceptionAnalysisOrchestrator.complexCheck(dto));
    }

    @PostMapping("/backfill-absence-context")
    public Result<Integer> backfillAbsenceContext() {
        return Result.success(Integer.valueOf(exceptionAnalysisOrchestrator.backfillAbsenceContext()));
    }

    @GetMapping("/list")
    public Result<PageResult<AttendanceExceptionVO>> list(ExceptionQueryDTO queryDTO) {
        return Result.success(exceptionQueryService.list(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<AttendanceExceptionVO> getById(@PathVariable Long id) {
        return Result.success(exceptionQueryService.getById(id));
    }

    @GetMapping("/{id}/decision-trace")
    public Result<List<DecisionTraceVO>> decisionTrace(@PathVariable Long id) {
        return Result.success(decisionTraceService.list("ATTENDANCE_EXCEPTION", id));
    }

    @GetMapping("/{id}/analysis-brief")
    public Result<ExceptionAnalysisBriefVO> analysisBrief(@PathVariable Long id) {
        return Result.success(exceptionQueryService.getAnalysisBrief(id));
    }
}
