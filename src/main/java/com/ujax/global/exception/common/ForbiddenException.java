package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

/**
 * 403 Forbidden - 접근 권한이 없는 경우
 */
public class ForbiddenException extends BusinessException {

	public ForbiddenException() {
		super(ErrorCode.ACCESS_DENIED);
	}

	public ForbiddenException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ForbiddenException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public ForbiddenException(String message) {
		super(ErrorCode.ACCESS_DENIED, message);
	}

	@Override
	public boolean isNecessaryToLog() {
		return false;
	}
}
