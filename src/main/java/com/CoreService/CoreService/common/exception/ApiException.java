package com.CoreService.CoreService.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for every exception that maps to a deliberate HTTP response.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    protected ApiException(String message, ErrorCode errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
