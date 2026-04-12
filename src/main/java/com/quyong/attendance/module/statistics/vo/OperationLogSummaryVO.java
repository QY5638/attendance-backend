package com.quyong.attendance.module.statistics.vo;

import java.util.Map;

public class OperationLogSummaryVO {

    private Long total;
    private Map<String, Long> typeCounts;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Map<String, Long> getTypeCounts() {
        return typeCounts;
    }

    public void setTypeCounts(Map<String, Long> typeCounts) {
        this.typeCounts = typeCounts;
    }
}
