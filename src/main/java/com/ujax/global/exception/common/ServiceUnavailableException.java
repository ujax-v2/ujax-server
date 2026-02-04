package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

/**
 * 503 Service Unavailable - 서비스를 사용할 수 없는 경우
 */
public class ServiceUnavailableException extends BusinessException {

	public ServiceUnavailableException() {
		super(ErrorCode.SERVICE_UNAVAILABLE);
	}

	public ServiceUnavailableException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ServiceUnavailableException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public ServiceUnavailableException(String message) {
		super(ErrorCode.SERVICE_UNAVAILABLE, message);
	}

	@Override
	public boolean isNecessaryToLog() {
		return true;
	}
}
