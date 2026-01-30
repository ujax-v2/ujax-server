package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

public class InvalidParameterException extends BusinessException {
	public InvalidParameterException(ErrorCode errorCode) {
		super(errorCode);
	}

	public InvalidParameterException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	@Override
	public boolean isNecessaryToLog() {
		return false;
	}
}
