package com.quyong.attendance.module.statistics.support;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class StatisticsSummarySupport {

    public String buildSummary(Long recordCount,
                               Long exceptionCount,
                               Long analysisCount,
                               Long warningCount,
                               Long reviewCount,
                               Long closedLoopCount) {
        return "统计周期内共打卡" + safeCount(recordCount)
                + "条，识别异常" + safeCount(exceptionCount)
                + "条，完成智能分析" + safeCount(analysisCount)
                + "条，生成预警" + safeCount(warningCount)
                + "条，人工复核" + safeCount(reviewCount)
                + "条，形成闭环" + safeCount(closedLoopCount) + "条。";
    }

    public String buildHighlightRisks(Long highRiskCount,
                                      Long analysisGapCount,
                                      Long unprocessedWarningCount,
                                      Long closedLoopGapCount,
                                      Long missingDecisionTraceCount,
                                      Long missingModelLogCount) {
        return "高风险异常" + safeCount(highRiskCount)
                + "条，未完成分析异常" + safeCount(analysisGapCount)
                + "条，未闭环异常" + safeCount(closedLoopGapCount)
                + "条，未处理预警" + safeCount(unprocessedWarningCount)
                + "条，审计缺口包含决策追踪缺失" + safeCount(missingDecisionTraceCount)
                + "条、模型日志缺失" + safeCount(missingModelLogCount) + "条。";
    }

    public String buildManageSuggestion(Long highRiskCount,
                                        Long unprocessedWarningCount,
                                        Long closedLoopGapCount) {
        if (safeCount(highRiskCount) > 0L || safeCount(unprocessedWarningCount) > 0L) {
            return "建议优先处理高风险预警并补齐人工复核，持续跟踪未闭环异常。";
        }
        if (safeCount(closedLoopGapCount) > 0L) {
            return "建议继续推进预警到复核的闭环处理，避免异常长期挂起。";
        }
        return "当前预警与复核闭环较稳定，建议持续监控重点人员与重点部门。";
    }

    public BigDecimal calculateDepartmentRiskScore(Long recordCount,
                                                   Long exceptionCount,
                                                   Long highRiskCount,
                                                   Long warningCount,
                                                   Long unprocessedWarningCount,
                                                   Long closedLoopCount) {
        if (safeCount(recordCount) == 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal exceptionRate = ratio(exceptionCount, recordCount).multiply(new BigDecimal("40"));
        BigDecimal highRiskRate = ratio(highRiskCount, exceptionCount).multiply(new BigDecimal("30"));
        BigDecimal unresolvedWarningRate = ratio(unprocessedWarningCount, warningCount).multiply(new BigDecimal("20"));
        BigDecimal closedLoopGapRate = ratio(subtract(warningCount, closedLoopCount), warningCount).multiply(new BigDecimal("10"));
        return exceptionRate.add(highRiskRate).add(unresolvedWarningRate).add(closedLoopGapRate)
                .min(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public String buildDepartmentRiskSummary(String deptName,
                                             Long highRiskCount,
                                             Long unprocessedWarningCount,
                                             Long closedLoopGapCount) {
        return deptName + "存在高风险异常" + safeCount(highRiskCount)
                + "条，未处理预警" + safeCount(unprocessedWarningCount)
                + "条，未闭环异常" + safeCount(closedLoopGapCount) + "条。";
    }

    public String buildDepartmentManageSuggestion(Long highRiskCount,
                                                  Long unprocessedWarningCount,
                                                  Long closedLoopGapCount) {
        if (safeCount(highRiskCount) > 0L) {
            return "建议部门优先复核高风险异常，并对未处理预警安排专人跟进。";
        }
        if (safeCount(unprocessedWarningCount) > 0L || safeCount(closedLoopGapCount) > 0L) {
            return "建议部门加快预警处理和人工复核，避免闭环滞后。";
        }
        return "建议部门维持当前管理节奏，持续关注异常趋势变化。";
    }

    private BigDecimal ratio(Long numerator, Long denominator) {
        if (safeCount(denominator) == 0L) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(safeCount(numerator)).divide(new BigDecimal(safeCount(denominator)), 4, RoundingMode.HALF_UP);
    }

    private Long subtract(Long left, Long right) {
        return Long.valueOf(Math.max(safeCount(left) - safeCount(right), 0L));
    }

    private Long safeCount(Long value) {
        return value == null ? 0L : value;
    }
}
