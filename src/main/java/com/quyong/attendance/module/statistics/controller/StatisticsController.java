package com.quyong.attendance.module.statistics.controller;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.module.statistics.dto.DepartmentRiskBriefQueryDTO;
import com.quyong.attendance.module.statistics.dto.DepartmentStatisticsQueryDTO;
import com.quyong.attendance.module.statistics.dto.ExceptionTrendQueryDTO;
import com.quyong.attendance.module.statistics.dto.PersonalStatisticsQueryDTO;
import com.quyong.attendance.module.statistics.dto.StatisticsExportQueryDTO;
import com.quyong.attendance.module.statistics.dto.StatisticsSummaryQueryDTO;
import com.quyong.attendance.module.statistics.service.StatisticsService;
import com.quyong.attendance.module.statistics.vo.DepartmentRiskBriefVO;
import com.quyong.attendance.module.statistics.vo.DepartmentStatisticsVO;
import com.quyong.attendance.module.statistics.vo.ExceptionTrendVO;
import com.quyong.attendance.module.statistics.vo.PersonalStatisticsVO;
import com.quyong.attendance.module.statistics.vo.StatisticsSummaryVO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/personal")
    public Result<PersonalStatisticsVO> personal(PersonalStatisticsQueryDTO dto) {
        return Result.success(statisticsService.personal(dto));
    }

    @GetMapping("/department")
    public Result<DepartmentStatisticsVO> department(DepartmentStatisticsQueryDTO dto) {
        return Result.success(statisticsService.department(dto));
    }

    @GetMapping("/exception-trend")
    public Result<ExceptionTrendVO> exceptionTrend(ExceptionTrendQueryDTO dto) {
        return Result.success(statisticsService.exceptionTrend(dto));
    }

    @GetMapping("/summary")
    public Result<StatisticsSummaryVO> summary(StatisticsSummaryQueryDTO dto) {
        return Result.success(statisticsService.summary(dto));
    }

    @GetMapping("/department-risk-brief")
    public Result<DepartmentRiskBriefVO> departmentRiskBrief(DepartmentRiskBriefQueryDTO dto) {
        return Result.success(statisticsService.departmentRiskBrief(dto));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(StatisticsExportQueryDTO dto) {
        String csv = statisticsService.export(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statistics-export.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
