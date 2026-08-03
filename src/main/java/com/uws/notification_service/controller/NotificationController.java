package com.uws.notification_service.controller;

import com.uws.notification_service.model.dtos.NotificationPageResponse;
import com.uws.notification_service.model.dtos.NotificationResponse;
import com.uws.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * List the authenticated user's notifications, most recent first.
     */
    @GetMapping
    public ResponseEntity<NotificationPageResponse> getNotifications(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /notification - userId: {}, page: {}, size: {}", userId, page, size);

        NotificationPageResponse response = notificationService.getNotifications(
                userId, PageRequest.of(page, size));

        return ResponseEntity.ok(response);
    }

    /**
     * Unread notification count for the authenticated user.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestHeader("X-User-Id") String userId) {
        log.info("GET /notification/unread-count - userId: {}", userId);

        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Mark one of the authenticated user's notifications as read.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String notificationId) {

        log.info("PATCH /notification/{}/read - userId: {}", notificationId, userId);

        try {
            NotificationResponse response = notificationService.markAsRead(userId, notificationId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Mark-as-read failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
