package com.quyong.attendance.module.statistics.service.impl;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.statistics.dto.OperationLogQueryDTO;
import com.quyong.attendance.module.statistics.entity.OperationLog;
import com.quyong.attendance.module.statistics.mapper.OperationLogMapper;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import com.quyong.attendance.module.statistics.support.StatisticsValidationSupport;
import com.quyong.attendance.module.statistics.vo.OperationLogVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
        int offset = (safe.getPageNum().intValue() - 1) * safe.getPageSize().intValue();
        long total = operationLogMapper.countByQuery(safe.getUserId(), safe.getType(), startTime, endTime);
        List<OperationLogVO> records = operationLogMapper.selectPageByQuery(
                safe.getUserId(),
                safe.getType(),
                startTime,
                endTime,
                safe.getPageSize().intValue(),
                offset
        );
        return new PageResult<OperationLogVO>(Long.valueOf(total), records);
    }

    @Override
    public List<OperationLogVO> listAll(OperationLogQueryDTO dto) {
        OperationLogQueryDTO safe = statisticsValidationSupport.validateOperationLogQuery(dto);
        LocalDateTime startTime = statisticsValidationSupport.parseQueryStart(safe.getStartDate());
        LocalDateTime endTime = statisticsValidationSupport.parseQueryEnd(safe.getEndDate());
        return operationLogMapper.selectAllByQuery(safe.getUserId(), safe.getType(), startTime, endTime);
    }
}
