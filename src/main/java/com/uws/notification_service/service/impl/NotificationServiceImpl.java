package com.uws.notification_service.service.impl;

import com.uws.notification_service.model.Notification;
import com.uws.notification_service.model.dtos.NotificationPageResponse;
import com.uws.notification_service.model.dtos.NotificationResponse;
import com.uws.notification_service.repository.NotificationRepository;
import com.uws.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void onTransactionCompleted(Map<String, Object> event) {
        String transactionId = (String) event.get("transactionId");
        String senderId = (String) event.get("senderId");
        String receiverId = (String) event.get("receiverId");
        String senderUpiId = (String) event.get("senderUpiId");
        String receiverUpiId = (String) event.get("receiverUpiId");
        String remarks = (String) event.get("remarks");
        BigDecimal amount = toBigDecimal(event.get("amount"));
        String currency = str(event.get("currency"), "INR");

        // Debitor (sender) notification
        saveIfAbsent(Notification.builder()
                .userId(senderId)
                .role("DEBITOR")
                .type("TRANSACTION_COMPLETED")
                .title("Payment sent")
                .message("You sent " + formatAmount(amount) + " " + currency + " to "
                        + fallback(receiverUpiId, receiverId) + appendRemarks(remarks))
                .transactionId(transactionId)
                .amount(amount)
                .currency(currency)
                .counterpartyId(fallback(receiverUpiId, receiverId))
                .build());

        // Creditor (receiver) notification
        saveIfAbsent(Notification.builder()
                .userId(receiverId)
                .role("CREDITOR")
                .type("TRANSACTION_COMPLETED")
                .title("Money received")
                .message("You received " + formatAmount(amount) + " " + currency + " from "
                        + fallback(senderUpiId, senderId) + appendRemarks(remarks))
                .transactionId(transactionId)
                .amount(amount)
                .currency(currency)
                .counterpartyId(fallback(senderUpiId, senderId))
                .build());

        log.info("Created DEBITOR/CREDITOR notifications for transactionId={}", transactionId);
    }

    @Override
    @Transactional
    public void onTransactionFailed(Map<String, Object> event) {
        String transactionId = (String) event.get("transactionId");
        String senderId = (String) event.get("senderId");
        String receiverUpiId = (String) event.get("receiverUpiId");
        String reason = (String) event.get("reason");
        BigDecimal amount = toBigDecimal(event.get("amount"));
        String currency = str(event.get("currency"), "INR");

        saveIfAbsent(Notification.builder()
                .userId(senderId)
                .role("DEBITOR")
                .type("TRANSACTION_FAILED")
                .title("Payment failed")
                .message("Your payment of " + formatAmount(amount) + " " + currency + " to "
                        + fallback(receiverUpiId, "the recipient") + " failed."
                        + (reason != null ? " Reason: " + reason : ""))
                .transactionId(transactionId)
                .amount(amount)
                .currency(currency)
                .counterpartyId(fallback(receiverUpiId, null))
                .build());

        log.info("Created FAILED notification for sender, transactionId={}", transactionId);
    }

    @Override
    @Transactional
    public void onTransactionReversed(Map<String, Object> event) {
        String transactionId = (String) event.get("transactionId");
        String senderId = (String) event.get("senderId");
        String receiverUpiId = (String) event.get("receiverUpiId");
        String reason = (String) event.get("reason");
        BigDecimal amount = toBigDecimal(event.get("refundedAmount"));
        String currency = str(event.get("currency"), "INR");

        saveIfAbsent(Notification.builder()
                .userId(senderId)
                .role("DEBITOR")
                .type("TRANSACTION_REVERSED")
                .title("Payment reversed")
                .message("Your payment of " + formatAmount(amount) + " " + currency + " to "
                        + fallback(receiverUpiId, "the recipient") + " was reversed and refunded."
                        + (reason != null ? " Reason: " + reason : ""))
                .transactionId(transactionId)
                .amount(amount)
                .currency(currency)
                .counterpartyId(fallback(receiverUpiId, null))
                .build());

        log.info("Created REVERSED notification for sender, transactionId={}", transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(String userId, PageRequest pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> notifications = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return NotificationPageResponse.builder()
                .notifications(notifications)
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .unreadCount(notificationRepository.countByUserIdAndReadFalse(userId))
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String userId, String notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found for user: " + notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Inserts a notification, swallowing duplicate-key errors so a redelivered Kafka event
     * (at-least-once delivery) does not create duplicates. The unique key is
     * (transactionId, userId, type).
     */
    private void saveIfAbsent(Notification notification) {
        try {
            notificationRepository.save(notification);
        } catch (DataIntegrityViolationException e) {
            log.debug("Skipping duplicate notification: txn={}, user={}, type={}",
                    notification.getTransactionId(), notification.getUserId(), notification.getType());
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .role(n.getRole())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .transactionId(n.getTransactionId())
                .amount(n.getAmount())
                .currency(n.getCurrency())
                .counterpartyId(n.getCounterpartyId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String fallback(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private String appendRemarks(String remarks) {
        return remarks != null && !remarks.isBlank() ? ". Remarks: " + remarks : "";
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? String.format("%,.2f", amount) : "0.00";
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String str(Object value, String fallback) {
        return value != null ? value.toString() : fallback;
    }
}
