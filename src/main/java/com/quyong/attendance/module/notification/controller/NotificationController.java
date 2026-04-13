package com.quyong.attendance.module.notification.controller;

import com.quyong.attendance.common.api.PageResult;
import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.auth.model.AuthUser;
import com.quyong.attendance.module.notification.dto.NotificationQueryDTO;
import com.quyong.attendance.module.notification.service.NotificationService;
import com.quyong.attendance.module.notification.vo.NotificationVO;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    public Result<PageResult<NotificationVO>> list(NotificationQueryDTO dto) {
        AuthUser authUser = currentAuthUser();
        return Result.success(notificationService.list(authUser.getUserId(), authUser.getRoleCode(), dto));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        AuthUser authUser = currentAuthUser();
        return Result.success(Long.valueOf(notificationService.unreadCount(authUser.getUserId(), authUser.getRoleCode())));
    }

    @GetMapping("/{id}")
    public Result<NotificationVO> detail(@PathVariable("id") Long id) {
        AuthUser authUser = currentAuthUser();
        return Result.success(notificationService.detail(authUser.getUserId(), authUser.getRoleCode(), id));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long id) {
        AuthUser authUser = currentAuthUser();
        notificationService.markRead(authUser.getUserId(), authUser.getRoleCode(), id);
        return Result.success(null);
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        AuthUser authUser = currentAuthUser();
        notificationService.markAllRead(authUser.getUserId(), authUser.getRoleCode());
        return Result.success(null);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        AuthUser authUser = currentAuthUser();
        return notificationService.stream(authUser.getUserId(), authUser.getRoleCode());
    }

    private AuthUser currentAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return (AuthUser) authentication.getPrincipal();
    }
}
