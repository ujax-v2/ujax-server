package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

/**
 * 400 Bad Request - 잘못된 파라미터인 경우
 */
public class InvalidParameterException extends BusinessException {

	public InvalidParameterException() {
		super(ErrorCode.INVALID_PARAMETER);
	}

	public InvalidParameterException(ErrorCode errorCode) {
		super(errorCode);
	}

	public InvalidParameterException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public InvalidParameterException(String message) {
		super(ErrorCode.INVALID_PARAMETER, message);
	}

	@Override
	public boolean isNecessaryToLog() {
		return false;
	}
}
