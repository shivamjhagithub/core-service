package com.CoreService.CoreService.notification.kafka;

import com.CoreService.CoreService.notification.dto.NotificationRequest;
import com.CoreService.CoreService.notification.enums.NotificationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Wire format of the {@code erp.notifications} topic.
 * <p>
 * The broker is configured with plain string serializers, so the payload is a
 * versioned, pipe-delimited line rather than JSON. Separators are stripped out
 * of every free-text field on the way in, which makes decoding unambiguous
 * without pulling a mapper into the path.
 * <pre>
 * v1|eventKey|collegeId|type|referenceId|title|body|recipient,recipient,...
 * </pre>
 */
public record NotificationMessage(String eventKey,
                                  UUID collegeId,
                                  NotificationType type,
                                  UUID referenceId,
                                  String title,
                                  String body,
                                  List<String> recipientUserIds) {

    public static final String TOPIC = "erp.notifications";

    private static final String VERSION = "v1";
    private static final String FIELD_SEPARATOR = "|";
    private static final String FIELD_SPLIT_PATTERN = "\\|";
    private static final String RECIPIENT_SEPARATOR = ",";
    private static final int FIELD_COUNT = 8;

    public static String encode(String eventKey, NotificationRequest request) {
        return String.join(FIELD_SEPARATOR,
                VERSION,
                sanitize(eventKey),
                asText(request.collegeId()),
                request.type() == null ? "" : request.type().name(),
                asText(request.referenceId()),
                sanitize(request.title()),
                sanitize(request.body()),
                encodeRecipients(request.recipientUserIds()));
    }

    public static Optional<NotificationMessage> decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String[] fields = raw.split(FIELD_SPLIT_PATTERN, -1);
        if (fields.length != FIELD_COUNT || !VERSION.equals(fields[0])) {
            return Optional.empty();
        }

        UUID collegeId = parseUuid(fields[2]);
        NotificationType type = parseType(fields[3]);
        if (collegeId == null || type == null || fields[1].isBlank() || fields[5].isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new NotificationMessage(
                fields[1],
                collegeId,
                type,
                parseUuid(fields[4]),
                fields[5],
                fields[6].isEmpty() ? null : fields[6],
                decodeRecipients(fields[7])));
    }

    public NotificationRequest toRequest() {
        return NotificationRequest.builder()
                .collegeId(collegeId)
                .recipientUserIds(recipientUserIds)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .build();
    }

    private static String encodeRecipients(Iterable<String> recipientUserIds) {
        if (recipientUserIds == null) {
            return "";
        }
        StringBuilder joined = new StringBuilder();
        for (String recipientUserId : recipientUserIds) {
            if (recipientUserId == null || recipientUserId.isBlank()) {
                continue;
            }
            if (!joined.isEmpty()) {
                joined.append(RECIPIENT_SEPARATOR);
            }
            joined.append(sanitize(recipientUserId).replace(RECIPIENT_SEPARATOR, ""));
        }
        return joined.toString();
    }

    private static List<String> decodeRecipients(String field) {
        if (field == null || field.isBlank()) {
            return List.of();
        }
        List<String> recipients = new ArrayList<>();
        for (String recipientUserId : field.split(RECIPIENT_SEPARATOR)) {
            String trimmed = recipientUserId.trim();
            if (!trimmed.isEmpty()) {
                recipients.add(trimmed);
            }
        }
        return List.copyOf(recipients);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(FIELD_SEPARATOR, "/")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
    }

    private static String asText(UUID value) {
        return value == null ? "" : value.toString();
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static NotificationType parseType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return NotificationType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
