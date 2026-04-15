package com.quyong.attendance.module.notification.service.impl;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.notification.config.NotificationProperties;
import com.quyong.attendance.module.notification.dto.NotificationCreateCommand;
import com.quyong.attendance.module.notification.dto.NotificationQueryDTO;
import com.quyong.attendance.module.notification.entity.NotificationRecord;
import com.quyong.attendance.module.notification.mapper.NotificationRecordMapper;
import com.quyong.attendance.module.notification.service.NotificationService;
import com.quyong.attendance.module.notification.vo.NotificationVO;
import com.quyong.attendance.module.user.entity.User;
import com.quyong.attendance.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Integer READ_STATUS_UNREAD = Integer.valueOf(0);
    private static final Integer READ_STATUS_READ = Integer.valueOf(1);
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final List<String> EMPLOYEE_VISIBLE_CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            "EXCEPTION_NOTICE",
            "REQUEST_EXPLANATION",
            "EMPLOYEE_REPLY_REMINDER",
            "REVIEW_RESULT",
            "REPAIR_RESULT",
            "FACE_REGISTER_RESULT"
    ));

    private final NotificationRecordMapper notificationRecordMapper;
    private final UserMapper userMapper;
    private final NotificationProperties notificationProperties;
    private final Clock clock;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitterMap = new ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>();

    public NotificationServiceImpl(NotificationRecordMapper notificationRecordMapper,
                                   UserMapper userMapper,
                                   NotificationProperties notificationProperties,
                                   Clock clock) {
        this.notificationRecordMapper = notificationRecordMapper;
        this.userMapper = userMapper;
        this.notificationProperties = notificationProperties;
        this.clock = clock;
    }

    @Override
    public PageResult<NotificationVO> list(Long userId, String roleCode, NotificationQueryDTO dto) {
        Long recipientUserId = requireUserId(userId);
        NotificationQueryDTO safe = normalizeQuery(dto);
        List<String> visibleCategories = resolveVisibleCategories(roleCode, safe.getCategory());
        int offset = (safe.getPageNum().intValue() - 1) * safe.getPageSize().intValue();
        long total = notificationRecordMapper.countByQuery(recipientUserId, safe.getReadStatus(), safe.getCategory(), visibleCategories);
        List<NotificationRecord> records = notificationRecordMapper.selectPageByQuery(
                recipientUserId,
                safe.getReadStatus(),
                safe.getCategory(),
                visibleCategories,
                safe.getPageSize().intValue(),
                offset
        );
        List<NotificationVO> items = new ArrayList<NotificationVO>();
        for (NotificationRecord record : records) {
            items.add(toVO(record));
        }
        return new PageResult<NotificationVO>(Long.valueOf(total), items);
    }

    @Override
    public NotificationVO detail(Long userId, String roleCode, Long notificationId) {
        NotificationRecord notificationRecord = requireOwnedNotification(userId, roleCode, notificationId);
        return toVO(notificationRecord);
    }

    @Override
    public long unreadCount(Long userId, String roleCode) {
        return notificationRecordMapper.countUnread(requireUserId(userId), resolveVisibleCategories(roleCode, null));
    }

    @Override
    public void markRead(Long userId, String roleCode, Long notificationId) {
        NotificationRecord notificationRecord = requireOwnedNotification(userId, roleCode, notificationId);
        if (READ_STATUS_READ.equals(notificationRecord.getReadStatus())) {
            return;
        }
        notificationRecord.setReadStatus(READ_STATUS_READ);
        notificationRecord.setReadTime(LocalDateTime.now(clock));
        notificationRecordMapper.updateById(notificationRecord);
        publishUnreadCount(notificationRecord.getRecipientUserId(), roleCode);
    }

    @Override
    public void markAllRead(Long userId, String roleCode) {
        Long recipientUserId = requireUserId(userId);
        List<String> visibleCategories = resolveVisibleCategories(roleCode, null);
        List<NotificationRecord> unreadRecords = notificationRecordMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<NotificationRecord>lambdaQuery()
                        .eq(NotificationRecord::getRecipientUserId, recipientUserId)
                        .eq(NotificationRecord::getReadStatus, READ_STATUS_UNREAD)
        );
        LocalDateTime now = LocalDateTime.now(clock);
        for (NotificationRecord record : unreadRecords) {
            if (!isVisibleCategory(record.getCategory(), visibleCategories)) {
                continue;
            }
            record.setReadStatus(READ_STATUS_READ);
            record.setReadTime(now);
            notificationRecordMapper.updateById(record);
        }
        publishUnreadCount(recipientUserId, roleCode);
    }

    @Override
    public SseEmitter stream(Long userId, String roleCode) {
        final Long recipientUserId = requireUserId(userId);
        final String currentRoleCode = roleCode;
        Long timeoutMs = notificationProperties.getStreamTimeoutMs();
        final SseEmitter emitter = new SseEmitter(timeoutMs == null ? 600000L : timeoutMs.longValue());
        CopyOnWriteArrayList<SseEmitter> emitters = emitterMap.get(recipientUserId);
        if (emitters == null) {
            emitters = new CopyOnWriteArrayList<SseEmitter>();
            emitterMap.put(recipientUserId, emitters);
        }
        emitters.add(emitter);
        final CopyOnWriteArrayList<SseEmitter> targetEmitters = emitters;
        emitter.onCompletion(new Runnable() {
            @Override
            public void run() {
                targetEmitters.remove(emitter);
            }
        });
        emitter.onTimeout(new Runnable() {
            @Override
            public void run() {
                targetEmitters.remove(emitter);
                emitter.complete();
            }
        });
        sendUnreadCount(emitter, recipientUserId, currentRoleCode);
        return emitter;
    }

    @Override
    public void push(NotificationCreateCommand command) {
        if (command == null || command.getRecipientUserId() == null || command.getBusinessId() == null
                || !StringUtils.hasText(command.getBusinessType()) || !StringUtils.hasText(command.getCategory())
                || !StringUtils.hasText(command.getTitle()) || !StringUtils.hasText(command.getContent())) {
            return;
        }
        NotificationRecord notificationRecord = new NotificationRecord();
        notificationRecord.setRecipientUserId(command.getRecipientUserId());
        notificationRecord.setSenderUserId(command.getSenderUserId());
        notificationRecord.setBusinessType(command.getBusinessType().trim());
        notificationRecord.setBusinessId(command.getBusinessId());
        notificationRecord.setCategory(command.getCategory().trim());
        notificationRecord.setTitle(limitText(command.getTitle(), 120));
        notificationRecord.setContent(sanitizeNotificationContent(limitText(command.getContent(), 1000), command.getCategory()));
        notificationRecord.setLevel(StringUtils.hasText(command.getLevel()) ? limitText(command.getLevel(), 16) : "INFO");
        notificationRecord.setActionCode(StringUtils.hasText(command.getActionCode()) ? limitText(command.getActionCode(), 32) : "VIEW");
        notificationRecord.setReadStatus(READ_STATUS_UNREAD);
        notificationRecord.setDeadline(command.getDeadline());
        notificationRecord.setExtraJson(limitText(command.getExtraJson(), 4000));
        notificationRecordMapper.insert(notificationRecord);
        publishUnreadCount(notificationRecord.getRecipientUserId(), null);
    }

    @Override
    public boolean hasRecentNotification(Long recipientUserId, String category, Long businessId, LocalDateTime since) {
        if (recipientUserId == null || businessId == null || since == null || !StringUtils.hasText(category)) {
            return false;
        }
        return notificationRecordMapper.countRecentByCategory(recipientUserId, category.trim(), businessId, since) > 0L;
    }

    private NotificationQueryDTO normalizeQuery(NotificationQueryDTO dto) {
        NotificationQueryDTO safe = dto == null ? new NotificationQueryDTO() : dto;
        safe.setPageNum(safe.getPageNum() == null || safe.getPageNum().intValue() < 1 ? Integer.valueOf(1) : safe.getPageNum());
        safe.setPageSize(safe.getPageSize() == null || safe.getPageSize().intValue() < 1 ? Integer.valueOf(20) : safe.getPageSize());
        if (safe.getReadStatus() != null && !READ_STATUS_UNREAD.equals(safe.getReadStatus()) && !READ_STATUS_READ.equals(safe.getReadStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "通知状态不合法");
        }
        if (safe.getCategory() != null) {
            String trimmed = safe.getCategory().trim();
            safe.setCategory(trimmed.isEmpty() ? null : trimmed);
        }
        return safe;
    }

    private NotificationRecord requireOwnedNotification(Long userId, String roleCode, Long notificationId) {
        requireUserId(notificationId);
        NotificationRecord notificationRecord = notificationRecordMapper.selectById(notificationId);
        if (notificationRecord == null || !requireUserId(userId).equals(notificationRecord.getRecipientUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "通知不存在");
        }
        if (!isVisibleCategory(notificationRecord.getCategory(), resolveVisibleCategories(roleCode, null))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "通知不存在");
        }
        return notificationRecord;
    }

    private Long requireUserId(Long userId) {
        if (userId == null || userId.longValue() <= 0L) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return userId;
    }

    private NotificationVO toVO(NotificationRecord entity) {
        NotificationVO vo = new NotificationVO();
        vo.setId(entity.getId());
        vo.setRecipientUserId(entity.getRecipientUserId());
        vo.setSenderUserId(entity.getSenderUserId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setCategory(entity.getCategory());
        vo.setTitle(entity.getTitle());
        vo.setContent(sanitizeNotificationContent(entity.getContent(), entity.getCategory()));
        vo.setLevel(entity.getLevel());
        vo.setActionCode(entity.getActionCode());
        vo.setReadStatus(entity.getReadStatus());
        vo.setDeadline(entity.getDeadline());
        vo.setExtraJson(entity.getExtraJson());
        vo.setCreateTime(entity.getCreateTime());
        vo.setReadTime(entity.getReadTime());
        if (entity.getSenderUserId() != null) {
            User sender = userMapper.selectById(entity.getSenderUserId());
            if (sender != null) {
                vo.setSenderName(StringUtils.hasText(sender.getRealName()) ? sender.getRealName() : sender.getUsername());
            }
        }
        if (!StringUtils.hasText(vo.getSenderName()) && entity.getSenderUserId() == null) {
            vo.setSenderName("系统");
        }
        return vo;
    }

    private void publishUnreadCount(Long userId, String roleCode) {
        CopyOnWriteArrayList<SseEmitter> emitters = emitterMap.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        List<SseEmitter> invalidEmitters = new ArrayList<SseEmitter>();
        for (SseEmitter emitter : emitters) {
            try {
                sendUnreadCount(emitter, userId, roleCode);
            } catch (RuntimeException exception) {
                invalidEmitters.add(emitter);
            }
        }
        if (!invalidEmitters.isEmpty()) {
            emitters.removeAll(invalidEmitters);
        }
    }

    private void sendUnreadCount(SseEmitter emitter, Long userId, String roleCode) {
        try {
            emitter.send(buildUnreadPayload(userId, roleCode));
        } catch (IOException exception) {
            emitter.completeWithError(exception);
            throw new IllegalStateException(exception);
        }
    }

    private String buildUnreadPayload(Long userId, String roleCode) {
        return "{\"unreadCount\":" + unreadCount(userId, roleCode) + ",\"eventTime\":\"" + LocalDateTime.now(clock) + "\"}";
    }

    private List<String> resolveVisibleCategories(String roleCode, String category) {
        if (!ROLE_EMPLOYEE.equals(roleCode)) {
            return Collections.emptyList();
        }
        if (StringUtils.hasText(category) && !EMPLOYEE_VISIBLE_CATEGORIES.contains(category.trim())) {
            return Collections.singletonList("__NO_MATCH__");
        }
        return EMPLOYEE_VISIBLE_CATEGORIES;
    }

    private boolean isVisibleCategory(String category, List<String> visibleCategories) {
        if (visibleCategories == null || visibleCategories.isEmpty()) {
            return true;
        }
        return StringUtils.hasText(category) && visibleCategories.contains(category.trim());
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String sanitizeNotificationContent(String content, String category) {
        return sanitizeQuestionPlaceholder(content, resolveNotificationFallback(category));
    }

    private String resolveNotificationFallback(String category) {
        if ("REQUEST_EXPLANATION".equals(category)) {
            return "历史说明请求内容无法直接显示，请联系管理员重新发起说明请求。";
        }
        if ("EMPLOYEE_REPLY".equals(category)) {
            return "历史员工说明内容无法直接显示，请联系员工重新补充说明。";
        }
        if ("REVIEW_RESULT".equals(category)) {
            return "历史复核结果说明无法直接显示，请联系管理员查看原始记录。";
        }
        return "历史通知内容无法直接显示，请联系管理员查看原始记录。";
    }

    private String sanitizeQuestionPlaceholder(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        if (!looksLikeQuestionPlaceholder(normalized)) {
            return normalized;
        }
        return StringUtils.hasText(fallback) ? fallback : normalized;
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
        return meaningfulCount >= 3 && placeholderCount == meaningfulCount;
    }
}
