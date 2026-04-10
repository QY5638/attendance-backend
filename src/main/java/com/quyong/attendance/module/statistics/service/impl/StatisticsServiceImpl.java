package com.quyong.attendance.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.department.entity.Department;
import com.quyong.attendance.module.department.mapper.DepartmentMapper;
import com.quyong.attendance.module.statistics.dto.DepartmentRiskBriefQueryDTO;
import com.quyong.attendance.module.statistics.dto.DepartmentStatisticsQueryDTO;
import com.quyong.attendance.module.statistics.dto.ExceptionTrendQueryDTO;
import com.quyong.attendance.module.statistics.dto.OperationLogQueryDTO;
import com.quyong.attendance.module.statistics.dto.PersonalStatisticsQueryDTO;
import com.quyong.attendance.module.statistics.dto.StatisticsExportQueryDTO;
import com.quyong.attendance.module.statistics.dto.StatisticsSummaryQueryDTO;
import com.quyong.attendance.module.statistics.mapper.StatisticsMapper;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.statistics.service.StatisticsService;
import com.quyong.attendance.module.statistics.support.StatisticsSummarySupport;
import com.quyong.attendance.module.statistics.support.StatisticsValidationSupport;
import com.quyong.attendance.module.statistics.vo.DepartmentRiskBriefVO;
import com.quyong.attendance.module.statistics.vo.DepartmentStatisticsVO;
import com.quyong.attendance.module.statistics.vo.ExceptionTrendPointVO;
import com.quyong.attendance.module.statistics.vo.ExceptionTrendVO;
import com.quyong.attendance.module.statistics.vo.OperationLogVO;
import com.quyong.attendance.module.statistics.vo.PersonalStatisticsVO;
import com.quyong.attendance.module.statistics.vo.StatisticsExportFileVO;
import com.quyong.attendance.module.statistics.vo.StatisticsSummaryVO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.support.UserValidationSupport;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final long AUDIT_EXPORT_MAX_ROWS = 5000L;
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_CONTENT_TYPE = "text/csv;charset=UTF-8";
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final StatisticsMapper statisticsMapper;
    private final StatisticsValidationSupport statisticsValidationSupport;
    private final StatisticsSummarySupport statisticsSummarySupport;
    private final UserValidationSupport userValidationSupport;
    private final DepartmentMapper departmentMapper;
    private final OperationLogService operationLogService;

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper,
                                 StatisticsValidationSupport statisticsValidationSupport,
                                 StatisticsSummarySupport statisticsSummarySupport,
                                 UserValidationSupport userValidationSupport,
                                 DepartmentMapper departmentMapper,
                                 OperationLogService operationLogService) {
        this.statisticsMapper = statisticsMapper;
        this.statisticsValidationSupport = statisticsValidationSupport;
        this.statisticsSummarySupport = statisticsSummarySupport;
        this.userValidationSupport = userValidationSupport;
        this.departmentMapper = departmentMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public PersonalStatisticsVO personal(PersonalStatisticsQueryDTO dto) {
        PersonalStatisticsQueryDTO safe = statisticsValidationSupport.validatePersonal(dto);
        AuthUser authUser = currentAuthUser();
        User user = userValidationSupport.requireExistingUser(authUser.getUserId());
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());

        PersonalStatisticsVO vo = new PersonalStatisticsVO();
        vo.setUserId(user.getId());
        vo.setRealName(user.getRealName());
        vo.setDeptId(user.getDeptId());
        vo.setRecordCount(Long.valueOf(statisticsMapper.countRecords(user.getId(), null, startTime, endTime)));
        vo.setExceptionCount(Long.valueOf(statisticsMapper.countExceptions(user.getId(), null, startTime, endTime)));
        vo.setAnalysisCount(Long.valueOf(statisticsMapper.countAnalyses(user.getId(), null, startTime, endTime)));
        vo.setWarningCount(Long.valueOf(statisticsMapper.countWarnings(user.getId(), null, startTime, endTime)));
        vo.setReviewCount(Long.valueOf(statisticsMapper.countReviews(user.getId(), null, startTime, endTime)));
        vo.setClosedLoopCount(Long.valueOf(statisticsMapper.countClosedLoops(user.getId(), null, startTime, endTime)));
        vo.setExceptionTypeDistribution(toDistributionMap(statisticsMapper.selectExceptionTypeDistribution(user.getId(), null, startTime, endTime)));
        vo.setRiskLevelDistribution(toDistributionMap(statisticsMapper.selectRiskLevelDistribution(user.getId(), null, startTime, endTime)));
        vo.setWarningStatusDistribution(toDistributionMap(statisticsMapper.selectWarningStatusDistribution(user.getId(), null, startTime, endTime)));
        vo.setReviewResultDistribution(toDistributionMap(statisticsMapper.selectReviewResultDistribution(user.getId(), null, startTime, endTime)));
        return vo;
    }

    @Override
    public DepartmentStatisticsVO department(DepartmentStatisticsQueryDTO dto) {
        DepartmentStatisticsQueryDTO safe = statisticsValidationSupport.validateDepartment(dto);
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        return buildDepartmentStatistics(null, "全部部门", startTime, endTime);
    }

    @Override
    public ExceptionTrendVO exceptionTrend(ExceptionTrendQueryDTO dto) {
        ExceptionTrendQueryDTO safe = statisticsValidationSupport.validateTrend(dto);
        if (safe.getDeptId() != null) {
            requireDepartment(safe.getDeptId());
        }
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        Map<String, ExceptionTrendPointVO> pointMap = new LinkedHashMap<String, ExceptionTrendPointVO>();

        mergeTrend(pointMap, statisticsMapper.selectRecordTrendRows(safe.getDeptId(), startTime, endTime), "recordCount", safe.getPeriodType());
        mergeTrend(pointMap, statisticsMapper.selectExceptionTrendRows(safe.getDeptId(), startTime, endTime), "exceptionCount", safe.getPeriodType());
        mergeTrend(pointMap, statisticsMapper.selectAnalysisTrendRows(safe.getDeptId(), startTime, endTime), "analysisCount", safe.getPeriodType());
        mergeTrend(pointMap, statisticsMapper.selectWarningTrendRows(safe.getDeptId(), startTime, endTime), "warningCount", safe.getPeriodType());
        mergeTrend(pointMap, statisticsMapper.selectReviewTrendRows(safe.getDeptId(), startTime, endTime), "reviewCount", safe.getPeriodType());
        mergeTrend(pointMap, statisticsMapper.selectClosedLoopTrendRows(safe.getDeptId(), startTime, endTime), "closedLoopCount", safe.getPeriodType());

        ExceptionTrendVO vo = new ExceptionTrendVO();
        vo.setPeriodType(safe.getPeriodType());
        vo.setPoints(new ArrayList<ExceptionTrendPointVO>(pointMap.values()));
        return vo;
    }

    @Override
    public StatisticsSummaryVO summary(StatisticsSummaryQueryDTO dto) {
        StatisticsSummaryQueryDTO safe = statisticsValidationSupport.validateSummary(dto);
        AuthUser authUser = currentAuthUser();
        Long scopeUserId = isAdmin(authUser) ? null : authUser.getUserId();
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        Long recordCount = Long.valueOf(statisticsMapper.countRecords(scopeUserId, null, startTime, endTime));
        Long exceptionCount = Long.valueOf(statisticsMapper.countExceptions(scopeUserId, null, startTime, endTime));
        Long analysisCount = Long.valueOf(statisticsMapper.countAnalyses(scopeUserId, null, startTime, endTime));
        Long warningCount = Long.valueOf(statisticsMapper.countWarnings(scopeUserId, null, startTime, endTime));
        Long reviewCount = Long.valueOf(statisticsMapper.countReviews(scopeUserId, null, startTime, endTime));
        Long closedLoopCount = Long.valueOf(statisticsMapper.countClosedLoops(scopeUserId, null, startTime, endTime));
        Long highRiskCount = distributionValue(statisticsMapper.selectRiskLevelDistribution(scopeUserId, null, startTime, endTime), "HIGH");
        Long analysisGapCount = Long.valueOf(Math.max(exceptionCount.longValue() - analysisCount.longValue(), 0L));
        Long unprocessedWarningCount = distributionValue(statisticsMapper.selectWarningStatusDistribution(scopeUserId, null, startTime, endTime), "UNPROCESSED");
        Long closedLoopGapCount = Long.valueOf(Math.max(warningCount.longValue() - closedLoopCount.longValue(), 0L));
        Long missingDecisionTraceCount = Long.valueOf(statisticsMapper.countMissingDecisionTrace(scopeUserId, null, startTime, endTime));
        Long missingModelLogCount = Long.valueOf(statisticsMapper.countMissingModelLog(scopeUserId, null, startTime, endTime));

        StatisticsSummaryVO vo = new StatisticsSummaryVO();
        vo.setPeriodType(safe.getPeriodType());
        vo.setSummary(emptyToNull(statisticsMapper.selectLatestSummaryText(scopeUserId, null, startTime, endTime)));
        vo.setHighlightRisks(emptyToNull(statisticsMapper.selectLatestHighlightText(scopeUserId, null, startTime, endTime)));
        vo.setManageSuggestion(emptyToNull(statisticsMapper.selectLatestManageSuggestionText(scopeUserId, null, startTime, endTime)));
        return vo;
    }

    @Override
    public DepartmentRiskBriefVO departmentRiskBrief(DepartmentRiskBriefQueryDTO dto) {
        DepartmentRiskBriefQueryDTO safe = statisticsValidationSupport.validateDepartmentRiskBrief(dto);
        Department department = requireDepartment(safe.getDeptId());
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        return buildDepartmentRiskBrief(department, startTime, endTime, true);
    }

    @Override
    public List<DepartmentRiskBriefVO> departmentRiskOverview(DepartmentStatisticsQueryDTO dto) {
        DepartmentStatisticsQueryDTO safe = statisticsValidationSupport.validateDepartment(dto);
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        List<Department> departments = departmentMapper.selectList(Wrappers.<Department>lambdaQuery().orderByAsc(Department::getId));
        List<DepartmentRiskBriefVO> result = new ArrayList<DepartmentRiskBriefVO>();

        for (Department department : departments) {
            DepartmentRiskBriefVO item = buildDepartmentRiskBrief(department, startTime, endTime, true);
            if (item != null) {
                result.add(item);
            }
        }

        result.sort(Comparator
                .comparing(DepartmentRiskBriefVO::getRiskScore, Comparator.nullsFirst(BigDecimal::compareTo))
                .reversed()
                .thenComparing(DepartmentRiskBriefVO::getDeptId, Comparator.nullsLast(Long::compareTo)));
        return result;
    }

    @Override
    public StatisticsExportFileVO export(StatisticsExportQueryDTO dto) {
        StatisticsExportQueryDTO safe = statisticsValidationSupport.validateExport(dto);
        if ("PERSONAL".equals(safe.getExportType())) {
            PersonalStatisticsQueryDTO personalQueryDTO = new PersonalStatisticsQueryDTO();
            personalQueryDTO.setStartDate(safe.getStartDate());
            personalQueryDTO.setEndDate(safe.getEndDate());
            PersonalStatisticsVO personal = personal(personalQueryDTO);
            return buildCsvExportFile("个人统计报表.csv", buildPersonalCsv(personal));
        }
        if ("DEPARTMENT".equals(safe.getExportType())) {
            LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
            LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
            return buildXlsxExportFile("部门统计报表.xlsx", buildDepartmentWorkbook(buildDepartmentStatisticsRows(startTime, endTime), safe));
        }
        if ("TREND".equals(safe.getExportType())) {
            ExceptionTrendQueryDTO trendQueryDTO = new ExceptionTrendQueryDTO();
            trendQueryDTO.setDeptId(safe.getDeptId());
            trendQueryDTO.setStartDate(safe.getStartDate());
            trendQueryDTO.setEndDate(safe.getEndDate());
            trendQueryDTO.setPeriodType(safe.getPeriodType());
            ExceptionTrendVO trendVO = exceptionTrend(trendQueryDTO);
            return buildCsvExportFile("异常趋势报表.csv", buildTrendCsv(trendVO));
        }
        OperationLogQueryDTO queryDTO = new OperationLogQueryDTO();
        queryDTO.setUserId(safe.getUserId());
        queryDTO.setType(safe.getType());
        queryDTO.setStartDate(safe.getStartDate());
        queryDTO.setEndDate(safe.getEndDate());
        long total = operationLogService.list(queryDTO).getTotal().longValue();
        if (total > AUDIT_EXPORT_MAX_ROWS) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导出数据量过大，请缩小查询范围");
        }
        List<OperationLogVO> records = selectAllOperationLogs(queryDTO);
        return buildCsvExportFile("业务记录报表.csv", buildAuditCsv(records));
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }

    private boolean isAdmin(AuthUser authUser) {
        return authUser != null && "ADMIN".equals(authUser.getRoleCode());
    }

    private Department requireDepartment(Long deptId) {
        Department department = deptId == null ? null : departmentMapper.selectById(deptId);
        if (department == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门不存在");
        }
        return department;
    }

    private DepartmentRiskBriefVO buildDepartmentRiskBrief(Department department,
                                                           LocalDateTime startTime,
                                                           LocalDateTime endTime,
                                                           boolean includeEmptyDepartment) {
        Long recordCount = Long.valueOf(statisticsMapper.countRecords(null, department.getId(), startTime, endTime));
        if (!includeEmptyDepartment && recordCount.longValue() == 0L) {
            return null;
        }
        Long exceptionCount = Long.valueOf(statisticsMapper.countExceptions(null, department.getId(), startTime, endTime));
        Long warningCount = Long.valueOf(statisticsMapper.countWarnings(null, department.getId(), startTime, endTime));
        Long closedLoopCount = Long.valueOf(statisticsMapper.countClosedLoops(null, department.getId(), startTime, endTime));
        Long highRiskCount = distributionValue(statisticsMapper.selectRiskLevelDistribution(null, department.getId(), startTime, endTime), "HIGH");
        Long unprocessedWarningCount = distributionValue(statisticsMapper.selectWarningStatusDistribution(null, department.getId(), startTime, endTime), "UNPROCESSED");
        Long closedLoopGapCount = Long.valueOf(Math.max(warningCount.longValue() - closedLoopCount.longValue(), 0L));

        DepartmentRiskBriefVO vo = new DepartmentRiskBriefVO();
        vo.setDeptId(department.getId());
        vo.setDeptName(department.getName());
        vo.setRiskScore(statisticsSummarySupport.calculateDepartmentRiskScore(recordCount, exceptionCount, highRiskCount, warningCount, unprocessedWarningCount, closedLoopCount));
        vo.setRiskSummary(emptyToNull(statisticsMapper.selectLatestHighlightText(null, department.getId(), startTime, endTime)));
        vo.setManageSuggestion(emptyToNull(statisticsMapper.selectLatestManageSuggestionText(null, department.getId(), startTime, endTime)));
        return vo;
    }

    private DepartmentStatisticsVO buildDepartmentStatistics(Long deptId,
                                                             String deptName,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime) {
        DepartmentStatisticsVO vo = new DepartmentStatisticsVO();
        vo.setDeptId(deptId);
        vo.setDeptName(deptName);
        vo.setRecordCount(Long.valueOf(statisticsMapper.countRecords(null, deptId, startTime, endTime)));
        vo.setExceptionCount(Long.valueOf(statisticsMapper.countExceptions(null, deptId, startTime, endTime)));
        vo.setAnalysisCount(Long.valueOf(statisticsMapper.countAnalyses(null, deptId, startTime, endTime)));
        vo.setWarningCount(Long.valueOf(statisticsMapper.countWarnings(null, deptId, startTime, endTime)));
        vo.setReviewCount(Long.valueOf(statisticsMapper.countReviews(null, deptId, startTime, endTime)));
        vo.setClosedLoopCount(Long.valueOf(statisticsMapper.countClosedLoops(null, deptId, startTime, endTime)));
        vo.setExceptionTypeDistribution(toDistributionMap(statisticsMapper.selectExceptionTypeDistribution(null, deptId, startTime, endTime)));
        vo.setRiskLevelDistribution(toDistributionMap(statisticsMapper.selectRiskLevelDistribution(null, deptId, startTime, endTime)));
        vo.setWarningStatusDistribution(toDistributionMap(statisticsMapper.selectWarningStatusDistribution(null, deptId, startTime, endTime)));
        vo.setReviewResultDistribution(toDistributionMap(statisticsMapper.selectReviewResultDistribution(null, deptId, startTime, endTime)));
        return vo;
    }

    private List<DepartmentStatisticsVO> buildDepartmentStatisticsRows(LocalDateTime startTime, LocalDateTime endTime) {
        List<DepartmentStatisticsVO> rows = new ArrayList<DepartmentStatisticsVO>();
        rows.add(buildDepartmentStatistics(null, "全部部门", startTime, endTime));
        List<Department> departments = departmentMapper.selectList(Wrappers.<Department>lambdaQuery().orderByAsc(Department::getId));
        for (Department department : departments) {
            rows.add(buildDepartmentStatistics(department.getId(), department.getName(), startTime, endTime));
        }
        return rows;
    }

    private Map<String, Long> toDistributionMap(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new LinkedHashMap<String, Long>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(valueOf(row, "label")), Long.valueOf(numberValue(row, "total")));
        }
        return result;
    }

    private Long distributionValue(List<Map<String, Object>> rows, String targetLabel) {
        if (rows == null) {
            return 0L;
        }
        for (Map<String, Object> row : rows) {
            if (targetLabel.equals(String.valueOf(valueOf(row, "label")))) {
                return Long.valueOf(numberValue(row, "total"));
            }
        }
        return 0L;
    }

    private String emptyToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void mergeTrend(Map<String, ExceptionTrendPointVO> pointMap,
                            List<Map<String, Object>> rows,
                            String fieldName,
                            String periodType) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            String bucket = normalizeBucket(String.valueOf(valueOf(row, "bucket")), periodType);
            ExceptionTrendPointVO point = pointMap.get(bucket);
            if (point == null) {
                point = new ExceptionTrendPointVO();
                point.setDate(bucket);
                point.setRecordCount(0L);
                point.setExceptionCount(0L);
                point.setAnalysisCount(0L);
                point.setWarningCount(0L);
                point.setReviewCount(0L);
                point.setClosedLoopCount(0L);
                pointMap.put(bucket, point);
            }
            Long count = Long.valueOf(numberValue(row, "total"));
            if ("recordCount".equals(fieldName)) {
                point.setRecordCount(Long.valueOf(point.getRecordCount().longValue() + count.longValue()));
            } else if ("exceptionCount".equals(fieldName)) {
                point.setExceptionCount(Long.valueOf(point.getExceptionCount().longValue() + count.longValue()));
            } else if ("analysisCount".equals(fieldName)) {
                point.setAnalysisCount(Long.valueOf(point.getAnalysisCount().longValue() + count.longValue()));
            } else if ("warningCount".equals(fieldName)) {
                point.setWarningCount(Long.valueOf(point.getWarningCount().longValue() + count.longValue()));
            } else if ("reviewCount".equals(fieldName)) {
                point.setReviewCount(Long.valueOf(point.getReviewCount().longValue() + count.longValue()));
            } else if ("closedLoopCount".equals(fieldName)) {
                point.setClosedLoopCount(Long.valueOf(point.getClosedLoopCount().longValue() + count.longValue()));
            }
        }
    }

    private String normalizeBucket(String bucket, String periodType) {
        if (!"WEEK".equals(periodType) && !"MONTH".equals(periodType)) {
            return bucket;
        }
        LocalDate date = LocalDate.parse(bucket);
        if ("WEEK".equals(periodType)) {
            return date.with(DayOfWeek.MONDAY).toString();
        }
        return date.withDayOfMonth(1).toString();
    }

    private Object valueOf(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String upperKey = key.toUpperCase();
        if (row.containsKey(upperKey)) {
            return row.get(upperKey);
        }
        return row.get(key.toLowerCase());
    }

    private long numberValue(Map<String, Object> row, String key) {
        Object value = valueOf(row, key);
        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
    }

    private String buildPersonalCsv(PersonalStatisticsVO personal) {
        return new StringBuilder()
                .append("人员编号,姓名,部门编号,考勤记录数,异常记录数,预警记录数,复核记录数,闭环记录数\n")
                .append(personal.getUserId()).append(',')
                .append(csvValue(personal.getRealName())).append(',')
                .append(personal.getDeptId()).append(',')
                .append(personal.getRecordCount()).append(',')
                .append(personal.getExceptionCount()).append(',')
                .append(personal.getWarningCount()).append(',')
                .append(personal.getReviewCount()).append(',')
                .append(personal.getClosedLoopCount())
                .append('\n')
                .toString();
    }

    private String buildDepartmentCsv(List<DepartmentStatisticsVO> departments) {
        DepartmentStatisticsVO summary = departments.isEmpty() ? new DepartmentStatisticsVO() : departments.get(0);
        StringBuilder builder = new StringBuilder();
        builder.append(csvValue("部门统计报表")).append('\n')
                .append("导出时间,").append(EXPORT_TIME_FORMATTER.format(LocalDateTime.now())).append('\n')
                .append('\n')
                .append(csvValue("汇总概况")).append('\n')
                .append("部门范围,考勤记录数,异常记录数,分析记录数,预警记录数,复核记录数,闭环记录数\n")
                .append(csvValue(summary.getDeptName())).append(',')
                .append(summary.getRecordCount()).append(',')
                .append(summary.getExceptionCount()).append(',')
                .append(summary.getAnalysisCount()).append(',')
                .append(summary.getWarningCount()).append(',')
                .append(summary.getReviewCount()).append(',')
                .append(summary.getClosedLoopCount())
                .append('\n')
                .append('\n')
                .append(csvValue("部门明细")).append('\n')
                .append("序号,部门编号,部门名称,考勤记录数,异常记录数,分析记录数,预警记录数,复核记录数,闭环记录数\n");

        for (int index = 1; index < departments.size(); index++) {
            DepartmentStatisticsVO department = departments.get(index);
            builder.append(index).append(',')
                    .append(nullableLongValue(department.getDeptId())).append(',')
                    .append(csvValue(department.getDeptName())).append(',')
                    .append(department.getRecordCount()).append(',')
                    .append(department.getExceptionCount()).append(',')
                    .append(department.getAnalysisCount()).append(',')
                    .append(department.getWarningCount()).append(',')
                    .append(department.getReviewCount()).append(',')
                    .append(department.getClosedLoopCount()).append('\n');
        }
        return builder.toString();
    }

    private byte[] buildDepartmentWorkbook(List<DepartmentStatisticsVO> departments, StatisticsExportQueryDTO query) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("部门统计报表");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle sectionStyle = createSectionStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0).setCellValue("部门统计报表");
            titleRow.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

            Row exportTimeRow = sheet.createRow(rowIndex++);
            exportTimeRow.createCell(0).setCellValue("导出时间");
            exportTimeRow.createCell(1).setCellValue(EXPORT_TIME_FORMATTER.format(LocalDateTime.now()));
            exportTimeRow.getCell(0).setCellStyle(textStyle);
            exportTimeRow.getCell(1).setCellStyle(textStyle);

            Row periodRow = sheet.createRow(rowIndex++);
            periodRow.createCell(0).setCellValue("统计周期");
            periodRow.createCell(1).setCellValue(resolvePeriodRange(query.getStartDate(), query.getEndDate()));
            periodRow.getCell(0).setCellStyle(textStyle);
            periodRow.getCell(1).setCellStyle(textStyle);

            rowIndex++;

            DepartmentStatisticsVO summary = departments.isEmpty() ? new DepartmentStatisticsVO() : departments.get(0);
            rowIndex = writeDepartmentSummarySection(sheet, rowIndex, summary, sectionStyle, headerStyle, textStyle, numberStyle, percentStyle);
            rowIndex++;
            writeDepartmentDetailSection(sheet, rowIndex, departments, sectionStyle, headerStyle, textStyle, numberStyle, percentStyle);

            for (int columnIndex = 0; columnIndex <= 12; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
                sheet.setColumnWidth(columnIndex, Math.min(sheet.getColumnWidth(columnIndex) + 1024, 12000));
            }
            sheet.createFreezePane(0, 6);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "生成统计报表失败");
        }
    }

    private String buildTrendCsv(ExceptionTrendVO trendVO) {
        StringBuilder builder = new StringBuilder();
        builder.append("日期,考勤记录数,异常记录数,分析记录数,预警记录数,复核记录数,闭环记录数\n");
        for (ExceptionTrendPointVO point : trendVO.getPoints()) {
            builder.append(point.getDate()).append(',')
                    .append(point.getRecordCount()).append(',')
                    .append(point.getExceptionCount()).append(',')
                    .append(point.getAnalysisCount()).append(',')
                    .append(point.getWarningCount()).append(',')
                    .append(point.getReviewCount()).append(',')
                    .append(point.getClosedLoopCount())
                    .append('\n');
        }
        return builder.toString();
    }

    private String buildAuditCsv(List<OperationLogVO> records) {
        StringBuilder builder = new StringBuilder();
        builder.append("记录编号,办理人编号,办理动作,办理内容,办理时间\n");
        for (OperationLogVO record : records) {
            builder.append(record.getId()).append(',')
                    .append(record.getUserId()).append(',')
                    .append(csvValue(formatAuditOperationType(record.getType()))).append(',')
                    .append(csvValue(record.getContent())).append(',')
                    .append(record.getOperationTime())
                    .append('\n');
        }
        return builder.toString();
    }

    private int writeDepartmentSummarySection(Sheet sheet,
                                              int rowIndex,
                                              DepartmentStatisticsVO summary,
                                              CellStyle sectionStyle,
                                              CellStyle headerStyle,
                                              CellStyle textStyle,
                                              CellStyle numberStyle,
                                              CellStyle percentStyle) {
        Row sectionRow = sheet.createRow(rowIndex++);
        sectionRow.createCell(0).setCellValue("汇总概况");
        sectionRow.getCell(0).setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(sectionRow.getRowNum(), sectionRow.getRowNum(), 0, 11));

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = new String[] {
                "部门范围", "考勤记录数", "异常记录数", "分析记录数", "预警记录数", "复核记录数", "闭环记录数",
                "异常率", "分析覆盖率", "预警触发率", "复核参与率", "闭环率"
        };
        for (int index = 0; index < headers.length; index++) {
            Cell cell = headerRow.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(headerStyle);
        }

        Row dataRow = sheet.createRow(rowIndex++);
        writeDepartmentStatisticsCells(dataRow, summary, false, textStyle, numberStyle, percentStyle);
        return rowIndex;
    }

    private void writeDepartmentDetailSection(Sheet sheet,
                                              int rowIndex,
                                              List<DepartmentStatisticsVO> departments,
                                              CellStyle sectionStyle,
                                              CellStyle headerStyle,
                                              CellStyle textStyle,
                                              CellStyle numberStyle,
                                              CellStyle percentStyle) {
        Row sectionRow = sheet.createRow(rowIndex++);
        sectionRow.createCell(0).setCellValue("部门明细");
        sectionRow.getCell(0).setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(sectionRow.getRowNum(), sectionRow.getRowNum(), 0, 12));

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = new String[] {
                "序号", "部门编号", "部门名称", "考勤记录数", "异常记录数", "分析记录数", "预警记录数", "复核记录数", "闭环记录数",
                "异常率", "分析覆盖率", "预警触发率", "闭环率"
        };
        for (int index = 0; index < headers.length; index++) {
            Cell cell = headerRow.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(headerStyle);
        }

        for (int index = 1; index < departments.size(); index++) {
            Row row = sheet.createRow(rowIndex++);
            Cell serialCell = row.createCell(0);
            serialCell.setCellValue(index);
            serialCell.setCellStyle(numberStyle);
            writeDepartmentStatisticsCells(row, departments.get(index), true, textStyle, numberStyle, percentStyle);
        }
    }

    private void writeDepartmentStatisticsCells(Row row,
                                                DepartmentStatisticsVO department,
                                                boolean includeDeptId,
                                                CellStyle textStyle,
                                                CellStyle numberStyle,
                                                CellStyle percentStyle) {
        if (includeDeptId) {
            Cell deptIdCell = row.createCell(1);
            deptIdCell.setCellValue(nullableLongValue(department.getDeptId()));
            deptIdCell.setCellStyle(textStyle);
            Cell deptNameCell = row.createCell(2);
            deptNameCell.setCellValue(StringUtils.hasText(department.getDeptName()) ? department.getDeptName() : "未命名部门");
            deptNameCell.setCellStyle(textStyle);
        } else {
            Cell rangeCell = row.createCell(0);
            rangeCell.setCellValue(StringUtils.hasText(department.getDeptName()) ? department.getDeptName() : "全部部门");
            rangeCell.setCellStyle(textStyle);
        }

        int startColumn = includeDeptId ? 3 : 1;
        writeLongCell(row, startColumn, department.getRecordCount(), numberStyle);
        writeLongCell(row, startColumn + 1, department.getExceptionCount(), numberStyle);
        writeLongCell(row, startColumn + 2, department.getAnalysisCount(), numberStyle);
        writeLongCell(row, startColumn + 3, department.getWarningCount(), numberStyle);
        writeLongCell(row, startColumn + 4, department.getReviewCount(), numberStyle);
        writeLongCell(row, startColumn + 5, department.getClosedLoopCount(), numberStyle);
        writeRatioCell(row, startColumn + 6, department.getExceptionCount(), department.getRecordCount(), percentStyle);
        writeRatioCell(row, startColumn + 7, department.getAnalysisCount(), department.getExceptionCount(), percentStyle);
        writeRatioCell(row, startColumn + 8, department.getWarningCount(), department.getExceptionCount(), percentStyle);
        if (includeDeptId) {
            writeRatioCell(row, startColumn + 9, department.getClosedLoopCount(), department.getWarningCount(), percentStyle);
        } else {
            writeRatioCell(row, startColumn + 9, department.getReviewCount(), department.getWarningCount(), percentStyle);
            writeRatioCell(row, startColumn + 10, department.getClosedLoopCount(), department.getWarningCount(), percentStyle);
        }
    }

    private void writeLongCell(Row row, int columnIndex, Long value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? 0D : value.doubleValue());
        cell.setCellStyle(style);
    }

    private void writeRatioCell(Row row, int columnIndex, Long numerator, Long denominator, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(calculateRatioValue(numerator, denominator));
        cell.setCellStyle(style);
    }

    private double calculateRatioValue(Long numerator, Long denominator) {
        if (denominator == null || denominator.longValue() <= 0L) {
            return 0D;
        }
        return new BigDecimal(numerator == null ? 0L : numerator.longValue())
                .divide(new BigDecimal(denominator.longValue()), 4, BigDecimal.ROUND_HALF_UP)
                .doubleValue();
    }

    private String resolvePeriodRange(String startDate, String endDate) {
        String normalizedStartDate = StringUtils.hasText(startDate) ? startDate.trim() : "";
        String normalizedEndDate = StringUtils.hasText(endDate) ? endDate.trim() : "";
        if (!StringUtils.hasText(normalizedStartDate) && !StringUtils.hasText(normalizedEndDate)) {
            return "全部时间";
        }
        if (StringUtils.hasText(normalizedStartDate) && StringUtils.hasText(normalizedEndDate)) {
            return normalizedStartDate + " 至 " + normalizedEndDate;
        }
        if (StringUtils.hasText(normalizedStartDate)) {
            return normalizedStartDate + " 起";
        }
        return "截至 " + normalizedEndDate;
    }

    private StatisticsExportFileVO buildCsvExportFile(String filename, String csv) {
        StatisticsExportFileVO file = new StatisticsExportFileVO();
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[3 + csvBytes.length];
        content[0] = (byte) 0xEF;
        content[1] = (byte) 0xBB;
        content[2] = (byte) 0xBF;
        System.arraycopy(csvBytes, 0, content, 3, csvBytes.length);
        file.setFilename(filename);
        file.setContentType(CSV_CONTENT_TYPE);
        file.setContent(content);
        return file;
    }

    private StatisticsExportFileVO buildXlsxExportFile(String filename, byte[] content) {
        StatisticsExportFileVO file = new StatisticsExportFileVO();
        file.setFilename(filename);
        file.setContentType(XLSX_CONTENT_TYPE);
        file.setContent(content);
        return file;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSectionStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorder(style);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorder(style);
        return style;
    }

    private CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorder(style);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorder(style);
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        applyThinBorder(style);
        return style;
    }

    private void applyThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private String formatAuditOperationType(String type) {
        String normalized = StringUtils.hasText(type) ? type.trim().toUpperCase() : "";
        if ("LOGIN".equals(normalized)) {
            return "登录";
        }
        if ("LOGOUT".equals(normalized)) {
            return "退出登录";
        }
        if ("CHECKIN".equals(normalized)) {
            return "上班打卡";
        }
        if ("CHECKOUT".equals(normalized)) {
            return "下班打卡";
        }
        if ("ATTENDANCE_APPLY".equals(normalized)) {
            return "补卡申请";
        }
        if ("WARNING_REEVALUATE".equals(normalized)) {
            return "预警处理";
        }
        if ("REVIEW_SUBMIT".equals(normalized)) {
            return "复核办理";
        }
        if ("REVIEW_FEEDBACK".equals(normalized)) {
            return "复核补充";
        }
        if ("SYSTEM_CONFIG".equals(normalized)) {
            return "系统配置";
        }
        return StringUtils.hasText(type) ? type : "其他操作";
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        String safeValue = value;
        if (safeValue.startsWith("=") || safeValue.startsWith("+") || safeValue.startsWith("-") || safeValue.startsWith("@")) {
            safeValue = "'" + safeValue;
        }
        boolean needQuote = safeValue.contains(",") || safeValue.contains("\n") || safeValue.contains("\r") || safeValue.contains("\"");
        safeValue = safeValue.replace("\"", "\"\"");
        return needQuote ? "\"" + safeValue + "\"" : safeValue;
    }

    private String nullableLongValue(Long value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<OperationLogVO> selectAllOperationLogs(OperationLogQueryDTO dto) {
        return operationLogService.listAll(dto);
    }
}
