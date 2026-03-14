package com.ujax.domain.solution;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SolutionStatus {

	ACCEPTED("맞았습니다!!"),
	WRONG_ANSWER("틀렸습니다"),
	TIME_LIMIT_EXCEEDED("시간 초과"),
	MEMORY_LIMIT_EXCEEDED("메모리 초과"),
	OUTPUT_LIMIT_EXCEEDED("출력 초과"),
	RUNTIME_ERROR("런타임 에러"),
	COMPILE_ERROR("컴파일 에러"),
	PRESENTATION_ERROR("출력 형식이 잘못되었습니다"),
	PARTIAL_ACCEPTED("부분 성공"),
	OTHER("기타")

	;

	private final String text;

	public static SolutionStatus fromVerdict(String verdict) {
		if (verdict == null) {
			return OTHER;
		}
		for (SolutionStatus status : values()) {
			if (status.text.equals(verdict)) {
				return status;
			}
		}
		return OTHER;
	}

	public boolean isAccepted() {
		return this == ACCEPTED;
	}
}
