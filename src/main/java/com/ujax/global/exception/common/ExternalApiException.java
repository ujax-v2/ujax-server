package com.ujax.global.exception.common;

import com.ujax.global.exception.BusinessException;
import com.ujax.global.exception.ErrorCode;

/**
 * 502 Bad Gateway - 외부 API 호출 실패 시
 */
public class ExternalApiException extends BusinessException {

	private final String serviceName;

	public ExternalApiException() {
		super(ErrorCode.EXTERNAL_API_ERROR);
		this.serviceName = "Unknown";
	}

	public ExternalApiException(String serviceName) {
		super(ErrorCode.EXTERNAL_API_ERROR, serviceName + " 서비스 연동 중 오류가 발생했습니다.");
		this.serviceName = serviceName;
	}

	public ExternalApiException(String serviceName, String message) {
		super(ErrorCode.EXTERNAL_API_ERROR, message);
		this.serviceName = serviceName;
	}

	public ExternalApiException(String serviceName, String message, Throwable cause) {
		super(ErrorCode.EXTERNAL_API_ERROR, message);
		this.serviceName = serviceName;
		initCause(cause);
	}

	public ExternalApiException(ErrorCode errorCode, String serviceName, String message) {
		super(errorCode, message);
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	@Override
	public boolean isNecessaryToLog() {
		return true;
	}
}
