package com.uws.notification_service.events;

import com.uws.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Consumes transaction lifecycle events from {@code transaction.events} and turns each one into
 * automatic notifications for both parties (debitor = sender, creditor = receiver).
 *
 * <p>Events are published by the transaction-service (Saga orchestrator). We dispatch on
 * {@code eventType} and let any exception propagate so Kafka retries the record.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${spring.kafka.topics.transaction-events:transaction.events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransactionEvent(@Payload Map<String, Object> event, Acknowledgment ack) {
        String eventType = event != null ? (String) event.get("eventType") : null;
        String transactionId = event != null ? (String) event.get("transactionId") : null;

        if (eventType == null || transactionId == null) {
            log.warn("Ignoring transaction event without eventType/transactionId: {}", event);
            ack.acknowledge();
            return;
        }

        log.info("Received transaction event: type={}, transactionId={}", eventType, transactionId);

        switch (eventType) {
            case "TransactionCompleted" -> notificationService.onTransactionCompleted(event);
            case "TransactionFailed" -> notificationService.onTransactionFailed(event);
            case "TransactionReversed" -> notificationService.onTransactionReversed(event);
            case "TransactionInitiated" -> log.debug("TransactionInitiated ignored (no notification yet): {}", transactionId);
            default -> log.warn("Unhandled transaction event type: {}", eventType);
        }

        ack.acknowledge();
    }
}
