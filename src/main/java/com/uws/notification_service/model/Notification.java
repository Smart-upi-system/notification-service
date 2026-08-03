package com.uws.notification_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_transaction_id", columnList = "transaction_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_txn_user_type", columnNames = {"transaction_id", "user_id", "type"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private String notificationId;

    /** The user this notification is addressed to. */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** DEBITOR (sender) or CREDITOR (receiver). */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    /** TRANSACTION_COMPLETED | TRANSACTION_FAILED | TRANSACTION_REVERSED */
    @Column(name = "type", nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String message;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    /** UPI ID (fallback: user id) of the other party involved. */
    @Column(name = "counterparty_id", length = 100)
    private String counterpartyId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
