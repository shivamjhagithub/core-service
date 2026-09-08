package com.CoreService.CoreService.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {

    public BusinessException(String message) {
        super(message, ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, ErrorCode errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
}
