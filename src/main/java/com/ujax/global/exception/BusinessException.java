package com.ujax.global.exception;

import lombok.Getter;

@Getter
public abstract class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	private final Object[] args;

	protected BusinessException(ErrorCode errorCode) {
		super(errorCode.getDetail());
		this.errorCode = errorCode;
		this.args = new Object[]{};
	}

	protected BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.args = new Object[]{};
	}

	/**
	 * 동적 메시지 포맷팅을 위한 생성자
	 * 예: new SomeException(ErrorCode.USER_NOT_FOUND, "userId", 123)
	 */
	protected BusinessException(ErrorCode errorCode, Object... args) {
		super(formatMessage(errorCode.getDetail(), args));
		this.errorCode = errorCode;
		this.args = args;
	}

	private static String formatMessage(String template, Object... args) {
		if (args == null || args.length == 0) {
			return template;
		}
		try {
			return String.format(template.replace("{}", "%s"), args);
		} catch (Exception e) {
			return template;
		}
	}

	/**
	 * 이 예외를 로그로 남길지 여부
	 */
	public abstract boolean isNecessaryToLog();

	/**
	 * HTTP 상태 코드 반환
	 */
	public int getHttpStatus() {
		return errorCode.getHttpStatus().value();
	}

	/**
	 * 에러 코드 문자열 반환
	 */
	public String getCode() {
		return errorCode.getCode();
	}
}
