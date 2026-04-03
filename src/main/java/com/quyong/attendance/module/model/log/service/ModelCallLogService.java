package com.quyong.attendance.module.model.log.service;

public interface ModelCallLogService {

    void logSuccess(String businessType,
                    Long businessId,
                    Long promptTemplateId,
                    String inputSummary,
                    String outputSummary,
                    Integer latencyMs);

    void logFailure(String businessType,
                    Long businessId,
                    Long promptTemplateId,
                    String inputSummary,
                    String errorMessage,
                    Integer latencyMs);
}
