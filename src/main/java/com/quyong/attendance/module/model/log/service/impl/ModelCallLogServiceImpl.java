package com.quyong.attendance.module.model.log.service.impl;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.model.log.dto.ModelCallLogQueryDTO;
import com.quyong.attendance.module.model.log.entity.ModelCallLog;
import com.quyong.attendance.module.model.log.mapper.ModelCallLogMapper;
import com.quyong.attendance.module.model.log.service.ModelCallLogService;
import com.quyong.attendance.module.model.log.support.ModelCallLogValidationSupport;
import com.quyong.attendance.module.model.log.vo.ModelCallLogVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModelCallLogServiceImpl implements ModelCallLogService {

    private final ModelCallLogMapper modelCallLogMapper;
    private final ModelCallLogValidationSupport modelCallLogValidationSupport;

    public ModelCallLogServiceImpl(ModelCallLogMapper modelCallLogMapper,
                                   ModelCallLogValidationSupport modelCallLogValidationSupport) {
        this.modelCallLogMapper = modelCallLogMapper;
        this.modelCallLogValidationSupport = modelCallLogValidationSupport;
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

    @Override
    public PageResult<ModelCallLogVO> list(ModelCallLogQueryDTO dto) {
        ModelCallLogQueryDTO safe = modelCallLogValidationSupport.validateQuery(dto);
        LocalDateTime startTime = modelCallLogValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = modelCallLogValidationSupport.parseQueryEnd(safe.getEndDate());
        int offset = (safe.getPageNum().intValue() - 1) * safe.getPageSize().intValue();
        long total = modelCallLogMapper.countByQuery(
                safe.getBusinessType(),
                safe.getBusinessId(),
                safe.getPromptTemplateId(),
                safe.getStatus(),
                startTime,
                endTime
        );
        List<ModelCallLogVO> records = modelCallLogMapper.selectPageByQuery(
                safe.getBusinessType(),
                safe.getBusinessId(),
                safe.getPromptTemplateId(),
                safe.getStatus(),
                startTime,
                endTime,
                safe.getPageSize().intValue(),
                offset
        );
        return new PageResult<ModelCallLogVO>(Long.valueOf(total), records);
    }
}
