package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

/**
 * 404 Not Found - 리소스를 찾을 수 없는 경우
 */
public class NotFoundException extends BusinessException {

	public NotFoundException() {
		super(ErrorCode.RESOURCE_NOT_FOUND);
	}

	public NotFoundException(ErrorCode errorCode) {
		super(errorCode);
	}

	public NotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public NotFoundException(String message) {
		super(ErrorCode.RESOURCE_NOT_FOUND, message);
	}

	/**
	 * 특정 엔티티를 찾을 수 없을 때 사용
	 * 예: NotFoundException.of("User", userId)
	 */
	public static NotFoundException of(String entityName, Object id) {
		return new NotFoundException(
			ErrorCode.RESOURCE_NOT_FOUND,
			String.format("%s(id=%s)를 찾을 수 없습니다.", entityName, id)
		);
	}

	@Override
	public boolean isNecessaryToLog() {
		return true;
	}
}
