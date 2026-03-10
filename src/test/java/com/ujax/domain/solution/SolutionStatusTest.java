package com.ujax.domain.solution;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SolutionStatusTest {

	@ParameterizedTest
	@CsvSource({
		"맞았습니다!!, ACCEPTED",
		"틀렸습니다, WRONG_ANSWER",
		"시간 초과, TIME_LIMIT_EXCEEDED",
		"메모리 초과, MEMORY_LIMIT_EXCEEDED",
		"출력 초과, OUTPUT_LIMIT_EXCEEDED",
		"런타임 에러, RUNTIME_ERROR",
		"컴파일 에러, COMPILE_ERROR",
		"출력 형식이 잘못되었습니다, PRESENTATION_ERROR",
		"부분 성공, PARTIAL_ACCEPTED"
	})
	@DisplayName("백준 verdict 문자열로 SolutionStatus를 매핑한다")
	void fromVerdict(String verdict, SolutionStatus expected) {
		assertThat(SolutionStatus.fromVerdict(verdict)).isEqualTo(expected);
	}

	@Nested
	@DisplayName("fromVerdict가 OTHER를 반환하는 경우")
	class FromVerdictOther {

		@Test
		@DisplayName("null이면 OTHER를 반환한다")
		void nullVerdict() {
			assertThat(SolutionStatus.fromVerdict(null)).isEqualTo(SolutionStatus.OTHER);
		}

		@Test
		@DisplayName("알 수 없는 문자열이면 OTHER를 반환한다")
		void unknownVerdict() {
			assertThat(SolutionStatus.fromVerdict("알 수 없는 결과")).isEqualTo(SolutionStatus.OTHER);
		}
	}

	@Test
	@DisplayName("ACCEPTED만 isAccepted가 true이다")
	void isAccepted() {
		assertThat(SolutionStatus.ACCEPTED.isAccepted()).isTrue();
		assertThat(SolutionStatus.WRONG_ANSWER.isAccepted()).isFalse();
		assertThat(SolutionStatus.OTHER.isAccepted()).isFalse();
	}
}
