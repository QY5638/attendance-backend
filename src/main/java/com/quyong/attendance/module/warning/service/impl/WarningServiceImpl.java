package com.quyong.attendance.module.warning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.attendance.entity.AttendanceRecord;
import com.quyong.attendance.module.attendance.mapper.AttendanceRecordMapper;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.exceptiondetect.entity.AttendanceException;
import com.quyong.attendance.module.exceptiondetect.entity.ExceptionAnalysis;
import com.quyong.attendance.module.exceptiondetect.entity.Rule;
import com.quyong.attendance.module.exceptiondetect.mapper.AttendanceExceptionMapper;
import com.quyong.attendance.module.exceptiondetect.mapper.ExceptionAnalysisMapper;
import com.quyong.attendance.module.exceptiondetect.service.RuleService;
import com.quyong.attendance.module.notification.config.NotificationProperties;
import com.quyong.attendance.module.notification.dto.NotificationCreateCommand;
import com.quyong.attendance.module.notification.service.NotificationService;
import com.quyong.attendance.module.review.entity.ReviewRecord;
import com.quyong.attendance.module.review.mapper.ReviewRecordMapper;
import com.quyong.attendance.module.review.support.ExceptionTypeCatalogService;
import com.quyong.attendance.module.role.entity.Role;
import com.quyong.attendance.module.role.mapper.RoleMapper;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import com.quyong.attendance.module.warning.dto.RiskLevelQueryDTO;
import com.quyong.attendance.module.warning.dto.RiskLevelUpdateDTO;
import com.quyong.attendance.module.warning.dto.WarningQueryDTO;
import com.quyong.attendance.module.warning.dto.WarningReplyDTO;
import com.quyong.attendance.module.warning.dto.WarningRequestExplanationDTO;
import com.quyong.attendance.module.warning.dto.WarningReevaluateDTO;
import com.quyong.attendance.module.warning.entity.WarningInteractionRecord;
import com.quyong.attendance.module.warning.entity.WarningRecord;
import com.quyong.attendance.module.warning.mapper.WarningInteractionRecordMapper;
import com.quyong.attendance.module.warning.mapper.WarningRecordMapper;
import com.quyong.attendance.module.warning.service.WarningService;
import com.quyong.attendance.module.warning.support.RiskLevelRegistry;
import com.quyong.attendance.module.warning.support.WarningValidationSupport;
import com.quyong.attendance.module.warning.vo.RiskLevelConfigVO;
import com.quyong.attendance.module.warning.vo.WarningAdviceVO;
import com.quyong.attendance.module.warning.vo.WarningDashboardVO;
import com.quyong.attendance.module.warning.vo.WarningExceptionTrendItemVO;
import com.quyong.attendance.module.warning.vo.WarningInteractionVO;
import com.quyong.attendance.module.warning.vo.WarningOverdueItemVO;
import com.quyong.attendance.module.warning.vo.WarningRankingItemVO;
import com.quyong.attendance.module.warning.vo.WarningTrendPointVO;
import com.quyong.attendance.module.warning.vo.WarningUserPortraitVO;
import com.quyong.attendance.module.warning.vo.WarningVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WarningServiceImpl implements WarningService {

    private static final String EXCEPTION_STATUS_REVIEWED = "REVIEWED";
    private static final String TYPE_RISK_WARNING = "RISK_WARNING";
    private static final String TYPE_ATTENDANCE_WARNING = "ATTENDANCE_WARNING";
    private static final String STATUS_UNPROCESSED = "UNPROCESSED";
    private static final String STATUS_PROCESSED = "PROCESSED";
    private static final String SOURCE_RULE = "RULE";
    private static final String SOURCE_MODEL = "MODEL";
    private static final String SOURCE_MODEL_FALLBACK = "MODEL_FALLBACK";
    private static final String DECISION_SOURCE_RULE = "RULE";
    private static final String DECISION_SOURCE_MODEL_FUSION = "MODEL_FUSION";
    private static final String INTERACTION_NONE = "NONE";
    private static final String INTERACTION_WAIT_EMPLOYEE_REPLY = "WAIT_EMPLOYEE_REPLY";
    private static final String INTERACTION_EMPLOYEE_REPLIED = "EMPLOYEE_REPLIED";
    private static final String INTERACTION_RESULT_SENT = "RESULT_SENT";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ROLE_SYSTEM = "SYSTEM";
    private static final String BUSINESS_TYPE_WARNING = "WARNING";
    private static final String ATTENDANCE_EXCEPTION_BUSINESS_TYPE = "ATTENDANCE_EXCEPTION";
    private static final String CATEGORY_EXCEPTION_NOTICE = "EXCEPTION_NOTICE";
    private static final String CATEGORY_WARNING_CREATED = "WARNING_CREATED";
    private static final String CATEGORY_REQUEST_EXPLANATION = "REQUEST_EXPLANATION";
    private static final String CATEGORY_EMPLOYEE_REPLY = "EMPLOYEE_REPLY";
    private static final String CATEGORY_REVIEW_RESULT = "REVIEW_RESULT";
    private static final String CATEGORY_EMPLOYEE_REPLY_REMINDER = "EMPLOYEE_REPLY_REMINDER";
    private static final String CATEGORY_WARNING_OVERDUE_REMINDER = "WARNING_OVERDUE_REMINDER";
    private static final String ACTION_VIEW = "VIEW";
    private static final String ACTION_REPLY = "REPLY";
    private static final String ACTION_REVIEW = "REVIEW";
    private static final String MESSAGE_TYPE_SYSTEM_NOTICE = "SYSTEM_NOTICE";
    private static final String MESSAGE_TYPE_REQUEST_EXPLANATION = "REQUEST_EXPLANATION";
    private static final String MESSAGE_TYPE_EMPLOYEE_REPLY = "EMPLOYEE_REPLY";
    private static final String MESSAGE_TYPE_REVIEW_RESULT = "REVIEW_RESULT";
    private static final String MESSAGE_TYPE_REMINDER = "REMINDER";
    private static final String ABSENT = "ABSENT";
    private static final String MISSING_CHECKOUT = "MISSING_CHECKOUT";

    private final WarningRecordMapper warningRecordMapper;
    private final AttendanceExceptionMapper attendanceExceptionMapper;
    private final ExceptionAnalysisMapper exceptionAnalysisMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final WarningInteractionRecordMapper warningInteractionRecordMapper;
    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;
    private final RoleMapper roleMapper;
    private final RuleService ruleService;
    private final UserMapper userMapper;
    private final ExceptionTypeCatalogService exceptionTypeCatalogService;
    private final WarningValidationSupport warningValidationSupport;
    private final RiskLevelRegistry riskLevelRegistry;
    private final OperationLogService operationLogService;
    private final Clock clock;

    public WarningServiceImpl(WarningRecordMapper warningRecordMapper,
                              AttendanceExceptionMapper attendanceExceptionMapper,
                              ExceptionAnalysisMapper exceptionAnalysisMapper,
                              AttendanceRecordMapper attendanceRecordMapper,
                              ReviewRecordMapper reviewRecordMapper,
                              WarningInteractionRecordMapper warningInteractionRecordMapper,
                              NotificationService notificationService,
                              NotificationProperties notificationProperties,
                              RoleMapper roleMapper,
                              RuleService ruleService,
                              UserMapper userMapper,
                              ExceptionTypeCatalogService exceptionTypeCatalogService,
                              WarningValidationSupport warningValidationSupport,
                              RiskLevelRegistry riskLevelRegistry,
                              OperationLogService operationLogService,
                              Clock clock) {
        this.warningRecordMapper = warningRecordMapper;
        this.attendanceExceptionMapper = attendanceExceptionMapper;
        this.exceptionAnalysisMapper = exceptionAnalysisMapper;
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.warningInteractionRecordMapper = warningInteractionRecordMapper;
        this.notificationService = notificationService;
        this.notificationProperties = notificationProperties;
        this.roleMapper = roleMapper;
        this.ruleService = ruleService;
        this.userMapper = userMapper;
        this.exceptionTypeCatalogService = exceptionTypeCatalogService;
        this.warningValidationSupport = warningValidationSupport;
        this.riskLevelRegistry = riskLevelRegistry;
        this.operationLogService = operationLogService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PageResult<WarningVO> list(WarningQueryDTO queryDTO) {
        WarningQueryDTO validatedDTO = warningValidationSupport.validateQuery(queryDTO);
        ensureWarningsGenerated();
        int offset = (validatedDTO.getPageNum().intValue() - 1) * validatedDTO.getPageSize().intValue();
        LocalDateTime overdueCutoff = LocalDateTime.now().minusHours(24L);
        long total = warningRecordMapper.countByQuery(validatedDTO.getUserId(), validatedDTO.getLevel(), validatedDTO.getStatus(), validatedDTO.getType());
        List<WarningRecord> entities = warningRecordMapper.selectPageByQuery(
                validatedDTO.getUserId(),
                validatedDTO.getLevel(),
                validatedDTO.getStatus(),
                validatedDTO.getType(),
                overdueCutoff,
                validatedDTO.getPageSize().intValue(),
                offset
        );
        List<WarningVO> records = new ArrayList<WarningVO>();
        for (WarningRecord entity : entities) {
            records.add(toVO(entity));
        }
        return new PageResult<WarningVO>(Long.valueOf(total), records);
    }

    @Override
    public WarningDashboardVO dashboard() {
        ensureWarningsGenerated();
        int recentDays = 7;
        long overdueThresholdMinutes = 24L * 60L;
        int slaTargetHours = 24;
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = today.minusDays(recentDays - 1L).atStartOfDay();
        List<WarningRecord> warnings = warningRecordMapper.selectBySendTimeSince(startTime);

        Map<LocalDate, WarningTrendPointVO> trendMap = new LinkedHashMap<LocalDate, WarningTrendPointVO>();
        for (int index = 0; index < recentDays; index++) {
            LocalDate day = today.minusDays(recentDays - 1L - index);
            WarningTrendPointVO point = new WarningTrendPointVO();
            point.setDateLabel(day.toString());
            point.setTotalCount(Long.valueOf(0L));
            point.setProcessedCount(Long.valueOf(0L));
            point.setUnprocessedCount(Long.valueOf(0L));
            point.setHighRiskCount(Long.valueOf(0L));
            trendMap.put(day, point);
        }

        long totalCount = 0L;
        long processedCount = 0L;
        long unprocessedCount = 0L;
        long highRiskCount = 0L;
        long overdueCount = 0L;
        long overdue24To48Count = 0L;
        long overdue48To72Count = 0L;
        long overdueOver72Count = 0L;
        long criticalRiskUserCount = 0L;
        long highRiskUserCount = 0L;
        long mediumRiskUserCount = 0L;
        long lowRiskUserCount = 0L;
        long withinSlaCount = 0L;
        long overSlaCount = 0L;
        BigDecimal totalProcessMinutes = BigDecimal.ZERO;
        int processedWithReviewCount = 0;
        Map<Long, WarningRankingItemVO> userRankingMap = new LinkedHashMap<Long, WarningRankingItemVO>();
        Map<String, WarningRankingItemVO> typeRankingMap = new LinkedHashMap<String, WarningRankingItemVO>();
        Map<String, long[]> exceptionTrendMap = new LinkedHashMap<String, long[]>();
        Map<Long, WarningUserPortraitVO> userPortraitMap = new LinkedHashMap<Long, WarningUserPortraitVO>();
        List<WarningOverdueItemVO> overdueItems = new ArrayList<WarningOverdueItemVO>();

        for (WarningRecord warning : warnings) {
            if (warning.getSendTime() == null) {
                continue;
            }
            LocalDate day = warning.getSendTime().toLocalDate();
            WarningTrendPointVO point = trendMap.get(day);
            if (point == null) {
                continue;
            }

            totalCount++;
            point.setTotalCount(Long.valueOf(point.getTotalCount().longValue() + 1L));

            AttendanceException attendanceException = warning.getExceptionId() == null ? null : attendanceExceptionMapper.selectById(warning.getExceptionId());
            User targetUser = attendanceException == null || attendanceException.getUserId() == null ? null : userMapper.selectById(attendanceException.getUserId());
            accumulateUserRanking(userRankingMap, warning, targetUser);
            accumulateUserPortrait(userPortraitMap, warning, attendanceException, targetUser);
            accumulateTypeRanking(typeRankingMap, attendanceException == null ? null : attendanceException.getType(), warning);
            accumulateExceptionTrend(exceptionTrendMap, attendanceException == null ? null : attendanceException.getType(), warning, day, startTime.toLocalDate(), recentDays);

            if (STATUS_PROCESSED.equals(warning.getStatus())) {
                processedCount++;
                point.setProcessedCount(Long.valueOf(point.getProcessedCount().longValue() + 1L));
                ReviewRecord reviewRecord = findLatestReview(warning.getExceptionId());
                if (reviewRecord != null && reviewRecord.getReviewTime() != null && !reviewRecord.getReviewTime().isBefore(warning.getSendTime())) {
                    long processMinutes = Duration.between(warning.getSendTime(), reviewRecord.getReviewTime()).toMinutes();
                    totalProcessMinutes = totalProcessMinutes.add(BigDecimal.valueOf(processMinutes));
                    processedWithReviewCount++;
                    if (processMinutes <= overdueThresholdMinutes) {
                        withinSlaCount++;
                    } else {
                        overSlaCount++;
                    }
                } else {
                    overSlaCount++;
                }
            } else {
                unprocessedCount++;
                point.setUnprocessedCount(Long.valueOf(point.getUnprocessedCount().longValue() + 1L));
                long overdueMinutes = Duration.between(warning.getSendTime(), now).toMinutes();
                if (overdueMinutes >= overdueThresholdMinutes) {
                    overdueCount++;
                    if (overdueMinutes < 48L * 60L) {
                        overdue24To48Count++;
                    } else if (overdueMinutes < 72L * 60L) {
                        overdue48To72Count++;
                    } else {
                        overdueOver72Count++;
                    }
                    overdueItems.add(buildOverdueItem(warning, attendanceException, targetUser, overdueMinutes));
                }
            }

            if ("HIGH".equals(warning.getLevel())) {
                highRiskCount++;
                point.setHighRiskCount(Long.valueOf(point.getHighRiskCount().longValue() + 1L));
            }
        }

        WarningDashboardVO vo = new WarningDashboardVO();
        vo.setRecentDays(Integer.valueOf(recentDays));
        vo.setTotalCount(Long.valueOf(totalCount));
        vo.setProcessedCount(Long.valueOf(processedCount));
        vo.setUnprocessedCount(Long.valueOf(unprocessedCount));
        vo.setHighRiskCount(Long.valueOf(highRiskCount));
        vo.setOverdueCount(Long.valueOf(overdueCount));
        vo.setOverdue24To48Count(Long.valueOf(overdue24To48Count));
        vo.setOverdue48To72Count(Long.valueOf(overdue48To72Count));
        vo.setOverdueOver72Count(Long.valueOf(overdueOver72Count));
        vo.setSlaTargetHours(Integer.valueOf(slaTargetHours));
        vo.setWithinSlaCount(Long.valueOf(withinSlaCount));
        vo.setOverSlaCount(Long.valueOf(overSlaCount));
        vo.setProcessedRate(totalCount == 0L
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(processedCount).multiply(new BigDecimal("100")).divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP));
        vo.setWithinSlaRate(processedCount == 0L
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(withinSlaCount).multiply(new BigDecimal("100")).divide(BigDecimal.valueOf(processedCount), 2, RoundingMode.HALF_UP));
        vo.setAverageProcessMinutes(processedWithReviewCount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalProcessMinutes.divide(BigDecimal.valueOf(processedWithReviewCount), 2, RoundingMode.HALF_UP));
        vo.setTrendPoints(new ArrayList<WarningTrendPointVO>(trendMap.values()));
        vo.setTopRiskUsers(sortRankingItems(userRankingMap, 4));
        vo.setTopExceptionTypes(sortRankingItems(typeRankingMap, 4));
        vo.setExceptionTrendItems(buildExceptionTrendItems(sortRankingItems(typeRankingMap, 4), exceptionTrendMap));
        vo.setOverdueItems(sortOverdueItems(overdueItems, 4));
        List<WarningUserPortraitVO> portraits = sortUserPortraits(userPortraitMap, 3);
        for (WarningUserPortraitVO portrait : userPortraitMap.values()) {
            String riskTier = portrait.getRiskTier();
            if ("CRITICAL".equals(riskTier)) {
                criticalRiskUserCount++;
            } else if ("HIGH".equals(riskTier)) {
                highRiskUserCount++;
            } else if ("MEDIUM".equals(riskTier)) {
                mediumRiskUserCount++;
            } else {
                lowRiskUserCount++;
            }
        }
        vo.setCriticalRiskUserCount(Long.valueOf(criticalRiskUserCount));
        vo.setHighRiskUserCount(Long.valueOf(highRiskUserCount));
        vo.setMediumRiskUserCount(Long.valueOf(mediumRiskUserCount));
        vo.setLowRiskUserCount(Long.valueOf(lowRiskUserCount));
        vo.setUserPortraits(portraits);
        return vo;
    }

    private void accumulateUserRanking(Map<Long, WarningRankingItemVO> rankingMap, WarningRecord warning, User targetUser) {
        if (warning == null || targetUser == null || targetUser.getId() == null) {
            return;
        }
        WarningRankingItemVO rankingItem = rankingMap.get(targetUser.getId());
        if (rankingItem == null) {
            rankingItem = new WarningRankingItemVO();
            rankingItem.setKey(String.valueOf(targetUser.getId()));
            rankingItem.setLabel(targetUser.getRealName() + "（" + targetUser.getUsername() + "）");
            rankingItem.setCount(Long.valueOf(0L));
            rankingItem.setHighRiskCount(Long.valueOf(0L));
            rankingMap.put(targetUser.getId(), rankingItem);
        }
        rankingItem.setCount(Long.valueOf(rankingItem.getCount().longValue() + 1L));
        if ("HIGH".equals(warning.getLevel())) {
            rankingItem.setHighRiskCount(Long.valueOf(rankingItem.getHighRiskCount().longValue() + 1L));
        }
    }

    private void accumulateTypeRanking(Map<String, WarningRankingItemVO> rankingMap, String exceptionType, WarningRecord warning) {
        String key = StringUtils.hasText(exceptionType) ? exceptionType.trim() : "UNKNOWN";
        WarningRankingItemVO rankingItem = rankingMap.get(key);
        if (rankingItem == null) {
            rankingItem = new WarningRankingItemVO();
            rankingItem.setKey(key);
            rankingItem.setLabel(exceptionTypeCatalogService.resolveName(key));
            rankingItem.setCount(Long.valueOf(0L));
            rankingItem.setHighRiskCount(Long.valueOf(0L));
            rankingMap.put(key, rankingItem);
        }
        rankingItem.setCount(Long.valueOf(rankingItem.getCount().longValue() + 1L));
        if (warning != null && "HIGH".equals(warning.getLevel())) {
            rankingItem.setHighRiskCount(Long.valueOf(rankingItem.getHighRiskCount().longValue() + 1L));
        }
    }

    private void accumulateExceptionTrend(Map<String, long[]> trendMap,
                                          String exceptionType,
                                          WarningRecord warning,
                                          LocalDate day,
                                          LocalDate startDate,
                                          int recentDays) {
        if (warning == null || day == null) {
            return;
        }
        String key = StringUtils.hasText(exceptionType) ? exceptionType.trim() : "UNKNOWN";
        long[] counters = trendMap.get(key);
        if (counters == null) {
            counters = new long[recentDays];
            trendMap.put(key, counters);
        }
        int dayIndex = (int) (day.toEpochDay() - startDate.toEpochDay());
        if (dayIndex < 0 || dayIndex >= counters.length) {
            return;
        }
        counters[dayIndex] = counters[dayIndex] + 1L;
    }

    private void accumulateUserPortrait(Map<Long, WarningUserPortraitVO> portraitMap,
                                        WarningRecord warning,
                                        AttendanceException attendanceException,
                                        User targetUser) {
        if (warning == null || targetUser == null || targetUser.getId() == null) {
            return;
        }

        WarningUserPortraitVO portrait = portraitMap.get(targetUser.getId());
        if (portrait == null) {
            portrait = new WarningUserPortraitVO();
            portrait.setUserId(targetUser.getId());
            portrait.setUsername(targetUser.getUsername());
            portrait.setRealName(targetUser.getRealName());
            portrait.setTotalWarnings(Long.valueOf(0L));
            portrait.setHighRiskWarnings(Long.valueOf(0L));
            portrait.setUnprocessedWarnings(Long.valueOf(0L));
            portrait.setOverdueWarnings(Long.valueOf(0L));
            portraitMap.put(targetUser.getId(), portrait);
        }

        portrait.setTotalWarnings(Long.valueOf(portrait.getTotalWarnings().longValue() + 1L));
        if ("HIGH".equals(warning.getLevel())) {
            portrait.setHighRiskWarnings(Long.valueOf(portrait.getHighRiskWarnings().longValue() + 1L));
        }
        if (STATUS_UNPROCESSED.equals(warning.getStatus())) {
            portrait.setUnprocessedWarnings(Long.valueOf(portrait.getUnprocessedWarnings().longValue() + 1L));
            if (isOverdue(warning)) {
                portrait.setOverdueWarnings(Long.valueOf(portrait.getOverdueWarnings().longValue() + 1L));
            }
        }

        if (portrait.getLatestWarningTime() == null || (warning.getSendTime() != null && warning.getSendTime().isAfter(portrait.getLatestWarningTime()))) {
            portrait.setLatestWarningTime(warning.getSendTime());
            portrait.setLatestWarningLevel(warning.getLevel());
            portrait.setLatestExceptionType(attendanceException == null ? null : attendanceException.getType());
            portrait.setLatestExceptionTypeName(exceptionTypeCatalogService.resolveName(attendanceException == null ? null : attendanceException.getType()));
        }
        portrait.setRiskTier(resolvePortraitRiskTier(portrait));
    }

    private String resolvePortraitRiskTier(WarningUserPortraitVO portrait) {
        if (portrait == null) {
            return "LOW";
        }
        long overdueWarnings = portrait.getOverdueWarnings() == null ? 0L : portrait.getOverdueWarnings().longValue();
        long highRiskWarnings = portrait.getHighRiskWarnings() == null ? 0L : portrait.getHighRiskWarnings().longValue();
        long unprocessedWarnings = portrait.getUnprocessedWarnings() == null ? 0L : portrait.getUnprocessedWarnings().longValue();
        long totalWarnings = portrait.getTotalWarnings() == null ? 0L : portrait.getTotalWarnings().longValue();

        if (overdueWarnings >= 2L || highRiskWarnings >= 3L) {
            return "CRITICAL";
        }
        if (overdueWarnings >= 1L || highRiskWarnings >= 2L) {
            return "HIGH";
        }
        if (unprocessedWarnings >= 2L || totalWarnings >= 3L) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private WarningOverdueItemVO buildOverdueItem(WarningRecord warning,
                                                  AttendanceException attendanceException,
                                                  User targetUser,
                                                  long overdueMinutes) {
        WarningOverdueItemVO vo = new WarningOverdueItemVO();
        vo.setWarningId(warning.getId());
        vo.setExceptionId(warning.getExceptionId());
        vo.setTitle(resolveOverdueTitle(attendanceException, warning));
        vo.setLevel(warning.getLevel());
        vo.setRealName(targetUser == null ? null : targetUser.getRealName());
        vo.setSendTime(warning.getSendTime());
        vo.setOverdueMinutes(Long.valueOf(overdueMinutes));
        return vo;
    }

    private String resolveOverdueTitle(AttendanceException attendanceException, WarningRecord warning) {
        if (attendanceException != null && StringUtils.hasText(attendanceException.getType())) {
            return attendanceException.getType();
        }
        return warning == null ? "WARNING" : warning.getType();
    }

    private List<WarningRankingItemVO> sortRankingItems(Map<?, WarningRankingItemVO> rankingMap, int limit) {
        List<WarningRankingItemVO> items = new ArrayList<WarningRankingItemVO>(rankingMap.values());
        Collections.sort(items, Comparator
                .comparing(WarningRankingItemVO::getHighRiskCount, Comparator.nullsFirst(Long::compareTo))
                .thenComparing(WarningRankingItemVO::getCount, Comparator.nullsFirst(Long::compareTo))
                .reversed());
        if (items.size() > limit) {
            return new ArrayList<WarningRankingItemVO>(items.subList(0, limit));
        }
        return items;
    }

    private List<WarningOverdueItemVO> sortOverdueItems(List<WarningOverdueItemVO> overdueItems, int limit) {
        Collections.sort(overdueItems, Comparator.comparing(WarningOverdueItemVO::getOverdueMinutes, Comparator.nullsFirst(Long::compareTo)).reversed());
        if (overdueItems.size() > limit) {
            return new ArrayList<WarningOverdueItemVO>(overdueItems.subList(0, limit));
        }
        return overdueItems;
    }

    private List<WarningExceptionTrendItemVO> buildExceptionTrendItems(List<WarningRankingItemVO> topTypes,
                                                                       Map<String, long[]> exceptionTrendMap) {
        List<WarningExceptionTrendItemVO> items = new ArrayList<WarningExceptionTrendItemVO>();
        for (WarningRankingItemVO rankingItem : topTypes) {
            WarningExceptionTrendItemVO item = new WarningExceptionTrendItemVO();
            item.setType(rankingItem.getKey());
            item.setName(exceptionTypeCatalogService.resolveName(rankingItem.getKey()));
            item.setTotalCount(rankingItem.getCount());
            item.setHighRiskCount(rankingItem.getHighRiskCount());
            long[] counters = exceptionTrendMap.get(rankingItem.getKey());
            if (counters == null) {
                counters = new long[0];
            }
            List<Long> dailyCounts = new ArrayList<Long>(counters.length);
            for (long counter : counters) {
                dailyCounts.add(Long.valueOf(counter));
            }
            item.setDailyCounts(dailyCounts);
            items.add(item);
        }
        return items;
    }

    private List<WarningUserPortraitVO> sortUserPortraits(Map<Long, WarningUserPortraitVO> portraitMap, int limit) {
        List<WarningUserPortraitVO> items = new ArrayList<WarningUserPortraitVO>(portraitMap.values());
        Collections.sort(items, Comparator
                .comparing(WarningUserPortraitVO::getOverdueWarnings, Comparator.nullsFirst(Long::compareTo))
                .thenComparing(WarningUserPortraitVO::getUnprocessedWarnings, Comparator.nullsFirst(Long::compareTo))
                .thenComparing(WarningUserPortraitVO::getHighRiskWarnings, Comparator.nullsFirst(Long::compareTo))
                .thenComparing(WarningUserPortraitVO::getTotalWarnings, Comparator.nullsFirst(Long::compareTo))
                .reversed());
        if (items.size() > limit) {
            return new ArrayList<WarningUserPortraitVO>(items.subList(0, limit));
        }
        return items;
    }

    @Override
    public WarningAdviceVO getAdvice(Long id) {
        WarningRecord warningRecord = requireExistingWarning(id);
        AttendanceException attendanceException = requireExistingException(warningRecord.getExceptionId());
        ExceptionAnalysis analysis = findLatestAnalysis(attendanceException.getId());
        AttendanceRecord attendanceRecord = attendanceException.getRecordId() == null ? null : attendanceRecordMapper.selectById(attendanceException.getRecordId());
        User targetUser = attendanceException.getUserId() == null ? null : userMapper.selectById(attendanceException.getUserId());
        ReviewRecord reviewRecord = findLatestReview(attendanceException.getId());
        User reviewUser = reviewRecord == null || reviewRecord.getReviewUserId() == null ? null : userMapper.selectById(reviewRecord.getReviewUserId());

        WarningAdviceVO vo = new WarningAdviceVO();
        vo.setId(warningRecord.getId());
        vo.setExceptionId(warningRecord.getExceptionId());
        vo.setType(warningRecord.getType());
        vo.setLevel(warningRecord.getLevel());
        vo.setStatus(warningRecord.getStatus());
        vo.setPriorityScore(warningRecord.getPriorityScore());
        vo.setAiSummary(sanitizeAdviceText(warningRecord.getAiSummary(), "历史系统摘要无法直接显示，请联系管理员查看原始记录。"));
        vo.setDisposeSuggestion(sanitizeAdviceText(warningRecord.getDisposeSuggestion(), "历史处置建议无法直接显示，请联系管理员查看原始记录。"));
        vo.setDecisionSource(warningRecord.getDecisionSource());
        vo.setSendTime(warningRecord.getSendTime());
        vo.setInteractionStatus(warningRecord.getInteractionStatus());
        vo.setEmployeeReplyDeadline(warningRecord.getEmployeeReplyDeadline());
        vo.setLastInteractTime(warningRecord.getLastInteractTime());
        if (targetUser != null) {
            vo.setUserId(targetUser.getId());
            vo.setUsername(targetUser.getUsername());
            vo.setRealName(targetUser.getRealName());
        }
        if (attendanceRecord != null) {
            vo.setRecordId(attendanceRecord.getId());
            vo.setCheckTime(attendanceRecord.getCheckTime());
            vo.setCheckType(attendanceRecord.getCheckType());
            vo.setLocation(attendanceRecord.getLocation());
            vo.setDeviceId(attendanceRecord.getDeviceId());
            vo.setDeviceInfo(attendanceRecord.getDeviceInfo());
            vo.setTerminalId(attendanceRecord.getTerminalId());
            vo.setIpAddr(attendanceRecord.getIpAddr());
            vo.setFaceScore(attendanceRecord.getFaceScore());
            vo.setRecordStatus(attendanceRecord.getStatus());
        }
        vo.setExceptionType(attendanceException.getType());
        vo.setExceptionTypeName(exceptionTypeCatalogService.resolveName(attendanceException.getType()));
        vo.setExceptionTypeDescription(exceptionTypeCatalogService.resolveDescription(attendanceException.getType()));
        vo.setExceptionSourceType(attendanceException.getSourceType());
        vo.setExceptionProcessStatus(attendanceException.getProcessStatus());
        vo.setExceptionDescription(sanitizeAdviceText(attendanceException.getDescription(), "历史异常说明无法直接显示，请联系管理员查看原始记录。"));
        vo.setExceptionCreateTime(attendanceException.getCreateTime());
        if (analysis != null) {
            vo.setModelConclusion(analysis.getModelConclusion());
            vo.setConfidenceScore(analysis.getConfidenceScore());
            vo.setDecisionReason(sanitizeAdviceText(analysis.getDecisionReason(), "历史判定依据无法直接显示，请联系管理员查看原始记录。"));
            vo.setSimilarCaseSummary(sanitizeAdviceText(analysis.getSimilarCaseSummary(), "历史相似案例摘要无法直接显示，请联系管理员查看原始记录。"));
        }
        if (reviewRecord != null) {
            vo.setReviewResult(reviewRecord.getResult());
            vo.setReviewComment(sanitizeAdviceText(reviewRecord.getComment(), "历史复核意见无法直接显示，请联系管理员查看原始记录。"));
            vo.setReviewAiSuggestion(sanitizeAdviceText(reviewRecord.getAiReviewSuggestion(), "历史复核建议无法直接显示，请联系管理员查看原始记录。"));
            vo.setReviewTime(reviewRecord.getReviewTime());
        }
        if (reviewUser != null) {
            vo.setReviewUserName(reviewUser.getRealName());
        }
        return vo;
    }

    @Override
    @Transactional
    public WarningVO reEvaluate(WarningReevaluateDTO dto) {
        WarningReevaluateDTO validatedDTO = warningValidationSupport.validateReevaluate(dto);
        AuthUser authUser = currentAuthUser();
        WarningRecord warningRecord = requireExistingWarning(validatedDTO.getWarningId());
        AttendanceException attendanceException = requireExistingException(warningRecord.getExceptionId());
        ExceptionAnalysis analysis = findLatestAnalysis(attendanceException.getId());
        applySnapshot(warningRecord, attendanceException, analysis, resolveWarningStatus(warningRecord, attendanceException));
        warningRecord.setSendTime(LocalDateTime.now());
        warningRecordMapper.updateById(warningRecord);
        operationLogService.save(authUser.getUserId(), "WARNING", authUser.getRealName() + "重新评估预警" + warningRecord.getId());
        return toVO(warningRecord);
    }

    @Override
    public List<WarningInteractionVO> listInteractions(Long warningId) {
        WarningRecord warningRecord = requireExistingWarning(warningId);
        List<WarningInteractionRecord> entities = warningInteractionRecordMapper.selectByWarningId(warningRecord.getId());
        List<WarningInteractionVO> records = new ArrayList<WarningInteractionVO>();
        for (WarningInteractionRecord entity : entities) {
            records.add(toInteractionVO(entity));
        }
        return records;
    }

    @Override
    @Transactional
    public void requestExplanation(Long warningId, WarningRequestExplanationDTO dto) {
        AuthUser authUser = currentAuthUser();
        if (!isAdmin(authUser)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage());
        }
        WarningRequestExplanationDTO validatedDTO = warningValidationSupport.validateRequestExplanation(dto, notificationProperties.getDefaultReplyTimeoutHours());
        WarningRecord warningRecord = requireExistingWarning(warningId);
        AttendanceException attendanceException = requireExistingException(warningRecord.getExceptionId());
        if (STATUS_PROCESSED.equals(warningRecord.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已处理预警不能再发起说明请求");
        }
        User targetUser = attendanceException.getUserId() == null ? null : userMapper.selectById(attendanceException.getUserId());
        if (targetUser == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "关联员工不存在");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = now.plusHours(validatedDTO.getDeadlineHours().longValue());
        warningRecord.setInteractionStatus(INTERACTION_WAIT_EMPLOYEE_REPLY);
        warningRecord.setEmployeeReplyDeadline(deadline);
        warningRecord.setAssignedAdminId(authUser.getUserId());
        warningRecord.setLastInteractTime(now);
        warningRecordMapper.updateById(warningRecord);

        appendInteraction(warningRecord, attendanceException.getId(), authUser.getUserId(), ROLE_ADMIN, MESSAGE_TYPE_REQUEST_EXPLANATION, validatedDTO.getContent());
        notificationService.push(buildNotification(
                targetUser.getId(),
                authUser.getUserId(),
                warningRecord.getId(),
                CATEGORY_REQUEST_EXPLANATION,
                buildWarningTitle(attendanceException, warningRecord) + "需要补充说明",
                validatedDTO.getContent(),
                warningRecord.getLevel(),
                ACTION_REPLY,
                deadline
        ));
        operationLogService.save(authUser.getUserId(), "WARNING", authUser.getRealName() + "向员工发起预警说明请求" + warningRecord.getId());
    }

    @Override
    @Transactional
    public void reply(Long warningId, WarningReplyDTO dto) {
        WarningReplyDTO validatedDTO = warningValidationSupport.validateReply(dto);
        AuthUser authUser = currentAuthUser();
        WarningRecord warningRecord = requireExistingWarning(warningId);
        AttendanceException attendanceException = requireExistingException(warningRecord.getExceptionId());
        if (!isAdmin(authUser) && (attendanceException.getUserId() == null || !attendanceException.getUserId().equals(authUser.getUserId()))) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权回复该预警说明");
        }
        if (STATUS_PROCESSED.equals(warningRecord.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预警已处理，不能继续回复");
        }
        if (!INTERACTION_WAIT_EMPLOYEE_REPLY.equals(warningRecord.getInteractionStatus()) && !isAdmin(authUser)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前预警没有待回复说明请求");
        }

        warningRecord.setInteractionStatus(INTERACTION_EMPLOYEE_REPLIED);
        warningRecord.setLastInteractTime(LocalDateTime.now(clock));
        warningRecordMapper.updateById(warningRecord);

        appendInteraction(warningRecord, attendanceException.getId(), authUser.getUserId(), authUser.getRoleCode(), MESSAGE_TYPE_EMPLOYEE_REPLY, validatedDTO.getContent());
        notifyAdminsForEmployeeReply(warningRecord, attendanceException, authUser, validatedDTO.getContent());
        operationLogService.save(authUser.getUserId(), "WARNING", authUser.getRealName() + "提交预警说明" + warningRecord.getId());
    }

    @Override
    @Transactional
    public void syncWarningByExceptionId(Long exceptionId) {
        AttendanceException attendanceException = requireExistingException(exceptionId);
        if (!shouldCreateWarning(attendanceException)) {
            return;
        }

        ExceptionAnalysis analysis = findLatestAnalysis(exceptionId);
        WarningRecord warningRecord = warningRecordMapper.selectByExceptionId(exceptionId);
        String nextStatus = resolveWarningStatus(warningRecord, attendanceException);
        LocalDateTime now = LocalDateTime.now(clock);
        if (warningRecord == null) {
            WarningRecord newWarningRecord = new WarningRecord();
            newWarningRecord.setExceptionId(exceptionId);
            applySnapshot(newWarningRecord, attendanceException, analysis, nextStatus);
            newWarningRecord.setSendTime(now);
            newWarningRecord.setInteractionStatus(INTERACTION_NONE);
            newWarningRecord.setLastInteractTime(now);
            try {
                warningRecordMapper.insert(newWarningRecord);
                initializeNewWarning(newWarningRecord, attendanceException);
                return;
            } catch (DuplicateKeyException exception) {
                warningRecord = warningRecordMapper.selectByExceptionId(exceptionId);
                if (warningRecord == null) {
                    return;
                }
            }
        }

        applySnapshot(warningRecord, attendanceException, analysis, nextStatus);
        normalizeInteractionState(warningRecord);
        warningRecordMapper.updateById(warningRecord);
    }

    @Override
    @Transactional
    public void markProcessedByExceptionId(Long exceptionId) {
        WarningRecord warningRecord = warningRecordMapper.selectByExceptionId(exceptionId);
        if (warningRecord == null || STATUS_PROCESSED.equals(warningRecord.getStatus())) {
            return;
        }
        warningRecord.setStatus(STATUS_PROCESSED);
        warningRecord.setLastInteractTime(LocalDateTime.now(clock));
        warningRecordMapper.updateById(warningRecord);
    }

    @Override
    @Transactional
    public void notifyReviewResult(Long exceptionId, String reviewResult, String reviewComment) {
        WarningRecord warningRecord = warningRecordMapper.selectByExceptionId(exceptionId);
        AttendanceException attendanceException = requireExistingException(exceptionId);
        User targetUser = attendanceException.getUserId() == null ? null : userMapper.selectById(attendanceException.getUserId());
        if (targetUser == null) {
            return;
        }
        AuthUser authUser = currentAuthUser();
        String resultLabel = "REJECTED".equals(reviewResult) ? "已排除异常" : "已确认异常";
        String message = buildReviewResultMessage(attendanceException, resultLabel, reviewComment);
        if (warningRecord != null) {
            warningRecord.setInteractionStatus(INTERACTION_RESULT_SENT);
            warningRecord.setLastInteractTime(LocalDateTime.now(clock));
            warningRecordMapper.updateById(warningRecord);
            appendInteraction(warningRecord, exceptionId, authUser.getUserId(), authUser.getRoleCode(), MESSAGE_TYPE_REVIEW_RESULT, message);
            notificationService.push(buildNotification(
                    targetUser.getId(),
                    authUser.getUserId(),
                    warningRecord.getId(),
                    CATEGORY_REVIEW_RESULT,
                    buildWarningTitle(attendanceException, warningRecord) + "处理结果",
                    message,
                    warningRecord.getLevel(),
                    ACTION_VIEW,
                    null
            ));
            return;
        }
        notificationService.push(buildDirectReviewResultNotification(targetUser.getId(), authUser.getUserId(), attendanceException, message));
    }

    @Override
    @Transactional
    public int runAbsenceCheck() {
        Rule rule = ruleService.getEnabledRule();
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return 0;
        }
        LocalTime cutoffTime = rule.getStartTime().plusMinutes(rule.getLateThreshold().longValue());
        if (now.toLocalTime().isBefore(cutoffTime)) {
            return 0;
        }

        Role employeeRole = roleMapper.selectOne(Wrappers.<Role>lambdaQuery()
                .eq(Role::getCode, ROLE_EMPLOYEE)
                .eq(Role::getStatus, 1)
                .last("LIMIT 1"));
        if (employeeRole == null) {
            return 0;
        }

        LocalDate today = now.toLocalDate();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1L).atStartOfDay();
        List<User> employees = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getRoleId, employeeRole.getId())
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId));
        int createdCount = 0;
        for (User employee : employees) {
            Long existingAbsenceCount = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                    .eq(AttendanceException::getUserId, employee.getId())
                    .eq(AttendanceException::getType, ABSENT)
                    .ge(AttendanceException::getCreateTime, dayStart)
                    .lt(AttendanceException::getCreateTime, nextDayStart));
            if (existingAbsenceCount != null && existingAbsenceCount.longValue() > 0L) {
                continue;
            }
            Long checkedInCount = attendanceRecordMapper.selectCount(Wrappers.<AttendanceRecord>lambdaQuery()
                    .eq(AttendanceRecord::getUserId, employee.getId())
                    .eq(AttendanceRecord::getCheckType, "IN")
                    .ge(AttendanceRecord::getCheckTime, dayStart)
                    .lt(AttendanceRecord::getCheckTime, nextDayStart));
            if (checkedInCount != null && checkedInCount.longValue() > 0L) {
                continue;
            }

            AttendanceException attendanceException = new AttendanceException();
            attendanceException.setRecordId(null);
            attendanceException.setUserId(employee.getId());
            attendanceException.setType(ABSENT);
            attendanceException.setRiskLevel("HIGH");
            attendanceException.setSourceType(SOURCE_RULE);
            attendanceException.setDescription(buildAbsenceDescription(cutoffTime));
            attendanceException.setProcessStatus("PENDING");
            attendanceExceptionMapper.insert(attendanceException);
            syncWarningByExceptionId(attendanceException.getId());
            createdCount++;
        }
        return createdCount;
    }

    @Override
    @Transactional
    public int runMissingCheckoutCheck() {
        Rule rule = ruleService.getEnabledRule();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate targetDate = now.toLocalDate().minusDays(1L);
        if (targetDate.getDayOfWeek() == DayOfWeek.SATURDAY || targetDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return 0;
        }

        Role employeeRole = roleMapper.selectOne(Wrappers.<Role>lambdaQuery()
                .eq(Role::getCode, ROLE_EMPLOYEE)
                .eq(Role::getStatus, 1)
                .last("LIMIT 1"));
        if (employeeRole == null) {
            return 0;
        }

        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime nextDayStart = targetDate.plusDays(1L).atStartOfDay();
        LocalDateTime settlementTime = nextDayStart.minusSeconds(1L);
        List<User> employees = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getRoleId, employeeRole.getId())
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId));
        int createdCount = 0;
        for (User employee : employees) {
            Long existingMissingCheckoutCount = attendanceExceptionMapper.selectCount(Wrappers.<AttendanceException>lambdaQuery()
                    .eq(AttendanceException::getUserId, employee.getId())
                    .eq(AttendanceException::getType, MISSING_CHECKOUT)
                    .ge(AttendanceException::getCreateTime, dayStart)
                    .lt(AttendanceException::getCreateTime, nextDayStart));
            if (existingMissingCheckoutCount != null && existingMissingCheckoutCount.longValue() > 0L) {
                continue;
            }
            Long checkedInCount = attendanceRecordMapper.selectCount(Wrappers.<AttendanceRecord>lambdaQuery()
                    .eq(AttendanceRecord::getUserId, employee.getId())
                    .eq(AttendanceRecord::getCheckType, "IN")
                    .ge(AttendanceRecord::getCheckTime, dayStart)
                    .lt(AttendanceRecord::getCheckTime, nextDayStart));
            if (checkedInCount == null || checkedInCount.longValue() <= 0L) {
                continue;
            }
            Long checkedOutCount = attendanceRecordMapper.selectCount(Wrappers.<AttendanceRecord>lambdaQuery()
                    .eq(AttendanceRecord::getUserId, employee.getId())
                    .eq(AttendanceRecord::getCheckType, "OUT")
                    .ge(AttendanceRecord::getCheckTime, dayStart)
                    .lt(AttendanceRecord::getCheckTime, nextDayStart));
            if (checkedOutCount != null && checkedOutCount.longValue() > 0L) {
                continue;
            }

            AttendanceException attendanceException = new AttendanceException();
            attendanceException.setRecordId(null);
            attendanceException.setUserId(employee.getId());
            attendanceException.setType(MISSING_CHECKOUT);
            attendanceException.setRiskLevel("MEDIUM");
            attendanceException.setSourceType(SOURCE_RULE);
            attendanceException.setDescription(buildMissingCheckoutDescription(rule.getEndTime(), targetDate));
            attendanceException.setProcessStatus("PENDING");
            attendanceException.setCreateTime(settlementTime);
            attendanceExceptionMapper.insert(attendanceException);
            syncWarningByExceptionId(attendanceException.getId());
            createdCount++;
        }
        return createdCount;
    }

    @Override
    @Transactional
    public int runReminderCheck() {
        int createdCount = 0;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cooldownSince = now.minusMinutes(notificationProperties.getReminderCooldownMinutes().longValue());
        List<WarningRecord> waitingEmployeeReplies = warningRecordMapper.selectList(Wrappers.<WarningRecord>lambdaQuery()
                .eq(WarningRecord::getInteractionStatus, INTERACTION_WAIT_EMPLOYEE_REPLY)
                .eq(WarningRecord::getStatus, STATUS_UNPROCESSED)
                .isNotNull(WarningRecord::getEmployeeReplyDeadline)
                .le(WarningRecord::getEmployeeReplyDeadline, now));
        for (WarningRecord warningRecord : waitingEmployeeReplies) {
            AttendanceException attendanceException = requireExistingException(warningRecord.getExceptionId());
            User employee = attendanceException.getUserId() == null ? null : userMapper.selectById(attendanceException.getUserId());
            if (employee != null && !notificationService.hasRecentNotification(employee.getId(), CATEGORY_EMPLOYEE_REPLY_REMINDER, warningRecord.getId(), cooldownSince)) {
                notificationService.push(buildNotification(
                        employee.getId(),
                        warningRecord.getAssignedAdminId(),
                        warningRecord.getId(),
                        CATEGORY_EMPLOYEE_REPLY_REMINDER,
                        buildWarningTitle(attendanceException, warningRecord) + "待你补充说明",
                        "管理员已发起说明请求，请尽快在消息中心补充情况说明。",
                        warningRecord.getLevel(),
                        ACTION_REPLY,
                        warningRecord.getEmployeeReplyDeadline()
                ));
                appendInteraction(warningRecord, attendanceException.getId(), null, ROLE_SYSTEM, MESSAGE_TYPE_REMINDER, "系统提醒：该预警说明请求已超时，请员工尽快补充说明。");
                createdCount++;
            }
            if (warningRecord.getAssignedAdminId() != null
                    && !notificationService.hasRecentNotification(warningRecord.getAssignedAdminId(), CATEGORY_EMPLOYEE_REPLY_REMINDER, warningRecord.getId(), cooldownSince)) {
                notificationService.push(buildNotification(
                        warningRecord.getAssignedAdminId(),
                        null,
                        warningRecord.getId(),
                        CATEGORY_EMPLOYEE_REPLY_REMINDER,
                        buildWarningTitle(attendanceException, warningRecord) + "员工说明已超时",
                        "员工尚未在规定时间内提交说明，请及时跟进处理。",
                        warningRecord.getLevel(),
                        ACTION_REVIEW,
                        null
                ));
                createdCount++;
            }
        }

        List<WarningRecord> overdueWarnings = warningRecordMapper.selectList(Wrappers.<WarningRecord>lambdaQuery()
                .eq(WarningRecord::getStatus, STATUS_UNPROCESSED)
                .le(WarningRecord::getSendTime, now.minusHours(24L)));
        for (WarningRecord warningRecord : overdueWarnings) {
            AttendanceException attendanceException = requireExistingException(warningRecord.getExceptionId());
            List<User> admins = resolveTargetAdmins(warningRecord);
            boolean reminded = false;
            for (User admin : admins) {
                if (notificationService.hasRecentNotification(admin.getId(), CATEGORY_WARNING_OVERDUE_REMINDER, warningRecord.getId(), cooldownSince)) {
                    continue;
                }
                notificationService.push(buildNotification(
                        admin.getId(),
                        null,
                        warningRecord.getId(),
                        CATEGORY_WARNING_OVERDUE_REMINDER,
                        buildWarningTitle(attendanceException, warningRecord) + "已超时",
                        "该预警已超过 24 小时未处理，请优先完成复核闭环。",
                        warningRecord.getLevel(),
                        ACTION_REVIEW,
                        null
                ));
                reminded = true;
                createdCount++;
            }
            if (reminded) {
                appendInteraction(warningRecord, attendanceException.getId(), null, ROLE_SYSTEM, MESSAGE_TYPE_REMINDER, "系统提醒：该预警已超过 24 小时未处理，请管理员优先跟进。");
            }
        }
        return createdCount;
    }

    @Override
    public PageResult<RiskLevelConfigVO> listRiskLevels(RiskLevelQueryDTO queryDTO) {
        return riskLevelRegistry.list(warningValidationSupport.validateRiskLevelQuery(queryDTO));
    }

    @Override
    public void updateRiskLevel(RiskLevelUpdateDTO dto) {
        riskLevelRegistry.update(warningValidationSupport.validateRiskLevelUpdate(dto));
    }

    private void ensureWarningsGenerated() {
        // BE-06 只消费异常结果；首次查询时懒生成缺失的预警记录。
        List<AttendanceException> candidates = attendanceExceptionMapper.selectList(Wrappers.<AttendanceException>lambdaQuery()
                .in(AttendanceException::getRiskLevel, "HIGH", "MEDIUM")
                .orderByDesc(AttendanceException::getCreateTime)
                .orderByDesc(AttendanceException::getId));
        for (AttendanceException attendanceException : candidates) {
            syncWarningByExceptionId(attendanceException.getId());
        }
    }

    private boolean shouldCreateWarning(AttendanceException attendanceException) {
        return "HIGH".equals(attendanceException.getRiskLevel()) || "MEDIUM".equals(attendanceException.getRiskLevel());
    }

    private String resolveWarningStatus(WarningRecord warningRecord, AttendanceException attendanceException) {
        if (EXCEPTION_STATUS_REVIEWED.equals(attendanceException.getProcessStatus())) {
            return STATUS_PROCESSED;
        }
        if (warningRecord == null || !StringUtils.hasText(warningRecord.getStatus())) {
            return STATUS_UNPROCESSED;
        }
        return warningRecord.getStatus();
    }

    private void applySnapshot(WarningRecord warningRecord,
                               AttendanceException attendanceException,
                               ExceptionAnalysis analysis,
                               String status) {
        warningRecord.setType(resolveWarningType(attendanceException));
        warningRecord.setLevel(attendanceException.getRiskLevel());
        warningRecord.setStatus(status);
        warningRecord.setPriorityScore(resolvePriorityScore(attendanceException, analysis));
        warningRecord.setAiSummary(resolveAiSummary(attendanceException, analysis));
        warningRecord.setDisposeSuggestion(resolveDisposeSuggestion(attendanceException, analysis));
        warningRecord.setDecisionSource(resolveDecisionSource(attendanceException, analysis));
    }

    private WarningRecord requireExistingWarning(Long id) {
        WarningRecord warningRecord = warningRecordMapper.selectById(id);
        if (warningRecord == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预警记录不存在");
        }
        normalizeInteractionState(warningRecord);
        return warningRecord;
    }

    private AttendanceException requireExistingException(Long exceptionId) {
        AttendanceException attendanceException = attendanceExceptionMapper.selectById(exceptionId);
        if (attendanceException == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "关联异常不存在");
        }
        return attendanceException;
    }

    private ExceptionAnalysis findLatestAnalysis(Long exceptionId) {
        return exceptionAnalysisMapper.selectOne(Wrappers.<ExceptionAnalysis>lambdaQuery()
                .eq(ExceptionAnalysis::getExceptionId, exceptionId)
                .orderByDesc(ExceptionAnalysis::getCreateTime)
                .orderByDesc(ExceptionAnalysis::getId)
                .last("LIMIT 1"));
    }

    private ReviewRecord findLatestReview(Long exceptionId) {
        return reviewRecordMapper.selectOne(Wrappers.<ReviewRecord>lambdaQuery()
                .eq(ReviewRecord::getExceptionId, exceptionId)
                .orderByDesc(ReviewRecord::getReviewTime)
                .orderByDesc(ReviewRecord::getId)
                .last("LIMIT 1"));
    }

    private String resolveWarningType(AttendanceException attendanceException) {
        if (SOURCE_MODEL.equals(attendanceException.getSourceType()) || SOURCE_MODEL_FALLBACK.equals(attendanceException.getSourceType())) {
            return TYPE_RISK_WARNING;
        }
        return TYPE_ATTENDANCE_WARNING;
    }

    private String resolveDecisionSource(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        if (analysis != null || !SOURCE_RULE.equals(attendanceException.getSourceType())) {
            return DECISION_SOURCE_MODEL_FUSION;
        }
        return DECISION_SOURCE_RULE;
    }

    private String resolveAiSummary(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        if (analysis != null && StringUtils.hasText(analysis.getReasonSummary())) {
            return analysis.getReasonSummary();
        }
        RiskLevelConfigVO riskLevelConfig = riskLevelRegistry.get(attendanceException.getRiskLevel());
        String riskLevelName = riskLevelConfig == null ? attendanceException.getRiskLevel() : riskLevelConfig.getName();
        return attendanceException.getDescription() + "（" + riskLevelName + "）";
    }

    private String resolveDisposeSuggestion(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        if (analysis != null && StringUtils.hasText(analysis.getActionSuggestion())) {
            return analysis.getActionSuggestion();
        }
        if (SOURCE_MODEL.equals(attendanceException.getSourceType()) || SOURCE_MODEL_FALLBACK.equals(attendanceException.getSourceType())) {
            return "建议管理员尽快查看异常详情并确认风险";
        }
        return "建议记录本次异常并结合历史记录继续观察";
    }

    private BigDecimal resolvePriorityScore(AttendanceException attendanceException, ExceptionAnalysis analysis) {
        int score = "HIGH".equals(attendanceException.getRiskLevel()) ? 90 : 70;
        if (SOURCE_MODEL.equals(attendanceException.getSourceType()) || SOURCE_MODEL_FALLBACK.equals(attendanceException.getSourceType())) {
            score += 5;
        }
        if (analysis != null && analysis.getConfidenceScore() != null
                && analysis.getConfidenceScore().compareTo(new BigDecimal("90")) >= 0) {
            score += 1;
        }
        return new BigDecimal(Math.min(score, 99)).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private WarningVO toVO(WarningRecord warningRecord) {
        WarningVO vo = new WarningVO();
        AttendanceException attendanceException = attendanceExceptionMapper.selectById(warningRecord.getExceptionId());
        long overdueMinutes = calculateOverdueMinutes(warningRecord);
        vo.setId(warningRecord.getId());
        vo.setExceptionId(warningRecord.getExceptionId());
        vo.setExceptionType(attendanceException == null ? null : attendanceException.getType());
        vo.setExceptionTypeName(exceptionTypeCatalogService.resolveName(attendanceException == null ? null : attendanceException.getType()));
        vo.setExceptionTypeDescription(exceptionTypeCatalogService.resolveDescription(attendanceException == null ? null : attendanceException.getType()));
        vo.setType(warningRecord.getType());
        vo.setLevel(warningRecord.getLevel());
        vo.setStatus(warningRecord.getStatus());
        vo.setPriorityScore(warningRecord.getPriorityScore());
        vo.setAiSummary(sanitizeAdviceText(warningRecord.getAiSummary(), "历史系统摘要无法直接显示，请联系管理员查看原始记录。"));
        vo.setDisposeSuggestion(sanitizeAdviceText(warningRecord.getDisposeSuggestion(), "历史处置建议无法直接显示，请联系管理员查看原始记录。"));
        vo.setDecisionSource(warningRecord.getDecisionSource());
        vo.setSendTime(warningRecord.getSendTime());
        vo.setOverdue(Boolean.valueOf(isOverdue(warningRecord)));
        vo.setOverdueMinutes(Long.valueOf(Math.max(overdueMinutes, 0L)));
        vo.setInteractionStatus(warningRecord.getInteractionStatus());
        vo.setEmployeeReplyDeadline(warningRecord.getEmployeeReplyDeadline());
        vo.setLastInteractTime(warningRecord.getLastInteractTime());
        return vo;
    }

    private boolean isOverdue(WarningRecord warningRecord) {
        if (warningRecord == null || !STATUS_UNPROCESSED.equals(warningRecord.getStatus()) || warningRecord.getSendTime() == null) {
            return false;
        }
        return !warningRecord.getSendTime().isAfter(LocalDateTime.now(clock).minusHours(24L));
    }

    private long calculateOverdueMinutes(WarningRecord warningRecord) {
        if (warningRecord == null || warningRecord.getSendTime() == null) {
            return 0L;
        }
        long minutes = Duration.between(warningRecord.getSendTime(), LocalDateTime.now(clock)).toMinutes();
        return Math.max(minutes, 0L);
    }

    private WarningInteractionVO toInteractionVO(WarningInteractionRecord entity) {
        WarningInteractionVO vo = new WarningInteractionVO();
        vo.setId(entity.getId());
        vo.setWarningId(entity.getWarningId());
        vo.setExceptionId(entity.getExceptionId());
        vo.setSenderUserId(entity.getSenderUserId());
        vo.setSenderRole(entity.getSenderRole());
        vo.setMessageType(entity.getMessageType());
        vo.setContent(sanitizeInteractionContent(entity.getContent(), entity.getMessageType()));
        vo.setCreateTime(entity.getCreateTime());
        if (ROLE_SYSTEM.equals(entity.getSenderRole()) || entity.getSenderUserId() == null) {
            vo.setSenderName("系统");
            return vo;
        }
        User sender = userMapper.selectById(entity.getSenderUserId());
        if (sender == null) {
            vo.setSenderName(entity.getSenderRole());
            return vo;
        }
        vo.setSenderName(StringUtils.hasText(sender.getRealName()) ? sender.getRealName() : sender.getUsername());
        return vo;
    }

    private void initializeNewWarning(WarningRecord warningRecord, AttendanceException attendanceException) {
        appendInteraction(warningRecord, attendanceException.getId(), null, ROLE_SYSTEM, MESSAGE_TYPE_SYSTEM_NOTICE, buildInitialInteractionMessage(attendanceException));
        notifyEmployeeForNewWarning(warningRecord, attendanceException);
        notifyAdminsForNewWarning(warningRecord, attendanceException);
    }

    private void notifyEmployeeForNewWarning(WarningRecord warningRecord, AttendanceException attendanceException) {
        if (attendanceException.getUserId() == null) {
            return;
        }
        User targetUser = userMapper.selectById(attendanceException.getUserId());
        if (targetUser == null) {
            return;
        }
        notificationService.push(buildNotification(
                targetUser.getId(),
                null,
                warningRecord.getId(),
                CATEGORY_EXCEPTION_NOTICE,
                buildWarningTitle(attendanceException, warningRecord),
                buildEmployeeWarningNotice(attendanceException),
                warningRecord.getLevel(),
                ACTION_VIEW,
                null
        ));
    }

    private void notifyAdminsForNewWarning(WarningRecord warningRecord, AttendanceException attendanceException) {
        User targetUser = attendanceException.getUserId() == null ? null : userMapper.selectById(attendanceException.getUserId());
        List<User> admins = resolveAllAdmins();
        for (User admin : admins) {
            notificationService.push(buildNotification(
                    admin.getId(),
                    null,
                    warningRecord.getId(),
                    CATEGORY_WARNING_CREATED,
                    buildWarningTitle(attendanceException, warningRecord),
                    buildAdminWarningNotice(attendanceException, targetUser),
                    warningRecord.getLevel(),
                    ACTION_REVIEW,
                    null
            ));
        }
    }

    private void notifyAdminsForEmployeeReply(WarningRecord warningRecord,
                                              AttendanceException attendanceException,
                                              AuthUser authUser,
                                              String replyContent) {
        List<User> admins = resolveTargetAdmins(warningRecord);
        for (User admin : admins) {
            notificationService.push(buildNotification(
                    admin.getId(),
                    authUser.getUserId(),
                    warningRecord.getId(),
                    CATEGORY_EMPLOYEE_REPLY,
                    buildWarningTitle(attendanceException, warningRecord) + "已收到员工说明",
                    sanitizeInteractionContent(limitText(replyContent, 240), MESSAGE_TYPE_EMPLOYEE_REPLY),
                    warningRecord.getLevel(),
                    ACTION_REVIEW,
                    null
            ));
        }
    }

    private NotificationCreateCommand buildNotification(Long recipientUserId,
                                                        Long senderUserId,
                                                        Long warningId,
                                                        String category,
                                                        String title,
                                                        String content,
                                                        String level,
                                                        String actionCode,
                                                        LocalDateTime deadline) {
        NotificationCreateCommand command = new NotificationCreateCommand();
        command.setRecipientUserId(recipientUserId);
        command.setSenderUserId(senderUserId);
        command.setBusinessType(BUSINESS_TYPE_WARNING);
        command.setBusinessId(warningId);
        command.setCategory(category);
        command.setTitle(title);
        command.setContent(content);
        command.setLevel(level == null ? "INFO" : level);
        command.setActionCode(actionCode);
        command.setDeadline(deadline);
        return command;
    }

    private NotificationCreateCommand buildDirectReviewResultNotification(Long recipientUserId,
                                                                          Long senderUserId,
                                                                          AttendanceException attendanceException,
                                                                          String content) {
        NotificationCreateCommand command = new NotificationCreateCommand();
        command.setRecipientUserId(recipientUserId);
        command.setSenderUserId(senderUserId);
        command.setBusinessType(ATTENDANCE_EXCEPTION_BUSINESS_TYPE);
        command.setBusinessId(attendanceException == null ? null : attendanceException.getId());
        command.setCategory(CATEGORY_REVIEW_RESULT);
        command.setTitle(resolveExceptionName(attendanceException == null ? null : attendanceException.getType()) + "人工复核结果");
        command.setContent(content);
        command.setLevel(attendanceException == null ? "INFO" : attendanceException.getRiskLevel());
        command.setActionCode(ACTION_VIEW);
        return command;
    }

    private void appendInteraction(WarningRecord warningRecord,
                                   Long exceptionId,
                                   Long senderUserId,
                                   String senderRole,
                                   String messageType,
                                   String content) {
        WarningInteractionRecord record = new WarningInteractionRecord();
        record.setWarningId(warningRecord.getId());
        record.setExceptionId(exceptionId);
        record.setSenderUserId(senderUserId);
        record.setSenderRole(senderRole);
        record.setMessageType(messageType);
        record.setContent(sanitizeInteractionContent(limitText(content, 2000), messageType));
        warningInteractionRecordMapper.insert(record);
    }

    private List<User> resolveTargetAdmins(WarningRecord warningRecord) {
        if (warningRecord != null && warningRecord.getAssignedAdminId() != null) {
            User admin = userMapper.selectById(warningRecord.getAssignedAdminId());
            if (admin != null && admin.getStatus() != null && admin.getStatus().intValue() == 1) {
                List<User> single = new ArrayList<User>();
                single.add(admin);
                return single;
            }
        }
        return resolveAllAdmins();
    }

    private List<User> resolveAllAdmins() {
        Role adminRole = roleMapper.selectOne(Wrappers.<Role>lambdaQuery()
                .eq(Role::getCode, ROLE_ADMIN)
                .eq(Role::getStatus, 1)
                .last("LIMIT 1"));
        if (adminRole == null) {
            return Collections.emptyList();
        }
        return userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getRoleId, adminRole.getId())
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId));
    }

    private void normalizeInteractionState(WarningRecord warningRecord) {
        if (warningRecord == null || StringUtils.hasText(warningRecord.getInteractionStatus())) {
            return;
        }
        warningRecord.setInteractionStatus(INTERACTION_NONE);
    }

    private String buildWarningTitle(AttendanceException attendanceException, WarningRecord warningRecord) {
        String levelName = resolveRiskLevelName(warningRecord == null ? null : warningRecord.getLevel());
        String exceptionName = resolveExceptionName(attendanceException == null ? null : attendanceException.getType());
        return levelName + exceptionName + "预警";
    }

    private String buildEmployeeWarningNotice(AttendanceException attendanceException) {
        return "系统检测到你存在" + resolveExceptionName(attendanceException == null ? null : attendanceException.getType()) + "，当前已进入人工核查流程，可在消息中心查看处理进度。";
    }

    private String buildAdminWarningNotice(AttendanceException attendanceException, User targetUser) {
        String actor = targetUser == null ? "关联员工" : (StringUtils.hasText(targetUser.getRealName()) ? targetUser.getRealName() : targetUser.getUsername());
        return actor + "出现" + resolveExceptionName(attendanceException == null ? null : attendanceException.getType()) + "，请及时查看证据链并完成处置。";
    }

    private String buildInitialInteractionMessage(AttendanceException attendanceException) {
        return "系统已生成" + resolveExceptionName(attendanceException == null ? null : attendanceException.getType()) + "预警，并等待后续处理。";
    }

    private String buildReviewResultMessage(AttendanceException attendanceException, String resultLabel, String reviewComment) {
        StringBuilder builder = new StringBuilder();
        builder.append("管理员已完成").append(resolveExceptionName(attendanceException == null ? null : attendanceException.getType())).append("复核，结果为").append(resultLabel);
        builder.append("；当前考勤记录已更新为").append(resolveReviewedRecordStatusLabel(resultLabel));
        if (StringUtils.hasText(reviewComment)) {
            builder.append("；复核意见：").append(reviewComment.trim());
        }
        return builder.toString();
    }

    private String resolveReviewedRecordStatusLabel(String resultLabel) {
        if ("已排除异常".equals(resultLabel)) {
            return "正常";
        }
        return "异常";
    }

    private String buildAbsenceDescription(LocalTime cutoffTime) {
        return "截至" + cutoffTime + "仍未完成上班打卡，系统按考勤规则判定为缺勤";
    }

    private String buildMissingCheckoutDescription(LocalTime cutoffTime, LocalDate targetDate) {
        String targetDateText = targetDate == null ? "前一日" : targetDate.toString();
        String cutoffTimeText = cutoffTime == null ? "--:--" : cutoffTime.toString();
        return "截至" + targetDateText + " 24:00 仍未完成下班打卡，系统按考勤规则判定为下班缺卡（规则下班时间" + cutoffTimeText + "）";
    }

    private String resolveRiskLevelName(String level) {
        RiskLevelConfigVO riskLevelConfig = riskLevelRegistry.get(level);
        if (riskLevelConfig != null && StringUtils.hasText(riskLevelConfig.getName())) {
            return riskLevelConfig.getName();
        }
        if ("HIGH".equals(level)) {
            return "高风险";
        }
        if ("MEDIUM".equals(level)) {
            return "中风险";
        }
        return "低风险";
    }

    private String resolveExceptionName(String type) {
        String resolved = exceptionTypeCatalogService.resolveName(type);
        return StringUtils.hasText(resolved) ? resolved : "异常";
    }

    private boolean isAdmin(AuthUser authUser) {
        return authUser != null && ROLE_ADMIN.equals(authUser.getRoleCode());
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String sanitizeInteractionContent(String content, String messageType) {
        return sanitizeQuestionPlaceholder(content, resolveInteractionFallback(messageType));
    }

    private String sanitizeAdviceText(String content, String fallback) {
        return sanitizeQuestionPlaceholder(content, fallback);
    }

    private String resolveInteractionFallback(String messageType) {
        if (MESSAGE_TYPE_REQUEST_EXPLANATION.equals(messageType)) {
            return "历史说明请求内容无法直接显示，请联系管理员重新发起说明请求。";
        }
        if (MESSAGE_TYPE_EMPLOYEE_REPLY.equals(messageType)) {
            return "历史员工说明内容无法直接显示，请联系员工重新补充说明。";
        }
        if (MESSAGE_TYPE_REVIEW_RESULT.equals(messageType)) {
            return "历史复核结果说明无法直接显示，请联系管理员查看原始记录。";
        }
        return "历史处理记录内容无法直接显示，请联系管理员查看原始记录。";
    }

    private String sanitizeQuestionPlaceholder(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        if (!looksLikeQuestionPlaceholder(normalized)) {
            return normalized;
        }
        return StringUtils.hasText(fallback) ? fallback : normalized;
    }

    private boolean looksLikeQuestionPlaceholder(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        int placeholderCount = 0;
        int meaningfulCount = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isWhitespace(current)
                    || current == ',' || current == '，'
                    || current == '.' || current == '。'
                    || current == ';' || current == '；'
                    || current == ':' || current == '：'
                    || current == '!' || current == '！'
                    || current == '(' || current == ')'
                    || current == '（' || current == '）') {
                continue;
            }
            meaningfulCount++;
            if (current == '?' || current == '？' || current == '\uFFFD') {
                placeholderCount++;
            }
        }
        return meaningfulCount >= 3 && placeholderCount == meaningfulCount;
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }
}
