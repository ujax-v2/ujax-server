package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

/**
 * 429 Too Many Requests - 요청 제한 초과 시
 */
public class TooManyRequestsException extends BusinessException {

	public TooManyRequestsException() {
		super(ErrorCode.TOO_MANY_REQUESTS);
	}

	public TooManyRequestsException(String message) {
		super(ErrorCode.TOO_MANY_REQUESTS, message);
	}

	public TooManyRequestsException(ErrorCode errorCode) {
		super(errorCode);
	}

	public TooManyRequestsException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	@Override
	public boolean isNecessaryToLog() {
		return true;
	}
}
