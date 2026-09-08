package com.CoreService.CoreService.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a college tries to use an ERP module it has not enabled.
 */
public class ModuleDisabledException extends ApiException {

    public ModuleDisabledException(String moduleCode) {
        super("Module is not enabled for this college: " + moduleCode,
                ErrorCode.MODULE_DISABLED, HttpStatus.FORBIDDEN);
    }
}
