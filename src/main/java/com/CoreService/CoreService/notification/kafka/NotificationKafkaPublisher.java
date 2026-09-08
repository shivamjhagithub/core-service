package com.CoreService.CoreService.notification.kafka;

import com.CoreService.CoreService.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka is intra-application asynchronous processing here, not inter-service
 * messaging: producer and consumer live in this same deployable and the topic
 * only exists to take notification fan-out off the thread that triggered it.
 * With {@code app.kafka.enabled=false} the bean is absent and callers fall back
 * to dispatching in process, so the monolith never depends on a broker.
 * <p>
 * Messages are keyed by the originating event key, which keeps all retries of
 * one domain event on the same partition and lets the consumer deduplicate.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class NotificationKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String eventKey, NotificationRequest request) {
        if (eventKey == null || eventKey.isBlank() || request == null) {
            return;
        }

        String payload = NotificationMessage.encode(eventKey, request);
        kafkaTemplate.send(NotificationMessage.TOPIC, eventKey, payload)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Could not publish notification event {} to {}",
                                eventKey, NotificationMessage.TOPIC, throwable);
                    }
                });
    }
}
