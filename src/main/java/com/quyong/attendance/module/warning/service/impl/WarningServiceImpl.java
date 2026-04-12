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
import com.quyong.attendance.module.exceptiondetect.mapper.AttendanceExceptionMapper;
import com.quyong.attendance.module.exceptiondetect.mapper.ExceptionAnalysisMapper;
import com.quyong.attendance.module.review.entity.ReviewRecord;
import com.quyong.attendance.module.review.mapper.ReviewRecordMapper;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import com.quyong.attendance.module.warning.dto.RiskLevelQueryDTO;
import com.quyong.attendance.module.warning.dto.RiskLevelUpdateDTO;
import com.quyong.attendance.module.warning.dto.WarningQueryDTO;
import com.quyong.attendance.module.warning.dto.WarningReevaluateDTO;
import com.quyong.attendance.module.warning.entity.WarningRecord;
import com.quyong.attendance.module.warning.mapper.WarningRecordMapper;
import com.quyong.attendance.module.warning.service.WarningService;
import com.quyong.attendance.module.warning.support.RiskLevelRegistry;
import com.quyong.attendance.module.warning.support.WarningValidationSupport;
import com.quyong.attendance.module.warning.vo.RiskLevelConfigVO;
import com.quyong.attendance.module.warning.vo.WarningAdviceVO;
import com.quyong.attendance.module.warning.vo.WarningDashboardVO;
import com.quyong.attendance.module.warning.vo.WarningExceptionTrendItemVO;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final WarningRecordMapper warningRecordMapper;
    private final AttendanceExceptionMapper attendanceExceptionMapper;
    private final ExceptionAnalysisMapper exceptionAnalysisMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final UserMapper userMapper;
    private final WarningValidationSupport warningValidationSupport;
    private final RiskLevelRegistry riskLevelRegistry;
    private final OperationLogService operationLogService;

    public WarningServiceImpl(WarningRecordMapper warningRecordMapper,
                              AttendanceExceptionMapper attendanceExceptionMapper,
                              ExceptionAnalysisMapper exceptionAnalysisMapper,
                              AttendanceRecordMapper attendanceRecordMapper,
                              ReviewRecordMapper reviewRecordMapper,
                              UserMapper userMapper,
                              WarningValidationSupport warningValidationSupport,
                              RiskLevelRegistry riskLevelRegistry,
                              OperationLogService operationLogService) {
        this.warningRecordMapper = warningRecordMapper;
        this.attendanceExceptionMapper = attendanceExceptionMapper;
        this.exceptionAnalysisMapper = exceptionAnalysisMapper;
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.userMapper = userMapper;
        this.warningValidationSupport = warningValidationSupport;
        this.riskLevelRegistry = riskLevelRegistry;
        this.operationLogService = operationLogService;
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
            rankingItem.setLabel(key);
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
        vo.setAiSummary(warningRecord.getAiSummary());
        vo.setDisposeSuggestion(warningRecord.getDisposeSuggestion());
        vo.setDecisionSource(warningRecord.getDecisionSource());
        vo.setSendTime(warningRecord.getSendTime());
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
        vo.setExceptionSourceType(attendanceException.getSourceType());
        vo.setExceptionProcessStatus(attendanceException.getProcessStatus());
        vo.setExceptionDescription(attendanceException.getDescription());
        vo.setExceptionCreateTime(attendanceException.getCreateTime());
        if (analysis != null) {
            vo.setModelConclusion(analysis.getModelConclusion());
            vo.setConfidenceScore(analysis.getConfidenceScore());
            vo.setDecisionReason(analysis.getDecisionReason());
            vo.setSimilarCaseSummary(analysis.getSimilarCaseSummary());
        }
        if (reviewRecord != null) {
            vo.setReviewResult(reviewRecord.getResult());
            vo.setReviewComment(reviewRecord.getComment());
            vo.setReviewAiSuggestion(reviewRecord.getAiReviewSuggestion());
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
    @Transactional
    public void syncWarningByExceptionId(Long exceptionId) {
        AttendanceException attendanceException = requireExistingException(exceptionId);
        if (!shouldCreateWarning(attendanceException)) {
            return;
        }

        ExceptionAnalysis analysis = findLatestAnalysis(exceptionId);
        WarningRecord warningRecord = warningRecordMapper.selectByExceptionId(exceptionId);
        String nextStatus = resolveWarningStatus(warningRecord, attendanceException);
        if (warningRecord == null) {
            WarningRecord newWarningRecord = new WarningRecord();
            newWarningRecord.setExceptionId(exceptionId);
            applySnapshot(newWarningRecord, attendanceException, analysis, nextStatus);
            newWarningRecord.setSendTime(LocalDateTime.now());
            try {
                warningRecordMapper.insert(newWarningRecord);
                return;
            } catch (DuplicateKeyException exception) {
                warningRecord = warningRecordMapper.selectByExceptionId(exceptionId);
                if (warningRecord == null) {
                    return;
                }
            }
        }

        applySnapshot(warningRecord, attendanceException, analysis, nextStatus);
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
        warningRecordMapper.updateById(warningRecord);
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
        vo.setType(warningRecord.getType());
        vo.setLevel(warningRecord.getLevel());
        vo.setStatus(warningRecord.getStatus());
        vo.setPriorityScore(warningRecord.getPriorityScore());
        vo.setAiSummary(warningRecord.getAiSummary());
        vo.setDisposeSuggestion(warningRecord.getDisposeSuggestion());
        vo.setDecisionSource(warningRecord.getDecisionSource());
        vo.setSendTime(warningRecord.getSendTime());
        vo.setOverdue(Boolean.valueOf(isOverdue(warningRecord)));
        vo.setOverdueMinutes(Long.valueOf(Math.max(overdueMinutes, 0L)));
        return vo;
    }

    private boolean isOverdue(WarningRecord warningRecord) {
        if (warningRecord == null || !STATUS_UNPROCESSED.equals(warningRecord.getStatus()) || warningRecord.getSendTime() == null) {
            return false;
        }
        return !warningRecord.getSendTime().isAfter(LocalDateTime.now().minusHours(24L));
    }

    private long calculateOverdueMinutes(WarningRecord warningRecord) {
        if (warningRecord == null || warningRecord.getSendTime() == null) {
            return 0L;
        }
        long minutes = Duration.between(warningRecord.getSendTime(), LocalDateTime.now()).toMinutes();
        return Math.max(minutes, 0L);
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }
}
