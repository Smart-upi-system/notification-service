package com.uws.notification_service.service;

import com.uws.notification_service.model.dtos.NotificationPageResponse;
import com.uws.notification_service.model.dtos.NotificationResponse;
import org.springframework.data.domain.PageRequest;

import java.util.Map;

public interface NotificationService {

    /**
     * Builds and persists automatic notifications for the sender (debitor) and receiver
     * (creditor) when a transfer completes.
     */
    void onTransactionCompleted(Map<String, Object> event);

    /** Notifies the sender when a transfer fails. */
    void onTransactionFailed(Map<String, Object> event);

    /** Notifies the sender when a failed transfer is compensated (refunded). */
    void onTransactionReversed(Map<String, Object> event);

    NotificationPageResponse getNotifications(String userId, PageRequest pageable);

    NotificationResponse markAsRead(String userId, String notificationId);

    long getUnreadCount(String userId);
}
