package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

public class Judge0Exception extends BusinessException {
    public Judge0Exception(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public boolean isNecessaryToLog() {
        return true;
    }
}
