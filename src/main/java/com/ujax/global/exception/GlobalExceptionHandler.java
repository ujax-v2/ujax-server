package com.ujax.global.exception;

import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * [Exception] 비즈니스/도메인 예외
	 */
	@ExceptionHandler(BusinessException.class)
	public ProblemDetail handle(BusinessException ex) {
		if (ex.isNecessaryToLog()) {
			log.error("[BusinessException] {}", ex.getMessage(), ex);
		}
		ErrorCode errorCode = ex.getErrorCode();

		ProblemDetail problemDetail = ProblemDetail.forStatus(errorCode.getHttpStatus());
		problemDetail.setType(URI.create("/docs/index.html#error-code-list"));
		problemDetail.setTitle(errorCode.getTitle());
		problemDetail.setDetail(ex.getMessage());
		problemDetail.setProperty("exception", ex.getClass().getSimpleName());
		problemDetail.setProperty("timestamp", LocalDateTime.now());

		log.warn("[BusinessException] code={}, message={}",
			errorCode.getTitle(), ex.getMessage(), ex);

		return problemDetail;
	}

	/**
	 * [Exception] @Valid, @RequestBody 검증 실패
	 * */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handle(MethodArgumentNotValidException ex) {
		return createProblemDetail(ex, ErrorCode.INVALID_INPUT);
	}

	/**
	 * [Exception] 예측하지 못한 모든 예외 처리
	 * */
	@ExceptionHandler(Exception.class)
	public ProblemDetail handle(Exception ex) {
		return createProblemDetail(ex, ErrorCode.INTERNAL_ERROR);
	}

	private ProblemDetail createProblemDetail(Exception ex, ErrorCode errorCode) {
		ProblemDetail problemDetail = ProblemDetail.forStatus(errorCode.getHttpStatus());
		problemDetail.setType(URI.create("/docs/index.html#error-code-list"));
		problemDetail.setTitle(errorCode.getTitle());
		problemDetail.setDetail(errorCode.getDetail());
		problemDetail.setProperty("exception", ex.getClass().getSimpleName());
		problemDetail.setProperty("timestamp", LocalDateTime.now());
		return problemDetail;
	}

}
