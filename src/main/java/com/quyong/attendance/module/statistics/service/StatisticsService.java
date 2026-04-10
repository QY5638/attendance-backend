package com.quyong.attendance.module.statistics.service;

import com.quyong.attendance.module.statistics.dto.DepartmentRiskBriefQueryDTO;
import com.quyong.attendance.module.statistics.dto.DepartmentStatisticsQueryDTO;
import com.quyong.attendance.module.statistics.dto.ExceptionTrendQueryDTO;
import com.quyong.attendance.module.statistics.dto.PersonalStatisticsQueryDTO;
import com.quyong.attendance.module.statistics.dto.StatisticsExportQueryDTO;
import com.quyong.attendance.module.statistics.dto.StatisticsSummaryQueryDTO;
import com.quyong.attendance.module.statistics.vo.DepartmentRiskBriefVO;
import com.quyong.attendance.module.statistics.vo.DepartmentStatisticsVO;
import com.quyong.attendance.module.statistics.vo.ExceptionTrendVO;
import com.quyong.attendance.module.statistics.vo.PersonalStatisticsVO;
import com.quyong.attendance.module.statistics.vo.StatisticsExportFileVO;
import com.quyong.attendance.module.statistics.vo.StatisticsSummaryVO;

import java.util.List;

public interface StatisticsService {

    PersonalStatisticsVO personal(PersonalStatisticsQueryDTO dto);

    DepartmentStatisticsVO department(DepartmentStatisticsQueryDTO dto);

    ExceptionTrendVO exceptionTrend(ExceptionTrendQueryDTO dto);

    StatisticsSummaryVO summary(StatisticsSummaryQueryDTO dto);

    DepartmentRiskBriefVO departmentRiskBrief(DepartmentRiskBriefQueryDTO dto);

    List<DepartmentRiskBriefVO> departmentRiskOverview(DepartmentStatisticsQueryDTO dto);

    StatisticsExportFileVO export(StatisticsExportQueryDTO dto);
}
