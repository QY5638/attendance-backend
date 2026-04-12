package com.quyong.attendance.module.statistics.service.impl;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.statistics.dto.OperationLogQueryDTO;
import com.quyong.attendance.module.statistics.entity.OperationLog;
import com.quyong.attendance.module.statistics.mapper.OperationLogMapper;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.statistics.support.StatisticsValidationSupport;
import com.quyong.attendance.module.statistics.vo.OperationLogSummaryVO;
import com.quyong.attendance.module.statistics.vo.OperationLogVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    private final OperationLogMapper operationLogMapper;
    private final StatisticsValidationSupport statisticsValidationSupport;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper,
                                   StatisticsValidationSupport statisticsValidationSupport) {
        this.operationLogMapper = operationLogMapper;
        this.statisticsValidationSupport = statisticsValidationSupport;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Long userId, String type, String content) {
        if (!StringUtils.hasText(type) || !StringUtils.hasText(content)) {
            return;
        }
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(userId);
            operationLog.setType(type.trim());
            operationLog.setContent(content.trim());
            operationLog.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(operationLog);
        } catch (RuntimeException exception) {
            log.warn("operationLog write skipped, userId={}, type={}, reason={}", userId, type, exception.getMessage());
        }
    }

    @Override
    public PageResult<OperationLogVO> list(OperationLogQueryDTO dto) {
        OperationLogQueryDTO safe = statisticsValidationSupport.validateOperationLogQuery(dto);
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        List<String> types = resolveTypes(safe);
        int offset = (safe.getPageNum().intValue() - 1) * safe.getPageSize().intValue();
        long total = operationLogMapper.countByQuery(safe.getUserId(), safe.getType(), types, startTime, endTime);
        List<OperationLogVO> records = operationLogMapper.selectPageByQuery(
                safe.getUserId(),
                safe.getType(),
                types,
                startTime,
                endTime,
                safe.getPageSize().intValue(),
                offset
        );
        return new PageResult<OperationLogVO>(Long.valueOf(total), records);
    }

    @Override
    public OperationLogSummaryVO summary(OperationLogQueryDTO dto) {
        OperationLogQueryDTO safe = statisticsValidationSupport.validateOperationLogQuery(dto);
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        List<String> types = resolveTypes(safe);

        long total = operationLogMapper.countByQuery(safe.getUserId(), safe.getType(), types, startTime, endTime);
        List<Map<String, Object>> rows = operationLogMapper.selectTypeSummaryByQuery(safe.getUserId(), safe.getType(), types, startTime, endTime);

        Map<String, Long> typeCounts = new LinkedHashMap<String, Long>();
        for (Map<String, Object> row : rows) {
            Object label = row.get("label") != null ? row.get("label") : row.get("LABEL");
            Object count = row.get("total") != null ? row.get("total") : row.get("TOTAL");
            if (label == null || count == null) {
                continue;
            }
            typeCounts.put(String.valueOf(label), count instanceof Number ? Long.valueOf(((Number) count).longValue()) : Long.valueOf(String.valueOf(count)));
        }

        OperationLogSummaryVO summaryVO = new OperationLogSummaryVO();
        summaryVO.setTotal(Long.valueOf(total));
        summaryVO.setTypeCounts(typeCounts);
        return summaryVO;
    }

    @Override
    public List<OperationLogVO> listAll(OperationLogQueryDTO dto) {
        OperationLogQueryDTO safe = statisticsValidationSupport.validateOperationLogQuery(dto);
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        return operationLogMapper.selectAllByQuery(safe.getUserId(), safe.getType(), resolveTypes(safe), startTime, endTime);
    }

    @Override
    public List<String> resolveTypes(OperationLogQueryDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTypes())) {
            return null;
        }
        String[] rawTypes = dto.getTypes().split(",");
        Set<String> uniqueTypes = new LinkedHashSet<String>();
        for (String rawType : rawTypes) {
            if (rawType == null) {
                continue;
            }
            String normalized = rawType.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            uniqueTypes.add(normalized.toUpperCase());
        }
        if (uniqueTypes.isEmpty()) {
            return null;
        }
        return new ArrayList<String>(uniqueTypes);
    }
}
