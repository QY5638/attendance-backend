package com.quyong.attendance.module.warning.vo;

import java.math.BigDecimal;
import java.util.List;

public class WarningDashboardVO {

    private Integer recentDays;
    private Long totalCount;
    private Long processedCount;
    private Long unprocessedCount;
    private Long highRiskCount;
    private Long overdueCount;
    private Long overdue24To48Count;
    private Long overdue48To72Count;
    private Long overdueOver72Count;
    private Long criticalRiskUserCount;
    private Long highRiskUserCount;
    private Long mediumRiskUserCount;
    private Long lowRiskUserCount;
    private Integer slaTargetHours;
    private Long withinSlaCount;
    private Long overSlaCount;
    private BigDecimal processedRate;
    private BigDecimal withinSlaRate;
    private BigDecimal averageProcessMinutes;
    private List<WarningTrendPointVO> trendPoints;
    private List<WarningRankingItemVO> topRiskUsers;
    private List<WarningRankingItemVO> topExceptionTypes;
    private List<WarningExceptionTrendItemVO> exceptionTrendItems;
    private List<WarningOverdueItemVO> overdueItems;
    private List<WarningUserPortraitVO> userPortraits;

    public Integer getRecentDays() {
        return recentDays;
    }

    public void setRecentDays(Integer recentDays) {
        this.recentDays = recentDays;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(Long processedCount) {
        this.processedCount = processedCount;
    }

    public Long getUnprocessedCount() {
        return unprocessedCount;
    }

    public void setUnprocessedCount(Long unprocessedCount) {
        this.unprocessedCount = unprocessedCount;
    }

    public Long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(Long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public Long getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(Long overdueCount) {
        this.overdueCount = overdueCount;
    }

    public Long getOverdue24To48Count() {
        return overdue24To48Count;
    }

    public void setOverdue24To48Count(Long overdue24To48Count) {
        this.overdue24To48Count = overdue24To48Count;
    }

    public Long getOverdue48To72Count() {
        return overdue48To72Count;
    }

    public void setOverdue48To72Count(Long overdue48To72Count) {
        this.overdue48To72Count = overdue48To72Count;
    }

    public Long getOverdueOver72Count() {
        return overdueOver72Count;
    }

    public void setOverdueOver72Count(Long overdueOver72Count) {
        this.overdueOver72Count = overdueOver72Count;
    }

    public Long getCriticalRiskUserCount() {
        return criticalRiskUserCount;
    }

    public void setCriticalRiskUserCount(Long criticalRiskUserCount) {
        this.criticalRiskUserCount = criticalRiskUserCount;
    }

    public Long getHighRiskUserCount() {
        return highRiskUserCount;
    }

    public void setHighRiskUserCount(Long highRiskUserCount) {
        this.highRiskUserCount = highRiskUserCount;
    }

    public Long getMediumRiskUserCount() {
        return mediumRiskUserCount;
    }

    public void setMediumRiskUserCount(Long mediumRiskUserCount) {
        this.mediumRiskUserCount = mediumRiskUserCount;
    }

    public Long getLowRiskUserCount() {
        return lowRiskUserCount;
    }

    public void setLowRiskUserCount(Long lowRiskUserCount) {
        this.lowRiskUserCount = lowRiskUserCount;
    }

    public Integer getSlaTargetHours() {
        return slaTargetHours;
    }

    public void setSlaTargetHours(Integer slaTargetHours) {
        this.slaTargetHours = slaTargetHours;
    }

    public Long getWithinSlaCount() {
        return withinSlaCount;
    }

    public void setWithinSlaCount(Long withinSlaCount) {
        this.withinSlaCount = withinSlaCount;
    }

    public Long getOverSlaCount() {
        return overSlaCount;
    }

    public void setOverSlaCount(Long overSlaCount) {
        this.overSlaCount = overSlaCount;
    }

    public BigDecimal getProcessedRate() {
        return processedRate;
    }

    public void setProcessedRate(BigDecimal processedRate) {
        this.processedRate = processedRate;
    }

    public BigDecimal getWithinSlaRate() {
        return withinSlaRate;
    }

    public void setWithinSlaRate(BigDecimal withinSlaRate) {
        this.withinSlaRate = withinSlaRate;
    }

    public BigDecimal getAverageProcessMinutes() {
        return averageProcessMinutes;
    }

    public void setAverageProcessMinutes(BigDecimal averageProcessMinutes) {
        this.averageProcessMinutes = averageProcessMinutes;
    }

    public List<WarningTrendPointVO> getTrendPoints() {
        return trendPoints;
    }

    public void setTrendPoints(List<WarningTrendPointVO> trendPoints) {
        this.trendPoints = trendPoints;
    }

    public List<WarningRankingItemVO> getTopRiskUsers() {
        return topRiskUsers;
    }

    public void setTopRiskUsers(List<WarningRankingItemVO> topRiskUsers) {
        this.topRiskUsers = topRiskUsers;
    }

    public List<WarningRankingItemVO> getTopExceptionTypes() {
        return topExceptionTypes;
    }

    public void setTopExceptionTypes(List<WarningRankingItemVO> topExceptionTypes) {
        this.topExceptionTypes = topExceptionTypes;
    }

    public List<WarningExceptionTrendItemVO> getExceptionTrendItems() {
        return exceptionTrendItems;
    }

    public void setExceptionTrendItems(List<WarningExceptionTrendItemVO> exceptionTrendItems) {
        this.exceptionTrendItems = exceptionTrendItems;
    }

    public List<WarningOverdueItemVO> getOverdueItems() {
        return overdueItems;
    }

    public void setOverdueItems(List<WarningOverdueItemVO> overdueItems) {
        this.overdueItems = overdueItems;
    }

    public List<WarningUserPortraitVO> getUserPortraits() {
        return userPortraits;
    }

    public void setUserPortraits(List<WarningUserPortraitVO> userPortraits) {
        this.userPortraits = userPortraits;
    }
}
