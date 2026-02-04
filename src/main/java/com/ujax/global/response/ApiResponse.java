package com.ujax.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API 공통 응답 래퍼
 * 성공 응답에 사용
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	private final boolean success;
	private final T data;
	private final String message;

	/**
	 * 데이터와 함께 성공 응답
	 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null);
	}

	/**
	 * 데이터와 메시지 함께 성공 응답
	 */
	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(true, data, message);
	}

	/**
	 * 데이터 없이 성공 응답
	 */
	public static ApiResponse<Void> success() {
		return new ApiResponse<>(true, null, null);
	}

	/**
	 * 메시지만 포함된 성공 응답
	 */
	public static ApiResponse<Void> successWithMessage(String message) {
		return new ApiResponse<>(true, null, message);
	}
}
