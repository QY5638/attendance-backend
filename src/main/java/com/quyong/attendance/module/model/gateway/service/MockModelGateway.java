package com.quyong.attendance.module.model.gateway.service;

import com.quyong.attendance.module.model.gateway.dto.ModelInvokeRequest;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@ConditionalOnMissingBean(ModelGateway.class)
public class MockModelGateway implements ModelGateway {

    @Override
    public ModelInvokeResponse invoke(ModelInvokeRequest request) {
        ModelInvokeResponse response = new ModelInvokeResponse();
        response.setConclusion("PROXY_CHECKIN");
        response.setRiskLevel("HIGH");
        response.setConfidenceScore(new BigDecimal("92.50"));
        response.setDecisionReason("设备异常、地点异常且行为模式偏离历史规律");
        response.setReasonSummary("设备与地点异常共同提升风险");
        response.setActionSuggestion("建议优先人工复核");
        response.setSimilarCaseSummary("存在相似设备异常与低分值组合案例");
        response.setRawResponse("{\"conclusion\":\"PROXY_CHECKIN\"}");
        return response;
    }
}
