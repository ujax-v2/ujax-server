package com.ujax.global.exception;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String ERROR_DOC_URI = "/docs/index.html#error-code-list";

	/**
	 * [Exception] 비즈니스/도메인 예외
	 */
	@ExceptionHandler(BusinessException.class)
	public ProblemDetail handle(BusinessException ex) {
		ErrorCode errorCode = ex.getErrorCode();

		if (ex.isNecessaryToLog()) {
			log.error("[BusinessException] code={}, message={}", errorCode.getCode(), ex.getMessage(), ex);
		} else {
			log.warn("[BusinessException] code={}, message={}", errorCode.getCode(), ex.getMessage());
		}

		return createProblemDetail(
			errorCode.getHttpStatus(),
			errorCode.getCode(),
			ex.getMessage(),
			ex.getClass().getSimpleName()
		);
	}

	/**
	 * [Exception] @Valid, @RequestBody 검증 실패
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handle(MethodArgumentNotValidException ex) {
		List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new FieldErrorDetail(
				error.getField(),
				error.getRejectedValue(),
				error.getDefaultMessage()
			))
			.collect(Collectors.toList());

		log.warn("[ValidationException] fieldErrors={}", fieldErrors);

		ProblemDetail problemDetail = createProblemDetail(
			HttpStatus.BAD_REQUEST,
			ErrorCode.INVALID_INPUT.getCode(),
			ErrorCode.INVALID_INPUT.getDetail(),
			ex.getClass().getSimpleName()
		);
		problemDetail.setProperty("fieldErrors", fieldErrors);

		return problemDetail;
	}

	/**
	 * [Exception] @Validated 검증 실패 (Query Parameter, Path Variable)
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handle(ConstraintViolationException ex) {
		List<String> violations = ex.getConstraintViolations().stream()
			.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
			.collect(Collectors.toList());

		log.warn("[ConstraintViolationException] violations={}", violations);

		ProblemDetail problemDetail = createProblemDetail(
			HttpStatus.BAD_REQUEST,
			ErrorCode.INVALID_PARAMETER.getCode(),
			ErrorCode.INVALID_PARAMETER.getDetail(),
			ex.getClass().getSimpleName()
		);
		problemDetail.setProperty("violations", violations);

		return problemDetail;
	}

	/**
	 * [Exception] 요청 파라미터 누락
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ProblemDetail handle(MissingServletRequestParameterException ex) {
		String message = String.format("필수 파라미터 '%s'이(가) 누락되었습니다.", ex.getParameterName());
		log.warn("[MissingParameterException] {}", message);

		return createProblemDetail(
			HttpStatus.BAD_REQUEST,
			ErrorCode.MISSING_REQUIRED_FIELD.getCode(),
			message,
			ex.getClass().getSimpleName()
		);
	}

	/**
	 * [Exception] 타입 변환 실패
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handle(MethodArgumentTypeMismatchException ex) {
		String message = String.format("파라미터 '%s'의 타입이 올바르지 않습니다. (기대 타입: %s)",
			ex.getName(),
			ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
		);
		log.warn("[TypeMismatchException] {}", message);

		return createProblemDetail(
			HttpStatus.BAD_REQUEST,
			ErrorCode.INVALID_TYPE_VALUE.getCode(),
			message,
			ex.getClass().getSimpleName()
		);
	}

	/**
	 * [Exception] JSON 파싱 실패
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handle(HttpMessageNotReadableException ex) {
		log.warn("[HttpMessageNotReadableException] {}", ex.getMessage());

		return createProblemDetail(
			HttpStatus.BAD_REQUEST,
			ErrorCode.INVALID_INPUT.getCode(),
			"요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요.",
			ex.getClass().getSimpleName()
		);
	}

	/**
	 * [Exception] 지원하지 않는 HTTP 메서드
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ProblemDetail handle(HttpRequestMethodNotSupportedException ex) {
		String message = String.format("지원하지 않는 HTTP 메서드입니다: %s", ex.getMethod());
		log.warn("[MethodNotSupportedException] {}", message);

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
		problemDetail.setType(URI.create(ERROR_DOC_URI));
		problemDetail.setTitle("METHOD_NOT_ALLOWED");
		problemDetail.setDetail(message);
		problemDetail.setProperty("exception", ex.getClass().getSimpleName());
		problemDetail.setProperty("timestamp", LocalDateTime.now());

		return problemDetail;
	}

	/**
	 * [Exception] 지원하지 않는 Media Type
	 */
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ProblemDetail handle(HttpMediaTypeNotSupportedException ex) {
		String message = String.format("지원하지 않는 Content-Type입니다: %s", ex.getContentType());
		log.warn("[MediaTypeNotSupportedException] {}", message);

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		problemDetail.setType(URI.create(ERROR_DOC_URI));
		problemDetail.setTitle("UNSUPPORTED_MEDIA_TYPE");
		problemDetail.setDetail(message);
		problemDetail.setProperty("exception", ex.getClass().getSimpleName());
		problemDetail.setProperty("timestamp", LocalDateTime.now());

		return problemDetail;
	}

	/**
	 * [Exception] 핸들러를 찾을 수 없음 (404)
	 */
	@ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
	public ProblemDetail handleNotFound(Exception ex) {
		log.warn("[NotFoundException] {}", ex.getMessage());

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problemDetail.setType(URI.create(ERROR_DOC_URI));
		problemDetail.setTitle("NOT_FOUND");
		problemDetail.setDetail("요청한 리소스를 찾을 수 없습니다.");
		problemDetail.setProperty("exception", ex.getClass().getSimpleName());
		problemDetail.setProperty("timestamp", LocalDateTime.now());

		return problemDetail;
	}

	/**
	 * [Exception] 예측하지 못한 모든 예외 처리
	 */
	@ExceptionHandler(Exception.class)
	public ProblemDetail handle(Exception ex) {
		log.error("[UnhandledException] ", ex);

		return createProblemDetail(
			HttpStatus.INTERNAL_SERVER_ERROR,
			ErrorCode.INTERNAL_ERROR.getCode(),
			ErrorCode.INTERNAL_ERROR.getDetail(),
			ex.getClass().getSimpleName()
		);
	}

	private ProblemDetail createProblemDetail(HttpStatus status, String code, String detail, String exceptionName) {
		ProblemDetail problemDetail = ProblemDetail.forStatus(status);
		problemDetail.setType(URI.create(ERROR_DOC_URI));
		problemDetail.setTitle(code);
		problemDetail.setDetail(detail);
		problemDetail.setProperty("exception", exceptionName);
		problemDetail.setProperty("timestamp", LocalDateTime.now());
		return problemDetail;
	}

	/**
	 * 필드 에러 상세 정보
	 */
	public record FieldErrorDetail(
		String field,
		Object rejectedValue,
		String message
	) {
	}
}
