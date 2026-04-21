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
import com.quyong.attendance.module.statistics.vo.ExceptionTypeTrendItemVO;
import com.quyong.attendance.module.statistics.vo.ExceptionTypeTrendVO;
import com.quyong.attendance.module.statistics.vo.OperationLogVO;
import com.quyong.attendance.module.statistics.vo.PersonalStatisticsVO;
import com.quyong.attendance.module.statistics.vo.StatisticsExportFileVO;
import com.quyong.attendance.module.statistics.vo.StatisticsSummaryVO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.support.UserValidationSupport;
import com.quyong.attendance.module.warning.service.WarningService;
import com.quyong.attendance.module.review.support.ExceptionTypeCatalogService;
import com.quyong.attendance.module.warning.vo.WarningDashboardVO;
import com.quyong.attendance.module.warning.vo.WarningExceptionTrendItemVO;
import com.quyong.attendance.module.warning.vo.WarningOverdueItemVO;
import com.quyong.attendance.module.warning.vo.WarningRankingItemVO;
import com.quyong.attendance.module.warning.vo.WarningUserPortraitVO;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final WarningService warningService;
    private final ExceptionTypeCatalogService exceptionTypeCatalogService;

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper,
                                 StatisticsValidationSupport statisticsValidationSupport,
                                 StatisticsSummarySupport statisticsSummarySupport,
                                 UserValidationSupport userValidationSupport,
                                 DepartmentMapper departmentMapper,
                                 OperationLogService operationLogService,
                                 WarningService warningService,
                                 ExceptionTypeCatalogService exceptionTypeCatalogService) {
        this.statisticsMapper = statisticsMapper;
        this.statisticsValidationSupport = statisticsValidationSupport;
        this.statisticsSummarySupport = statisticsSummarySupport;
        this.userValidationSupport = userValidationSupport;
        this.departmentMapper = departmentMapper;
        this.operationLogService = operationLogService;
        this.warningService = warningService;
        this.exceptionTypeCatalogService = exceptionTypeCatalogService;
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
    public ExceptionTypeTrendVO exceptionTypeTrend(ExceptionTrendQueryDTO dto) {
        ExceptionTrendQueryDTO safe = statisticsValidationSupport.validateTrend(dto);
        if (safe.getDeptId() != null) {
            requireDepartment(safe.getDeptId());
        }
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        List<Map<String, Object>> rows = statisticsMapper.selectExceptionTypeTrendRows(safe.getDeptId(), startTime, endTime);

        Set<String> labelSet = new LinkedHashSet<String>();
        Map<String, Map<String, Long> > trendMap = new LinkedHashMap<String, Map<String, Long> >();
        Map<String, Long> totalMap = new LinkedHashMap<String, Long>();

        for (Map<String, Object> row : rows) {
            String bucket = normalizeBucket(String.valueOf(valueOf(row, "bucket")), safe.getPeriodType());
            String type = String.valueOf(valueOf(row, "type"));
            Long total = Long.valueOf(numberValue(row, "total"));
            labelSet.add(bucket);

            Map<String, Long> itemMap = trendMap.get(type);
            if (itemMap == null) {
                itemMap = new LinkedHashMap<String, Long>();
                trendMap.put(type, itemMap);
            }
            itemMap.put(bucket, Long.valueOf((itemMap.containsKey(bucket) ? itemMap.get(bucket).longValue() : 0L) + total.longValue()));
            totalMap.put(type, Long.valueOf((totalMap.containsKey(type) ? totalMap.get(type).longValue() : 0L) + total.longValue()));
        }

        List<String> labels = new ArrayList<String>(labelSet);
        List<ExceptionTypeTrendItemVO> items = new ArrayList<ExceptionTypeTrendItemVO>();
        for (Map.Entry<String, Long> entry : totalMap.entrySet()) {
            ExceptionTypeTrendItemVO item = new ExceptionTypeTrendItemVO();
            item.setType(entry.getKey());
            item.setName(exceptionTypeCatalogService.resolveName(entry.getKey()));
            item.setTotalCount(entry.getValue());
            Map<String, Long> itemMap = trendMap.get(entry.getKey());
            List<Long> values = new ArrayList<Long>(labels.size());
            for (String label : labels) {
                values.add(itemMap == null || !itemMap.containsKey(label) ? Long.valueOf(0L) : itemMap.get(label));
            }
            item.setValues(values);
            items.add(item);
        }
        items.sort(Comparator.comparing(ExceptionTypeTrendItemVO::getTotalCount, Comparator.nullsFirst(Long::compareTo)).reversed());
        if (items.size() > 5) {
            items = new ArrayList<ExceptionTypeTrendItemVO>(items.subList(0, 5));
        }

        ExceptionTypeTrendVO vo = new ExceptionTypeTrendVO();
        vo.setPeriodType(safe.getPeriodType());
        vo.setLabels(labels);
        vo.setItems(items);
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
        if ("WARNING_DASHBOARD".equals(safe.getExportType())) {
            WarningDashboardVO dashboard = warningService.dashboard();
            return buildCsvExportFile("预警看板报表.csv", buildWarningDashboardCsv(dashboard));
        }
        OperationLogQueryDTO queryDTO = new OperationLogQueryDTO();
        queryDTO.setUserId(safe.getUserId());
        queryDTO.setType(safe.getType());
        queryDTO.setTypes(safe.getTypes());
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
                .append("人员编号,姓名,部门编号,考勤记录数,异常记录数,系统处理次数,风险预警数,人工复核数,已完成处置数,待处理预警数,处置完成率\n")
                .append(personal.getUserId()).append(',')
                .append(csvValue(personal.getRealName())).append(',')
                .append(personal.getDeptId()).append(',')
                .append(personal.getRecordCount()).append(',')
                .append(personal.getExceptionCount()).append(',')
                .append(personal.getAnalysisCount()).append(',')
                .append(personal.getWarningCount()).append(',')
                .append(personal.getReviewCount()).append(',')
                .append(personal.getClosedLoopCount()).append(',')
                .append(Math.max(safeLongValue(personal.getWarningCount()) - safeLongValue(personal.getClosedLoopCount()), 0L)).append(',')
                .append(formatPercent(calculateRatioValue(personal.getClosedLoopCount(), personal.getWarningCount())))
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
                .append("部门范围,考勤记录数,异常记录数,系统处理次数,风险预警数,人工复核数,已完成处置数\n")
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
                .append("序号,部门编号,部门名称,考勤记录数,异常记录数,系统处理次数,风险预警数,人工复核数,已完成处置数\n");

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
            CellStyle wrapTextStyle = createWrapTextStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0).setCellValue("部门统计报表");
            titleRow.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 16));

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
            rowIndex = writeDepartmentDetailSection(sheet, rowIndex, departments, sectionStyle, headerStyle, textStyle, numberStyle, percentStyle);
            rowIndex++;
            writeDepartmentDefinitionSection(sheet, rowIndex, sectionStyle, headerStyle, textStyle, wrapTextStyle);

            for (int columnIndex = 0; columnIndex <= 16; columnIndex++) {
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
        builder.append("日期,考勤记录数,异常记录数,系统处理次数,风险预警数,人工复核数,已完成处置数\n");
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
        builder.append("记录编号,办理人编号,账号,姓名,办理动作,办理内容,办理时间\n");
        for (OperationLogVO record : records) {
            builder.append(record.getId()).append(',')
                    .append(record.getUserId()).append(',')
                    .append(csvValue(record.getUsername())).append(',')
                    .append(csvValue(record.getRealName())).append(',')
                    .append(csvValue(formatAuditOperationType(record.getType()))).append(',')
                    .append(csvValue(record.getContent())).append(',')
                    .append(record.getOperationTime())
                    .append('\n');
        }
        return builder.toString();
    }

    private String buildWarningDashboardCsv(WarningDashboardVO dashboard) {
        StringBuilder builder = new StringBuilder();
        builder.append("模块,指标,值\n")
                .append("预警看板,预警总量,").append(nullableLongValue(dashboard.getTotalCount())).append('\n')
                .append("预警看板,已处理预警,").append(nullableLongValue(dashboard.getProcessedCount())).append('\n')
                .append("预警看板,待处理积压,").append(nullableLongValue(dashboard.getUnprocessedCount())).append('\n')
                .append("预警看板,超时预警,").append(nullableLongValue(dashboard.getOverdueCount())).append('\n')
                .append("预警看板,超时24-48小时,").append(nullableLongValue(dashboard.getOverdue24To48Count())).append('\n')
                .append("预警看板,超时48-72小时,").append(nullableLongValue(dashboard.getOverdue48To72Count())).append('\n')
                .append("预警看板,超时72小时以上,").append(nullableLongValue(dashboard.getOverdueOver72Count())).append('\n')
                .append("预警看板,关键风险人员,").append(nullableLongValue(dashboard.getCriticalRiskUserCount())).append('\n')
                .append("预警看板,高风险人员,").append(nullableLongValue(dashboard.getHighRiskUserCount())).append('\n')
                .append("预警看板,中风险人员,").append(nullableLongValue(dashboard.getMediumRiskUserCount())).append('\n')
                .append("预警看板,低风险人员,").append(nullableLongValue(dashboard.getLowRiskUserCount())).append('\n')
                .append("预警看板,平均处置时长(分钟),").append(nullableBigDecimalValue(dashboard.getAverageProcessMinutes())).append('\n')
                .append("预警看板,SLA目标(小时),").append(nullableIntegerValue(dashboard.getSlaTargetHours())).append('\n')
                .append("预警看板,按时关闭,").append(nullableLongValue(dashboard.getWithinSlaCount())).append('\n')
                .append("预警看板,超时关闭,").append(nullableLongValue(dashboard.getOverSlaCount())).append('\n')
                .append("预警看板,SLA按时关闭率(%),").append(nullableBigDecimalValue(dashboard.getWithinSlaRate())).append('\n')
                .append('\n')
                .append("高风险人员排行,人员,预警数,高风险数\n");

        for (WarningRankingItemVO item : safeList(dashboard.getTopRiskUsers())) {
            builder.append("高风险人员排行,")
                    .append(csvValue(item.getLabel())).append(',')
                    .append(nullableLongValue(item.getCount())).append(',')
                    .append(nullableLongValue(item.getHighRiskCount())).append('\n');
        }

        builder.append('\n').append("异常类型排行,异常类型,总次数,高风险次数\n");
        for (WarningRankingItemVO item : safeList(dashboard.getTopExceptionTypes())) {
            builder.append("异常类型排行,")
                    .append(csvValue(item.getLabel())).append(',')
                    .append(nullableLongValue(item.getCount())).append(',')
                    .append(nullableLongValue(item.getHighRiskCount())).append('\n');
        }

        builder.append('\n').append("异常类型趋势,异常类型,每日分布\n");
        for (WarningExceptionTrendItemVO item : safeList(dashboard.getExceptionTrendItems())) {
            builder.append("异常类型趋势,")
                    .append(csvValue(item.getName() == null ? item.getType() : item.getName())).append(',')
                    .append(csvValue(joinDailyCounts(item.getDailyCounts())))
                    .append('\n');
        }

        builder.append('\n').append("异常人员画像,人员,风险层级,总预警,高风险,待处理,超时,最新异常\n");
        for (WarningUserPortraitVO item : safeList(dashboard.getUserPortraits())) {
            String displayName = item.getRealName() == null ? item.getUsername() : item.getRealName() + "（" + item.getUsername() + "）";
            builder.append("异常人员画像,")
                    .append(csvValue(displayName)).append(',')
                    .append(csvValue(item.getRiskTier())).append(',')
                    .append(nullableLongValue(item.getTotalWarnings())).append(',')
                    .append(nullableLongValue(item.getHighRiskWarnings())).append(',')
                    .append(nullableLongValue(item.getUnprocessedWarnings())).append(',')
                    .append(nullableLongValue(item.getOverdueWarnings())).append(',')
                    .append(csvValue(item.getLatestExceptionTypeName() == null ? item.getLatestExceptionType() : item.getLatestExceptionTypeName())).append('\n');
        }

        builder.append('\n').append("处置超时提醒,预警编号,异常编号,标题,人员,超时分钟\n");
        for (WarningOverdueItemVO item : safeList(dashboard.getOverdueItems())) {
            builder.append("处置超时提醒,")
                    .append(nullableLongValue(item.getWarningId())).append(',')
                    .append(nullableLongValue(item.getExceptionId())).append(',')
                    .append(csvValue(item.getTitle())).append(',')
                    .append(csvValue(item.getRealName())).append(',')
                    .append(nullableLongValue(item.getOverdueMinutes())).append('\n');
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
        sheet.addMergedRegion(new CellRangeAddress(sectionRow.getRowNum(), sectionRow.getRowNum(), 0, 14));

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = new String[] {
                "部门范围", "考勤记录数", "异常记录数", "系统处理次数", "风险预警数", "人工复核数", "已完成处置数",
                "待处理预警数", "高风险异常数", "异常率", "系统处理覆盖率", "预警触发率", "复核覆盖率", "处置完成率", "待处理占比"
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

    private int writeDepartmentDetailSection(Sheet sheet,
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
        sheet.addMergedRegion(new CellRangeAddress(sectionRow.getRowNum(), sectionRow.getRowNum(), 0, 16));

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = new String[] {
                "序号", "部门编号", "部门名称", "考勤记录数", "异常记录数", "系统处理次数", "风险预警数", "人工复核数", "已完成处置数",
                "待处理预警数", "高风险异常数", "异常率", "系统处理覆盖率", "预警触发率", "复核覆盖率", "处置完成率", "待处理占比"
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
        return rowIndex;
    }

    private int writeDepartmentDefinitionSection(Sheet sheet,
                                                 int rowIndex,
                                                 CellStyle sectionStyle,
                                                 CellStyle headerStyle,
                                                 CellStyle textStyle,
                                                 CellStyle wrapTextStyle) {
        Row sectionRow = sheet.createRow(rowIndex++);
        sectionRow.createCell(0).setCellValue("指标说明");
        sectionRow.getCell(0).setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(sectionRow.getRowNum(), sectionRow.getRowNum(), 0, 16));

        Row headerRow = sheet.createRow(rowIndex++);
        Cell indicatorHeader = headerRow.createCell(0);
        indicatorHeader.setCellValue("指标");
        indicatorHeader.setCellStyle(headerStyle);
        Cell descriptionHeader = headerRow.createCell(1);
        descriptionHeader.setCellValue("说明");
        descriptionHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(headerRow.getRowNum(), headerRow.getRowNum(), 1, 16));

        String[][] definitions = new String[][] {
                {"系统处理次数", "系统完成异常识别与分析的次数（含规则判定与智能分析）。"},
                {"风险预警数", "系统生成并发送给管理人员的风险预警次数。"},
                {"人工复核数", "管理人员完成人工核查并提交复核结论的次数。"},
                {"已完成处置数", "风险预警已完成复核并形成处理结论的次数（原“闭环记录数”）。"},
                {"处置完成率", "已完成处置数 / 风险预警数（原“闭环率”）。"},
                {"待处理预警数", "风险预警数 - 已完成处置数，用于识别当前待办压力。"},
                {"待处理占比", "待处理预警数 / 风险预警数。"},
                {"高风险异常数", "风险等级为高风险的异常数量。"}
        };

        for (String[] definition : definitions) {
            Row row = sheet.createRow(rowIndex++);
            Cell indicatorCell = row.createCell(0);
            indicatorCell.setCellValue(definition[0]);
            indicatorCell.setCellStyle(textStyle);
            Cell descriptionCell = row.createCell(1);
            descriptionCell.setCellValue(definition[1]);
            descriptionCell.setCellStyle(wrapTextStyle);
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 1, 16));
        }
        return rowIndex;
    }

    private void writeDepartmentStatisticsCells(Row row,
                                                DepartmentStatisticsVO department,
                                                boolean includeDeptId,
                                                CellStyle textStyle,
                                                CellStyle numberStyle,
                                                CellStyle percentStyle) {
        long warningCount = safeLongValue(department.getWarningCount());
        long completedCount = safeLongValue(department.getClosedLoopCount());
        long pendingWarningCount = Math.max(warningCount - completedCount, 0L);
        long highRiskCount = distributionCount(department.getRiskLevelDistribution(), "HIGH");

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
        writeLongCell(row, startColumn + 6, Long.valueOf(pendingWarningCount), numberStyle);
        writeLongCell(row, startColumn + 7, Long.valueOf(highRiskCount), numberStyle);
        writeRatioCell(row, startColumn + 8, department.getExceptionCount(), department.getRecordCount(), percentStyle);
        writeRatioCell(row, startColumn + 9, department.getAnalysisCount(), department.getExceptionCount(), percentStyle);
        writeRatioCell(row, startColumn + 10, department.getWarningCount(), department.getExceptionCount(), percentStyle);
        writeRatioCell(row, startColumn + 11, department.getReviewCount(), department.getWarningCount(), percentStyle);
        writeRatioCell(row, startColumn + 12, department.getClosedLoopCount(), department.getWarningCount(), percentStyle);
        writeRatioCell(row, startColumn + 13, Long.valueOf(pendingWarningCount), department.getWarningCount(), percentStyle);
    }

    private long safeLongValue(Long value) {
        return value == null ? 0L : value.longValue();
    }

    private long distributionCount(Map<String, Long> distribution, String key) {
        if (distribution == null || !StringUtils.hasText(key)) {
            return 0L;
        }
        Long count = distribution.get(key);
        return count == null ? 0L : count.longValue();
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

    private String formatPercent(double ratioValue) {
        return new BigDecimal(ratioValue * 100D).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "%";
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

    private CellStyle createWrapTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
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
        if ("LOGIN_FAILURE".equals(normalized)) {
            return "登录失败";
        }
        if ("LOGIN_LOCKED".equals(normalized)) {
            return "登录锁定";
        }
        if ("LOGOUT".equals(normalized)) {
            return "退出登录";
        }
        if ("TOKEN_REFRESH".equals(normalized)) {
            return "刷新令牌";
        }
        if ("TOKEN_REFRESH_FAILURE".equals(normalized)) {
            return "刷新失败";
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
        if ("FACE_REGISTER_APPLY".equals(normalized)) {
            return "人脸重录申请";
        }
        if ("FACE_REGISTER_APPROVE".equals(normalized)) {
            return "人脸重录通过";
        }
        if ("FACE_REGISTER_REJECT".equals(normalized)) {
            return "人脸重录驳回";
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
        if ("FACE_LIVENESS_SESSION".equals(normalized)) {
            return "活体会话创建";
        }
        if ("FACE_LIVENESS_PASS".equals(normalized)) {
            return "活体挑战通过";
        }
        if ("FACE_LIVENESS_FAIL".equals(normalized)) {
            return "活体挑战失败";
        }
        if ("FACE_LIVENESS_REJECT".equals(normalized)) {
            return "活体证明拒绝";
        }
        if ("FACE_LIVENESS_CONSUME".equals(normalized)) {
            return "活体证明消费";
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

    private String nullableIntegerValue(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullableBigDecimalValue(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String joinDailyCounts(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append('/');
            }
            builder.append(nullableLongValue(values.get(index)));
        }
        return builder.toString();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.<T>emptyList() : values;
    }

    private List<OperationLogVO> selectAllOperationLogs(OperationLogQueryDTO dto) {
        return operationLogService.listAll(dto);
    }
}
