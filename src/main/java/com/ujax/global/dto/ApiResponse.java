package com.ujax.global.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

	private final boolean success;
	private final T data;
	private final String message;

	public static <T> ApiResponse<T>
	success(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(true, data, message);
	}

	public static ApiResponse<Void> success() {
		return new ApiResponse<>(true, null, null);
	}

	public static ApiResponse<Void> successWithMessage(String message) {
		return new ApiResponse<>(true, null, message);
	}
}
