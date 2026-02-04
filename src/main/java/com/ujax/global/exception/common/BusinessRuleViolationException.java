package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

/**
 * 422 Unprocessable Entity - 비즈니스 규칙 위반 시
 */
public class BusinessRuleViolationException extends BusinessException {

	public BusinessRuleViolationException() {
		super(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	public BusinessRuleViolationException(String message) {
		super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
	}

	public BusinessRuleViolationException(ErrorCode errorCode) {
		super(errorCode);
	}

	public BusinessRuleViolationException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	@Override
	public boolean isNecessaryToLog() {
		return false;
	}
}
