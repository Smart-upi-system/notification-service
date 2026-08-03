package com.uws.notification_service.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String notificationId;
    private String userId;
    private String role;
    private String type;
    private String title;
    private String message;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String counterpartyId;
    private boolean read;
    private LocalDateTime createdAt;
}
