package com.ujax.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// ==================== 400 Bad Request ====================
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청 형식입니다."),
	INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "C002", "잘못된 파라미터 형식입니다."),
	MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "C003", "필수 입력값이 누락되었습니다."),
	INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입의 값입니다."),
	INVALID_SUBMISSION(HttpStatus.BAD_REQUEST, "C005", "유효하지 않은 제출 정보입니다."),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "C006", "비밀번호는 8자 이상이며, 영문과 숫자를 포함해야 합니다."),

	// ==================== 401 Unauthorized ====================
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A004", "이메일 또는 비밀번호가 올바르지 않습니다."),

	// ==================== 403 Forbidden ====================
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "A010", "접근 권한이 없습니다."),
	FORBIDDEN_RESOURCE(HttpStatus.FORBIDDEN, "A011", "해당 리소스에 대한 권한이 없습니다."),
	WORKSPACE_FORBIDDEN(HttpStatus.FORBIDDEN, "A012", "워크스페이스에 대한 권한이 없습니다."),
	WORKSPACE_MEMBER_FORBIDDEN(HttpStatus.FORBIDDEN, "A013", "워크스페이스에 소속된 멤버가 아닙니다."),
	WORKSPACE_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "A014", "소유자만 이 작업을 수행할 수 있습니다."),

	// ==================== 404 Not Found ====================
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "요청한 리소스를 찾을 수 없습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "R002", "사용자를 찾을 수 없습니다."),
	WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "R003", "워크스페이스를 찾을 수 없습니다."),
	WORKSPACE_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "R004", "워크스페이스 멤버를 찾을 수 없습니다."),
	BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "R007", "게시글을 찾을 수 없습니다."),
	BOARD_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "R008", "댓글을 찾을 수 없습니다."),
	PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "R005", "문제를 찾을 수 없습니다."),

	// ==================== 409 Conflict ====================
	DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "D001", "이미 존재하는 리소스입니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "D002", "이미 사용 중인 이메일입니다."),
	RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "D003", "리소스가 이미 존재합니다."),
	ALREADY_WORKSPACE_MEMBER(HttpStatus.CONFLICT, "D004", "이미 워크스페이스에 가입된 멤버입니다."),
	DUPLICATE_PROBLEM(HttpStatus.CONFLICT, "D005", "이미 등록된 문제 번호입니다."),
	WORKSPACE_NAME_DUPLICATE(HttpStatus.CONFLICT, "D006", "이미 존재하는 워크스페이스 이름입니다."),
	OAUTH_ACCOUNT_EXISTS(HttpStatus.CONFLICT, "D008", "이미 다른 소셜 계정으로 가입된 이메일입니다."),

	// ==================== 422 Unprocessable Entity ====================
	UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "U001", "요청을 처리할 수 없습니다."),
	BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "U002", "비즈니스 규칙 위반입니다."),
	WORKSPACE_OWNER_CANNOT_WITHDRAW(HttpStatus.UNPROCESSABLE_ENTITY, "U003", "워크스페이스 소유자는 탈퇴할 수 없습니다. 소유권을 양도하거나 워크스페이스를 삭제해 주세요."),

	// ==================== 429 Too Many Requests ====================
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "T001", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

	// ==================== 500 Internal Server Error ====================
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 오류가 발생했습니다."),
	DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S002", "데이터베이스 오류가 발생했습니다."),

	// ==================== 502 Bad Gateway ====================
	EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "E001", "외부 서비스 연동 중 오류가 발생했습니다."),
	JUDGE0_API_ERROR(HttpStatus.BAD_GATEWAY, "E003", "Judge0 서비스 연동 중 오류가 발생했습니다."),

	// ==================== 503 Service Unavailable ====================
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "E002", "서비스를 일시적으로 사용할 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String code;
	private final String detail;
}
