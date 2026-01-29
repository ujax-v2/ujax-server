package com.ujax.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "400", "잘못된 요청 형식 입니다."),
	INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "401", "잘못된 파라미터 형식 입니다."),

	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 오류가 발생했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String title;
	private final String detail;
}
