package com.multi.runrunbackend.domain.notification.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.notification.dto.NotificationResDto;
import com.multi.runrunbackend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@AuthenticationPrincipal CustomUser principal) {
    return notificationService.subscribe(principal);
  }

  @GetMapping("/remaining")
  public ResponseEntity<ApiResponse<Slice<NotificationResDto>>> remaining(
      @AuthenticationPrincipal CustomUser principal,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    Slice<NotificationResDto> unreadSlice = notificationService.getRemainingNotifications(principal,
        pageable);
    return ResponseEntity.ok(ApiResponse.success("삭제되지 않은 알림 조회 성공", unreadSlice));
  }

  @PatchMapping("/{id}/read")
  public ResponseEntity<ApiResponse<Void>> readNotification(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUser principal
  ) {
    notificationService.markAsRead(id, principal);
    return ResponseEntity.ok(ApiResponse.success("알림 읽음 처리 완료", null));
  }

}