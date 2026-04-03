package com.quyong.attendance.module.model.gateway.service;

import com.quyong.attendance.module.model.gateway.dto.ModelInvokeRequest;
import com.quyong.attendance.module.model.gateway.dto.ModelInvokeResponse;

public interface ModelGateway {

    ModelInvokeResponse invoke(ModelInvokeRequest request);
}
