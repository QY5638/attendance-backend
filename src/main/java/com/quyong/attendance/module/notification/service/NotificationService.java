package com.quyong.attendance.module.notification.service;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.module.notification.dto.NotificationCreateCommand;
import com.quyong.attendance.module.notification.dto.NotificationQueryDTO;
import com.quyong.attendance.module.notification.vo.NotificationVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {

    PageResult<NotificationVO> list(Long userId, String roleCode, NotificationQueryDTO dto);

    NotificationVO detail(Long userId, String roleCode, Long notificationId);

    long unreadCount(Long userId, String roleCode);

    void markRead(Long userId, String roleCode, Long notificationId);

    void markAllRead(Long userId, String roleCode);

    SseEmitter stream(Long userId, String roleCode);

    void push(NotificationCreateCommand command);

    boolean hasRecentNotification(Long recipientUserId, String category, Long businessId, java.time.LocalDateTime since);
}
