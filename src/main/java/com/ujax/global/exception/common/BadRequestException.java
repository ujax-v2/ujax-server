package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

/**
 * 400 Bad Request - 잘못된 요청인 경우
 */
public class BadRequestException extends BusinessException {

	public BadRequestException() {
		super(ErrorCode.INVALID_INPUT);
	}

	public BadRequestException(ErrorCode errorCode) {
		super(errorCode);
	}

	public BadRequestException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public BadRequestException(String message) {
		super(ErrorCode.INVALID_INPUT, message);
	}

	@Override
	public boolean isNecessaryToLog() {
		return false;
	}
}
