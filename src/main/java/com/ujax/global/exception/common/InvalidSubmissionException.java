package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

public class InvalidSubmissionException extends BusinessException {
    public InvalidSubmissionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidSubmissionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public boolean isNecessaryToLog() {
        return false;
    }
}
