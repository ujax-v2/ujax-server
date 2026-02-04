package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

/**
 * 409 Conflict - 리소스 충돌이 발생한 경우 (중복 등)
 */
public class ConflictException extends BusinessException {

	public ConflictException() {
		super(ErrorCode.DUPLICATE_RESOURCE);
	}

	public ConflictException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ConflictException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public ConflictException(String message) {
		super(ErrorCode.DUPLICATE_RESOURCE, message);
	}

	/**
	 * 중복 리소스 예외 생성
	 * 예: ConflictException.duplicate("email", "test@example.com")
	 */
	public static ConflictException duplicate(String fieldName, Object value) {
		return new ConflictException(
			ErrorCode.DUPLICATE_RESOURCE,
			String.format("이미 존재하는 %s입니다: %s", fieldName, value)
		);
	}

	@Override
	public boolean isNecessaryToLog() {
		return false;
	}
}
