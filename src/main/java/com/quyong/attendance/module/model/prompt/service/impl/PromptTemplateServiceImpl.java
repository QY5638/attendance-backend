package com.quyong.attendance.module.model.prompt.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateQueryDTO;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateSaveDTO;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateStatusDTO;
import com.quyong.attendance.module.model.prompt.entity.PromptTemplate;
import com.quyong.attendance.module.model.prompt.mapper.PromptTemplateMapper;
import com.quyong.attendance.module.model.prompt.service.PromptTemplateService;
import com.quyong.attendance.module.model.prompt.support.PromptTemplateValidationSupport;
import com.quyong.attendance.module.model.prompt.vo.PromptTemplateVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final PromptTemplateMapper promptTemplateMapper;
    private final PromptTemplateValidationSupport promptTemplateValidationSupport;

    public PromptTemplateServiceImpl(PromptTemplateMapper promptTemplateMapper,
                                     PromptTemplateValidationSupport promptTemplateValidationSupport) {
        this.promptTemplateMapper = promptTemplateMapper;
        this.promptTemplateValidationSupport = promptTemplateValidationSupport;
    }

    @Override
    public PageResult<PromptTemplateVO> list(PromptTemplateQueryDTO dto) {
        PromptTemplateQueryDTO validatedDTO = promptTemplateValidationSupport.validateQuery(dto);
        List<PromptTemplate> entities = promptTemplateMapper.selectList(Wrappers.<PromptTemplate>lambdaQuery()
                .and(StringUtils.hasText(validatedDTO.getKeyword()), wrapper -> wrapper.like(PromptTemplate::getCode, validatedDTO.getKeyword())
                        .or()
                        .like(PromptTemplate::getName, validatedDTO.getKeyword())
                        .or()
                        .like(PromptTemplate::getVersion, validatedDTO.getKeyword()))
                .eq(StringUtils.hasText(validatedDTO.getSceneType()), PromptTemplate::getSceneType, validatedDTO.getSceneType())
                .eq(StringUtils.hasText(validatedDTO.getStatus()), PromptTemplate::getStatus, validatedDTO.getStatus())
                .orderByDesc(PromptTemplate::getUpdateTime)
                .orderByDesc(PromptTemplate::getId));
        long total = entities.size();
        int fromIndex = (validatedDTO.getPageNum().intValue() - 1) * validatedDTO.getPageSize().intValue();
        if (fromIndex >= entities.size()) {
            return new PageResult<PromptTemplateVO>(Long.valueOf(total), Collections.<PromptTemplateVO>emptyList());
        }
        int toIndex = Math.min(fromIndex + validatedDTO.getPageSize().intValue(), entities.size());
        List<PromptTemplateVO> records = new ArrayList<PromptTemplateVO>();
        for (PromptTemplate entity : entities.subList(fromIndex, toIndex)) {
            records.add(toVO(entity));
        }
        return new PageResult<PromptTemplateVO>(Long.valueOf(total), records);
    }

    @Override
    public void add(PromptTemplateSaveDTO dto) {
        PromptTemplateSaveDTO validatedDTO = promptTemplateValidationSupport.validateSave(dto, false);
        PromptTemplate promptTemplate = new PromptTemplate();
        fillPromptTemplate(promptTemplate, validatedDTO);
        LocalDateTime now = LocalDateTime.now();
        promptTemplate.setCreateTime(now);
        promptTemplate.setUpdateTime(now);
        promptTemplateMapper.insert(promptTemplate);
    }

    @Override
    public void update(PromptTemplateSaveDTO dto) {
        PromptTemplateSaveDTO validatedDTO = promptTemplateValidationSupport.validateSave(dto, true);
        PromptTemplate promptTemplate = promptTemplateValidationSupport.requireExistingTemplate(validatedDTO.getId());
        fillPromptTemplate(promptTemplate, validatedDTO);
        promptTemplate.setUpdateTime(LocalDateTime.now());
        promptTemplateMapper.updateById(promptTemplate);
    }

    @Override
    public void updateStatus(PromptTemplateStatusDTO dto) {
        PromptTemplateStatusDTO validatedDTO = promptTemplateValidationSupport.validateStatus(dto);
        PromptTemplate promptTemplate = promptTemplateValidationSupport.requireExistingTemplate(validatedDTO.getId());
        promptTemplate.setStatus(validatedDTO.getStatus());
        promptTemplate.setUpdateTime(LocalDateTime.now());
        promptTemplateMapper.updateById(promptTemplate);
    }

    private void fillPromptTemplate(PromptTemplate promptTemplate, PromptTemplateSaveDTO dto) {
        promptTemplate.setCode(dto.getCode());
        promptTemplate.setName(dto.getName());
        promptTemplate.setSceneType(dto.getSceneType());
        promptTemplate.setVersion(dto.getVersion());
        promptTemplate.setContent(dto.getContent());
        promptTemplate.setStatus(dto.getStatus());
        promptTemplate.setRemark(dto.getRemark());
    }

    private PromptTemplateVO toVO(PromptTemplate entity) {
        PromptTemplateVO vo = new PromptTemplateVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setSceneType(entity.getSceneType());
        vo.setVersion(entity.getVersion());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
