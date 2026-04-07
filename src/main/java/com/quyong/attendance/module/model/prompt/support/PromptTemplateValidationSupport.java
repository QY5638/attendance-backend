package com.quyong.attendance.module.model.prompt.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateQueryDTO;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateSaveDTO;
import com.quyong.attendance.module.model.prompt.dto.PromptTemplateStatusDTO;
import com.quyong.attendance.module.model.prompt.entity.PromptTemplate;
import com.quyong.attendance.module.model.prompt.mapper.PromptTemplateMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptTemplateValidationSupport {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final PromptTemplateMapper promptTemplateMapper;

    public PromptTemplateValidationSupport(PromptTemplateMapper promptTemplateMapper) {
        this.promptTemplateMapper = promptTemplateMapper;
    }

    public PromptTemplateQueryDTO validateQuery(PromptTemplateQueryDTO dto) {
        PromptTemplateQueryDTO safe = dto == null ? new PromptTemplateQueryDTO() : dto;
        safe.setPageNum(safe.getPageNum() == null || safe.getPageNum().intValue() < 1 ? 1 : safe.getPageNum());
        safe.setPageSize(safe.getPageSize() == null || safe.getPageSize().intValue() < 1 ? 10 : safe.getPageSize());
        safe.setKeyword(normalize(safe.getKeyword()));
        safe.setSceneType(normalize(safe.getSceneType()));
        safe.setStatus(resolveStatus(normalize(safe.getStatus()), false));
        return safe;
    }

    public PromptTemplateSaveDTO validateSave(PromptTemplateSaveDTO dto, boolean requireId) {
        if (dto == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请求参数错误");
        }
        if (requireId && dto.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "提示词模板不存在");
        }

        dto.setCode(requireText(dto.getCode(), "模板编码不能为空"));
        dto.setName(requireText(dto.getName(), "模板名称不能为空"));
        dto.setSceneType(requireText(dto.getSceneType(), "场景类型不能为空"));
        dto.setVersion(requireText(dto.getVersion(), "模板版本不能为空"));
        dto.setContent(requireText(dto.getContent(), "模板内容不能为空"));
        dto.setStatus(resolveStatus(normalize(dto.getStatus()), true));
        dto.setRemark(normalize(dto.getRemark()));

        ensureUniqueCodeVersion(dto.getCode(), dto.getVersion(), requireId ? dto.getId() : null);
        return dto;
    }

    public PromptTemplateStatusDTO validateStatus(PromptTemplateStatusDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "提示词模板不存在");
        }
        dto.setStatus(resolveStatus(normalize(dto.getStatus()), true));
        return dto;
    }

    public PromptTemplate requireExistingTemplate(Long id) {
        PromptTemplate promptTemplate = id == null ? null : promptTemplateMapper.selectById(id);
        if (promptTemplate == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "提示词模板不存在");
        }
        return promptTemplate;
    }

    private void ensureUniqueCodeVersion(String code, String version, Long currentId) {
        PromptTemplate existing = promptTemplateMapper.selectOne(Wrappers.<PromptTemplate>lambdaQuery()
                .eq(PromptTemplate::getCode, code)
                .eq(PromptTemplate::getVersion, version)
                .ne(currentId != null, PromptTemplate::getId, currentId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "提示词模板版本已存在");
        }
    }

    private String requireText(String value, String message) {
        String normalizedValue = normalize(value);
        if (!StringUtils.hasText(normalizedValue)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
        }
        return normalizedValue;
    }

    private String resolveStatus(String status, boolean required) {
        if (!StringUtils.hasText(status)) {
            if (required) {
                return STATUS_ENABLED;
            }
            return null;
        }
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "模板状态不合法");
        }
        return status;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
