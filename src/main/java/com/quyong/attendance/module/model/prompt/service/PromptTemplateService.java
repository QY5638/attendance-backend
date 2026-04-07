package com.quyong.attendance.module.model.prompt.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateQueryDTO;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateSaveDTO;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateStatusDTO;
import com.quyong.attendance.module.model.prompt.vo.PromptTemplateVO;

public interface PromptTemplateService {

    PageResult<PromptTemplateVO> list(PromptTemplateQueryDTO dto);

    void add(PromptTemplateSaveDTO dto);

    void update(PromptTemplateSaveDTO dto);

    void updateStatus(PromptTemplateStatusDTO dto);
}
