package com.quyong.attendance.module.review.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.review.dto.ExceptionTypeQueryDTO;
import com.quyong.attendance.module.review.dto.ExceptionTypeUpdateDTO;
import com.quyong.attendance.module.review.entity.ExceptionType;
import com.quyong.attendance.module.review.mapper.ExceptionTypeMapper;
import com.quyong.attendance.module.review.service.ExceptionTypeService;
import com.quyong.attendance.module.review.support.ReviewValidationSupport;
import com.quyong.attendance.module.review.vo.ExceptionTypeVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExceptionTypeServiceImpl implements ExceptionTypeService {

    private static final Map<String, String> DEFAULT_EXCEPTION_NAMES = buildDefaultExceptionNames();
    private static final Map<String, String> DEFAULT_EXCEPTION_DESCRIPTIONS = buildDefaultExceptionDescriptions();

    private final ExceptionTypeMapper exceptionTypeMapper;
    private final ReviewValidationSupport reviewValidationSupport;

    public ExceptionTypeServiceImpl(ExceptionTypeMapper exceptionTypeMapper,
                                    ReviewValidationSupport reviewValidationSupport) {
        this.exceptionTypeMapper = exceptionTypeMapper;
        this.reviewValidationSupport = reviewValidationSupport;
    }

    @Override
    public PageResult<ExceptionTypeVO> list(ExceptionTypeQueryDTO dto) {
        ExceptionTypeQueryDTO validatedDTO = reviewValidationSupport.validateExceptionTypeQuery(dto);
        List<ExceptionType> entities = exceptionTypeMapper.selectList(Wrappers.<ExceptionType>lambdaQuery()
                .eq(validatedDTO.getStatus() != null, ExceptionType::getStatus, validatedDTO.getStatus())
                .orderByDesc(ExceptionType::getUpdateTime)
                .orderByDesc(ExceptionType::getId));
        long total = entities.size();
        int fromIndex = (validatedDTO.getPageNum().intValue() - 1) * validatedDTO.getPageSize().intValue();
        if (fromIndex >= entities.size()) {
            return new PageResult<ExceptionTypeVO>(Long.valueOf(total), Collections.<ExceptionTypeVO>emptyList());
        }
        int toIndex = Math.min(fromIndex + validatedDTO.getPageSize().intValue(), entities.size());
        List<ExceptionTypeVO> records = new ArrayList<ExceptionTypeVO>();
        for (ExceptionType entity : entities.subList(fromIndex, toIndex)) {
            records.add(toVO(entity));
        }
        return new PageResult<ExceptionTypeVO>(Long.valueOf(total), records);
    }

    @Override
    public void update(ExceptionTypeUpdateDTO dto) {
        ExceptionTypeUpdateDTO validatedDTO = reviewValidationSupport.validateExceptionTypeUpdate(dto);
        ExceptionType entity = exceptionTypeMapper.selectOne(Wrappers.<ExceptionType>lambdaQuery()
                .eq(ExceptionType::getCode, validatedDTO.getCode())
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "异常类型配置不存在");
        }
        entity.setName(validatedDTO.getName());
        entity.setDescription(validatedDTO.getDescription());
        entity.setStatus(validatedDTO.getStatus());
        exceptionTypeMapper.updateById(entity);
    }

    private ExceptionTypeVO toVO(ExceptionType entity) {
        ExceptionTypeVO vo = new ExceptionTypeVO();
        vo.setCode(entity.getCode());
        vo.setName(resolveName(entity));
        vo.setDescription(resolveDescription(entity));
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private String resolveName(ExceptionType entity) {
        if (entity == null) {
            return null;
        }
        String name = entity.getName();
        if (StringUtils.hasText(name) && !looksLikeQuestionPlaceholder(name.trim())) {
            return name.trim();
        }
        return DEFAULT_EXCEPTION_NAMES.getOrDefault(entity.getCode(), StringUtils.hasText(name) ? name.trim() : null);
    }

    private String resolveDescription(ExceptionType entity) {
        if (entity == null) {
            return null;
        }
        String description = entity.getDescription();
        if (StringUtils.hasText(description) && !looksLikeQuestionPlaceholder(description.trim())) {
            return description.trim();
        }
        return DEFAULT_EXCEPTION_DESCRIPTIONS.getOrDefault(entity.getCode(), StringUtils.hasText(description) ? description.trim() : null);
    }

    private boolean looksLikeQuestionPlaceholder(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        int placeholderCount = 0;
        int meaningfulCount = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isWhitespace(current)
                    || current == ',' || current == '，'
                    || current == '.' || current == '。'
                    || current == ';' || current == '；'
                    || current == ':' || current == '：'
                    || current == '!' || current == '！'
                    || current == '(' || current == ')'
                    || current == '（' || current == '）') {
                continue;
            }
            meaningfulCount++;
            if (current == '?' || current == '？' || current == '\uFFFD') {
                placeholderCount++;
            }
        }
        return meaningfulCount >= 2 && placeholderCount == meaningfulCount;
    }

    private static Map<String, String> buildDefaultExceptionDescriptions() {
        Map<String, String> descriptions = new HashMap<String, String>();
        descriptions.put("PROXY_CHECKIN", "系统怀疑本次打卡并非本人完成，建议尽快核实。");
        descriptions.put("CONTINUOUS_LATE", "最近一周多次晚于上班时间打卡，已形成持续迟到情况。");
        descriptions.put("CONTINUOUS_EARLY_LEAVE", "最近一周多次早于下班时间打卡，已形成持续早退情况。");
        descriptions.put("CONTINUOUS_MULTI_LOCATION_CONFLICT", "最近一周多次出现打卡地点跨度过大，疑似异地打卡。");
        descriptions.put("CONTINUOUS_ILLEGAL_TIME", "最近一周多次在异常时段打卡，已形成持续异常时段行为。");
        descriptions.put("CONTINUOUS_REPEAT_CHECK", "最近一周多次短时间重复打卡，建议核实是否存在重复提交或操作异常。");
        descriptions.put("CONTINUOUS_PROXY_CHECKIN", "最近一周多次出现可疑代打卡迹象，建议优先人工复核。");
        descriptions.put("CONTINUOUS_ATTENDANCE_RISK", "最近一周多次出现不同类型的异常打卡，说明出勤情况持续不稳定。");
        descriptions.put("COMPLEX_ATTENDANCE_RISK", "系统综合设备、地点和行为特征后，认为本次打卡存在可疑风险。");
        descriptions.put("CONTINUOUS_MODEL_RISK", "最近一周多次被系统识别为可疑打卡，建议持续关注。");
        descriptions.put("LATE", "上班打卡晚于规则时间，系统判定为迟到。");
        descriptions.put("EARLY_LEAVE", "下班打卡早于规则时间，系统判定为早退。");
        descriptions.put("ILLEGAL_TIME", "打卡时间不在当前规则允许的时段内。");
        descriptions.put("REPEAT_CHECK", "短时间内重复提交了同类打卡。");
        descriptions.put("MULTI_LOCATION_CONFLICT", "本次打卡地点与前后记录差异过大，系统判定为异地打卡。");
        descriptions.put("ABSENT", "在规定时段内未完成上班打卡。");
        descriptions.put("MISSING_CHECKOUT", "当日已上班打卡，但未完成下班打卡。");
        return Collections.unmodifiableMap(descriptions);
    }

    private static Map<String, String> buildDefaultExceptionNames() {
        Map<String, String> names = new HashMap<String, String>();
        names.put("PROXY_CHECKIN", "可疑代打卡");
        names.put("CONTINUOUS_LATE", "多次迟到");
        names.put("CONTINUOUS_EARLY_LEAVE", "多次早退");
        names.put("CONTINUOUS_MULTI_LOCATION_CONFLICT", "多次异地打卡");
        names.put("CONTINUOUS_ILLEGAL_TIME", "多次异常时段打卡");
        names.put("CONTINUOUS_REPEAT_CHECK", "多次重复打卡");
        names.put("CONTINUOUS_PROXY_CHECKIN", "多次可疑代打卡");
        names.put("CONTINUOUS_ATTENDANCE_RISK", "多次异常打卡");
        names.put("COMPLEX_ATTENDANCE_RISK", "可疑打卡");
        names.put("CONTINUOUS_MODEL_RISK", "多次可疑打卡");
        names.put("LATE", "迟到");
        names.put("EARLY_LEAVE", "早退");
        names.put("ILLEGAL_TIME", "异常时段打卡");
        names.put("REPEAT_CHECK", "重复打卡");
        names.put("MULTI_LOCATION_CONFLICT", "异地打卡");
        names.put("ABSENT", "缺勤");
        names.put("MISSING_CHECKOUT", "未打下班卡");
        return Collections.unmodifiableMap(names);
    }
}
