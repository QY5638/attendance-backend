package com.quyong.attendance.module.model.log.service.impl;

import com.quyong.attendance.module.model.log.entity.ModelCallLog;
import com.quyong.attendance.module.model.log.mapper.ModelCallLogMapper;
import com.quyong.attendance.module.model.log.service.ModelCallLogService;
import org.springframework.stereotype.Service;

@Service
public class ModelCallLogServiceImpl implements ModelCallLogService {

    private final ModelCallLogMapper modelCallLogMapper;

    public ModelCallLogServiceImpl(ModelCallLogMapper modelCallLogMapper) {
        this.modelCallLogMapper = modelCallLogMapper;
    }

    @Override
    public void logSuccess(String businessType,
                           Long businessId,
                           Long promptTemplateId,
                           String inputSummary,
                           String outputSummary,
                           Integer latencyMs) {
        ModelCallLog entity = new ModelCallLog();
        entity.setBusinessType(businessType);
        entity.setBusinessId(businessId);
        entity.setPromptTemplateId(promptTemplateId);
        entity.setInputSummary(inputSummary);
        entity.setOutputSummary(outputSummary);
        entity.setStatus("SUCCESS");
        entity.setLatencyMs(latencyMs);
        modelCallLogMapper.insert(entity);
    }

    @Override
    public void logFailure(String businessType,
                           Long businessId,
                           Long promptTemplateId,
                           String inputSummary,
                           String errorMessage,
                           Integer latencyMs) {
        ModelCallLog entity = new ModelCallLog();
        entity.setBusinessType(businessType);
        entity.setBusinessId(businessId);
        entity.setPromptTemplateId(promptTemplateId);
        entity.setInputSummary(inputSummary);
        entity.setStatus("FAILED");
        entity.setLatencyMs(latencyMs);
        entity.setErrorMessage(errorMessage);
        modelCallLogMapper.insert(entity);
    }
}
