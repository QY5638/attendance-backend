package com.quyong.attendance.module.warning.support;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.warning.dto.RiskLevelQueryDTO;
import com.quyong.attendance.module.warning.dto.RiskLevelUpdateDTO;
import com.quyong.attendance.module.warning.vo.RiskLevelConfigVO;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RiskLevelRegistry {

    private final Map<String, RiskLevelConfigVO> configs = new LinkedHashMap<String, RiskLevelConfigVO>();

    @PostConstruct
    public synchronized void init() {
        resetDefaults();
    }

    public synchronized void resetDefaults() {
        configs.clear();
        configs.put("HIGH", createConfig("HIGH", "高风险", "需要优先人工复核", 1));
        configs.put("MEDIUM", createConfig("MEDIUM", "中风险", "建议尽快关注并结合历史记录判断", 1));
        configs.put("LOW", createConfig("LOW", "低风险", "记录留档并持续观察", 1));
    }

    public synchronized PageResult<RiskLevelConfigVO> list(RiskLevelQueryDTO queryDTO) {
        List<RiskLevelConfigVO> filtered = new ArrayList<RiskLevelConfigVO>();
        for (RiskLevelConfigVO config : configs.values()) {
            if (queryDTO.getStatus() != null && !queryDTO.getStatus().equals(config.getStatus())) {
                continue;
            }
            filtered.add(copy(config));
        }
        int fromIndex = (queryDTO.getPageNum().intValue() - 1) * queryDTO.getPageSize().intValue();
        if (fromIndex >= filtered.size()) {
            return new PageResult<RiskLevelConfigVO>(Long.valueOf(filtered.size()), new ArrayList<RiskLevelConfigVO>());
        }
        int toIndex = Math.min(fromIndex + queryDTO.getPageSize().intValue(), filtered.size());
        return new PageResult<RiskLevelConfigVO>(Long.valueOf(filtered.size()), new ArrayList<RiskLevelConfigVO>(filtered.subList(fromIndex, toIndex)));
    }

    public synchronized void update(RiskLevelUpdateDTO updateDTO) {
        RiskLevelConfigVO existing = configs.get(updateDTO.getCode());
        if (existing == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "风险等级配置不存在");
        }
        existing.setName(updateDTO.getName());
        existing.setDescription(updateDTO.getDescription());
        existing.setStatus(updateDTO.getStatus());
    }

    public synchronized RiskLevelConfigVO get(String code) {
        RiskLevelConfigVO config = configs.get(code);
        return config == null ? null : copy(config);
    }

    private RiskLevelConfigVO createConfig(String code, String name, String description, Integer status) {
        RiskLevelConfigVO vo = new RiskLevelConfigVO();
        vo.setCode(code);
        vo.setName(name);
        vo.setDescription(description);
        vo.setStatus(status);
        return vo;
    }

    private RiskLevelConfigVO copy(RiskLevelConfigVO source) {
        RiskLevelConfigVO vo = new RiskLevelConfigVO();
        vo.setCode(source.getCode());
        vo.setName(source.getName());
        vo.setDescription(source.getDescription());
        vo.setStatus(source.getStatus());
        return vo;
    }
}
