package com.quyong.attendance.module.statistics.service.impl;

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
import com.quyong.attendance.module.statistics.vo.StatisticsSummaryVO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.support.UserValidationSupport;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final long AUDIT_EXPORT_MAX_ROWS = 5000L;

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

        DepartmentStatisticsVO vo = new DepartmentStatisticsVO();
        vo.setDeptId(null);
        vo.setDeptName("全部部门");
        vo.setRecordCount(Long.valueOf(statisticsMapper.countRecords(null, null, startTime, endTime)));
        vo.setExceptionCount(Long.valueOf(statisticsMapper.countExceptions(null, null, startTime, endTime)));
        vo.setAnalysisCount(Long.valueOf(statisticsMapper.countAnalyses(null, null, startTime, endTime)));
        vo.setWarningCount(Long.valueOf(statisticsMapper.countWarnings(null, null, startTime, endTime)));
        vo.setReviewCount(Long.valueOf(statisticsMapper.countReviews(null, null, startTime, endTime)));
        vo.setClosedLoopCount(Long.valueOf(statisticsMapper.countClosedLoops(null, null, startTime, endTime)));
        vo.setExceptionTypeDistribution(toDistributionMap(statisticsMapper.selectExceptionTypeDistribution(null, null, startTime, endTime)));
        vo.setRiskLevelDistribution(toDistributionMap(statisticsMapper.selectRiskLevelDistribution(null, null, startTime, endTime)));
        vo.setWarningStatusDistribution(toDistributionMap(statisticsMapper.selectWarningStatusDistribution(null, null, startTime, endTime)));
        vo.setReviewResultDistribution(toDistributionMap(statisticsMapper.selectReviewResultDistribution(null, null, startTime, endTime)));
        return vo;
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
        vo.setSummary(statisticsSummarySupport.buildSummary(recordCount, exceptionCount, analysisCount, warningCount, reviewCount, closedLoopCount));
        vo.setHighlightRisks(statisticsSummarySupport.buildHighlightRisks(highRiskCount, analysisGapCount, unprocessedWarningCount, closedLoopGapCount, missingDecisionTraceCount, missingModelLogCount));
        vo.setManageSuggestion(statisticsSummarySupport.buildManageSuggestion(highRiskCount, unprocessedWarningCount, closedLoopGapCount));
        return vo;
    }

    @Override
    public DepartmentRiskBriefVO departmentRiskBrief(DepartmentRiskBriefQueryDTO dto) {
        DepartmentRiskBriefQueryDTO safe = statisticsValidationSupport.validateDepartmentRiskBrief(dto);
        Department department = requireDepartment(safe.getDeptId());
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        Long recordCount = Long.valueOf(statisticsMapper.countRecords(null, department.getId(), startTime, endTime));
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
        vo.setRiskSummary(statisticsSummarySupport.buildDepartmentRiskSummary(department.getName(), highRiskCount, unprocessedWarningCount, closedLoopGapCount));
        vo.setManageSuggestion(statisticsSummarySupport.buildDepartmentManageSuggestion(highRiskCount, unprocessedWarningCount, closedLoopGapCount));
        return vo;
    }

    @Override
    public String export(StatisticsExportQueryDTO dto) {
        StatisticsExportQueryDTO safe = statisticsValidationSupport.validateExport(dto);
        if ("PERSONAL".equals(safe.getExportType())) {
            PersonalStatisticsQueryDTO personalQueryDTO = new PersonalStatisticsQueryDTO();
            personalQueryDTO.setStartDate(safe.getStartDate());
            personalQueryDTO.setEndDate(safe.getEndDate());
            PersonalStatisticsVO personal = personal(personalQueryDTO);
            return buildPersonalCsv(personal);
        }
        if ("DEPARTMENT".equals(safe.getExportType())) {
            DepartmentStatisticsQueryDTO departmentQueryDTO = new DepartmentStatisticsQueryDTO();
            departmentQueryDTO.setStartDate(safe.getStartDate());
            departmentQueryDTO.setEndDate(safe.getEndDate());
            DepartmentStatisticsVO department = department(departmentQueryDTO);
            return buildDepartmentCsv(department);
        }
        if ("TREND".equals(safe.getExportType())) {
            ExceptionTrendQueryDTO trendQueryDTO = new ExceptionTrendQueryDTO();
            trendQueryDTO.setDeptId(safe.getDeptId());
            trendQueryDTO.setStartDate(safe.getStartDate());
            trendQueryDTO.setEndDate(safe.getEndDate());
            trendQueryDTO.setPeriodType(safe.getPeriodType());
            ExceptionTrendVO trendVO = exceptionTrend(trendQueryDTO);
            return buildTrendCsv(trendVO);
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
        return buildAuditCsv(records);
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
                .append("userId,realName,deptId,recordCount,exceptionCount,warningCount,reviewCount,closedLoopCount\n")
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

    private String buildDepartmentCsv(DepartmentStatisticsVO department) {
        return new StringBuilder()
                .append("deptId,deptName,recordCount,exceptionCount,analysisCount,warningCount,reviewCount,closedLoopCount\n")
                .append(nullableLongValue(department.getDeptId())).append(',')
                .append(csvValue(department.getDeptName())).append(',')
                .append(department.getRecordCount()).append(',')
                .append(department.getExceptionCount()).append(',')
                .append(department.getAnalysisCount()).append(',')
                .append(department.getWarningCount()).append(',')
                .append(department.getReviewCount()).append(',')
                .append(department.getClosedLoopCount())
                .append('\n')
                .toString();
    }

    private String buildTrendCsv(ExceptionTrendVO trendVO) {
        StringBuilder builder = new StringBuilder();
        builder.append("date,recordCount,exceptionCount,analysisCount,warningCount,reviewCount,closedLoopCount\n");
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
        builder.append("id,userId,type,content,operationTime\n");
        for (OperationLogVO record : records) {
            builder.append(record.getId()).append(',')
                    .append(record.getUserId()).append(',')
                    .append(csvValue(record.getType())).append(',')
                    .append(csvValue(record.getContent())).append(',')
                    .append(record.getOperationTime())
                    .append('\n');
        }
        return builder.toString();
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
