package com.CoreService.CoreService.common.websocket;

import org.springframework.messaging.MessagingException;

/**
 * Aborts a STOMP frame. Thrown from the inbound channel interceptor, which is
 * outside the reach of {@code @RestControllerAdvice}.
 */
public class WebSocketAccessDeniedException extends MessagingException {

    public WebSocketAccessDeniedException(String message) {
        super(message);
    }
}
