package com.quyong.attendance.module.review.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quyong.attendance.module.review.entity.ExceptionType;
import com.quyong.attendance.module.review.mapper.ExceptionTypeMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExceptionTypeCatalogService {

    private static final Map<String, String> DEFAULT_NAMES = buildDefaultNames();
    private static final Map<String, String> DEFAULT_DESCRIPTIONS = buildDefaultDescriptions();

    private final ExceptionTypeMapper exceptionTypeMapper;

    public ExceptionTypeCatalogService(ExceptionTypeMapper exceptionTypeMapper) {
        this.exceptionTypeMapper = exceptionTypeMapper;
    }

    public String resolveName(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        ExceptionType entity = findByCode(code);
        if (entity != null && StringUtils.hasText(entity.getName())) {
            return entity.getName().trim();
        }
        return DEFAULT_NAMES.getOrDefault(code, code);
    }

    public String resolveDescription(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        ExceptionType entity = findByCode(code);
        if (entity != null && StringUtils.hasText(entity.getDescription())) {
            return entity.getDescription().trim();
        }
        return DEFAULT_DESCRIPTIONS.getOrDefault(code, null);
    }

    private ExceptionType findByCode(String code) {
        return exceptionTypeMapper.selectOne(Wrappers.<ExceptionType>lambdaQuery()
                .eq(ExceptionType::getCode, code.trim())
                .last("LIMIT 1"));
    }

    private static Map<String, String> buildDefaultNames() {
        Map<String, String> names = new HashMap<String, String>();
        names.put("PROXY_CHECKIN", "可疑代打卡");
        names.put("CONTINUOUS_PROXY_CHECKIN", "连续代打卡");
        names.put("LATE", "迟到");
        names.put("CONTINUOUS_LATE", "连续迟到");
        names.put("EARLY_LEAVE", "早退");
        names.put("CONTINUOUS_EARLY_LEAVE", "连续早退");
        names.put("REPEAT_CHECK", "重复打卡");
        names.put("CONTINUOUS_REPEAT_CHECK", "连续重复打卡");
        names.put("ILLEGAL_TIME", "非法时间打卡");
        names.put("CONTINUOUS_ILLEGAL_TIME", "连续非法时间打卡");
        names.put("MULTI_LOCATION_CONFLICT", "多地点异常");
        names.put("CONTINUOUS_MULTI_LOCATION_CONFLICT", "连续多地点冲突");
        names.put("ABSENT", "缺勤");
        names.put("MISSING_CHECKOUT", "下班缺卡");
        names.put("CONTINUOUS_ATTENDANCE_RISK", "连续综合考勤异常");
        names.put("COMPLEX_ATTENDANCE_RISK", "综合识别异常");
        names.put("CONTINUOUS_MODEL_RISK", "连续模型风险异常");
        return Collections.unmodifiableMap(names);
    }

    private static Map<String, String> buildDefaultDescriptions() {
        Map<String, String> descriptions = new HashMap<String, String>();
        descriptions.put("PROXY_CHECKIN", "系统怀疑本次打卡并非本人完成，建议尽快核实。");
        descriptions.put("CONTINUOUS_PROXY_CHECKIN", "最近一周多次出现可疑代打卡迹象，建议优先人工复核。");
        descriptions.put("LATE", "上班打卡晚于规则时间，系统判定为迟到。");
        descriptions.put("CONTINUOUS_LATE", "最近一周多次晚于上班时间打卡，已形成持续迟到情况。");
        descriptions.put("EARLY_LEAVE", "下班打卡早于规则时间，系统判定为早退。");
        descriptions.put("CONTINUOUS_EARLY_LEAVE", "最近一周多次早于下班时间打卡，已形成持续早退情况。");
        descriptions.put("REPEAT_CHECK", "短时间内重复提交了同类打卡。");
        descriptions.put("CONTINUOUS_REPEAT_CHECK", "最近一周多次短时间重复打卡，建议核实是否存在重复提交或操作异常。");
        descriptions.put("ILLEGAL_TIME", "打卡时间不在当前规则允许的时段内。");
        descriptions.put("CONTINUOUS_ILLEGAL_TIME", "最近一周多次在异常时段打卡，已形成持续异常时段行为。");
        descriptions.put("MULTI_LOCATION_CONFLICT", "本次打卡地点与前后记录差异过大，系统判定为空间冲突。");
        descriptions.put("CONTINUOUS_MULTI_LOCATION_CONFLICT", "最近一周多次出现打卡地点跨度过大，疑似持续异地打卡。");
        descriptions.put("ABSENT", "在规定时段内未完成上班打卡。");
        descriptions.put("MISSING_CHECKOUT", "当日已上班打卡，但未完成下班打卡。");
        descriptions.put("CONTINUOUS_ATTENDANCE_RISK", "最近一周多次出现不同类型规则异常，说明出勤情况持续不稳定。");
        descriptions.put("COMPLEX_ATTENDANCE_RISK", "系统综合设备、地点和行为特征后，认为本次打卡存在可疑风险。");
        descriptions.put("CONTINUOUS_MODEL_RISK", "最近一周多次被系统识别为可疑打卡，建议持续关注。");
        return Collections.unmodifiableMap(descriptions);
    }
}
