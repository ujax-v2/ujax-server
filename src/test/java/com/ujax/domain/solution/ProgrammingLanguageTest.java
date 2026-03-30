package com.ujax.domain.solution;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

class ProgrammingLanguageTest {

	@ParameterizedTest
	@CsvSource({
		"JAVA, JAVA",
		"python, PYTHON",
		"' cpp ', CPP",
		"javascript, JAVASCRIPT",
		"basic, OTHER"
	})
	@DisplayName("제출 요청 언어 문자열로 ProgrammingLanguage를 매핑한다")
	void fromSubmissionLanguage(String language, ProgrammingLanguage expected) {
		assertThat(ProgrammingLanguage.fromSubmissionLanguage(language)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({
		"C++17, CPP",
		"'C++ (Clang)', CPP",
		"C++14, CPP",
		"C11, C",
		"C99, C",
		"C, C",
		"Java 11, JAVA",
		"Java 8, JAVA",
		"Python 3, PYTHON",
		"PyPy3, PYTHON",
		"Kotlin, KOTLIN",
		"Kotlin (JVM), KOTLIN",
		"Swift, SWIFT",
		"'C#', CSHARP",
		"JavaScript, JAVASCRIPT",
		"Node.js, JAVASCRIPT",
		"Go, GO",
		"Rust 2021, RUST",
		"Ruby, RUBY"
	})
	@DisplayName("백준 언어 문자열로 ProgrammingLanguage를 매핑한다")
	void fromLanguage(String language, ProgrammingLanguage expected) {
		assertThat(ProgrammingLanguage.fromLanguage(language)).isEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource("baekjoonModifiedLanguages")
	@DisplayName("백준 수정 표기와 NBSP가 섞인 언어 문자열도 매핑한다")
	void fromLanguageWithModifiedSuffix(String language, ProgrammingLanguage expected) {
		assertThat(ProgrammingLanguage.fromLanguage(language)).isEqualTo(expected);
	}

	@Nested
	@DisplayName("fromLanguage가 OTHER를 반환하는 경우")
	class FromLanguageOther {

		@Test
		@DisplayName("null이면 OTHER를 반환한다")
		void nullLanguage() {
			assertThat(ProgrammingLanguage.fromLanguage(null)).isEqualTo(ProgrammingLanguage.OTHER);
		}

		@Test
		@DisplayName("빈 문자열이면 OTHER를 반환한다")
		void blankLanguage() {
			assertThat(ProgrammingLanguage.fromLanguage("")).isEqualTo(ProgrammingLanguage.OTHER);
		}

		@Test
		@DisplayName("알 수 없는 문자열이면 OTHER를 반환한다")
		void unknownLanguage() {
			assertThat(ProgrammingLanguage.fromLanguage("Haskell")).isEqualTo(ProgrammingLanguage.OTHER);
		}
	}

	private static Stream<Arguments> baekjoonModifiedLanguages() {
		return Stream.of(
			Arguments.of("C99\u00A0/\u00A0수정", ProgrammingLanguage.C),
			Arguments.of("C++17\u00A0/\u00A0수정", ProgrammingLanguage.CPP),
			Arguments.of("C# 9.0\u00A0/\u00A0수정", ProgrammingLanguage.CSHARP),
			Arguments.of("node.js\u00A0/\u00A0수정", ProgrammingLanguage.JAVASCRIPT)
		);
	}
}
