package com.CoreService.CoreService.common.security;

import com.CoreService.CoreService.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;

/**
 * Writes the same error envelope from security filters that
 * {@code GlobalExceptionHandler} produces for controllers. Errors raised inside
 * the filter chain never reach {@code @RestControllerAdvice}.
 */
final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    static void write(HttpServletResponse response,
                      HttpServletRequest request,
                      HttpStatus status,
                      String message,
                      ErrorCode errorCode) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"message":"%s","errorCode":"%s","timestamp":"%s","path":"%s"}"""
                .formatted(message, errorCode.name(), Instant.now(), request.getRequestURI()));
    }
}
