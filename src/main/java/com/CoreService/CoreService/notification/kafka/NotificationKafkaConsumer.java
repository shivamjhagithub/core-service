package com.CoreService.CoreService.notification.kafka;

import com.CoreService.CoreService.common.redis.RedisService;
import com.CoreService.CoreService.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Consumes the intra-application notification topic. Kafka guarantees at least
 * once delivery and a rebalance replays uncommitted offsets, so the event key
 * is checked against Redis first: the same domain event never produces a second
 * set of inbox rows.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private static final String PROCESSED_KEY_PREFIX = "KAFKA_PROCESSED:";
    private static final Duration PROCESSED_TTL = Duration.ofHours(24);

    private final NotificationDispatcher notificationDispatcher;
    private final RedisService redisService;

    @KafkaListener(topics = NotificationMessage.TOPIC, groupId = "core-service-notifications")
    public void onNotificationMessage(String payload) {
        Optional<NotificationMessage> decoded = NotificationMessage.decode(payload);
        if (decoded.isEmpty()) {
            log.warn("Discarded an unreadable message on {}", NotificationMessage.TOPIC);
            return;
        }

        NotificationMessage message = decoded.get();
        String processedKey = PROCESSED_KEY_PREFIX + message.eventKey();

        if (redisService.exists(processedKey)) {
            log.debug("Skipping notification event {}, it was already processed", message.eventKey());
            return;
        }

        notificationDispatcher.dispatch(message.toRequest());
        redisService.save(processedKey, Instant.now().toString(), PROCESSED_TTL);
    }
}
