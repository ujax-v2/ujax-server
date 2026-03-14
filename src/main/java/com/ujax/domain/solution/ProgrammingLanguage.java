package com.ujax.domain.solution;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProgrammingLanguage {

	C("C", 50),
	CPP("C++", 54),
	JAVA("Java", 62),
	PYTHON("Python", 71),
	KOTLIN("Kotlin", 78),
	SWIFT("Swift", 83),
	CSHARP("C#", 51),
	JAVASCRIPT("JavaScript", 63),
	GO("Go", 60),
	RUST("Rust", 73),
	RUBY("Ruby", 72),
	OTHER("기타", null)

	;

	private final String text;
	private final Integer judge0Id;

	public static ProgrammingLanguage fromSubmissionLanguage(String language) {
		if (language == null || language.isBlank()) {
			return OTHER;
		}
		return findByEnumName(language.trim().toUpperCase(Locale.ROOT));
	}

	public static ProgrammingLanguage fromLanguage(String language) {
		if (language == null || language.isBlank()) {
			return OTHER;
		}
		return switch (normalize(language)) {
			case "C++" -> CPP;
			case "C#" -> CSHARP;
			case "C" -> C;
			case "Java" -> JAVA;
			case "Python", "PyPy" -> PYTHON;
			case "Kotlin" -> KOTLIN;
			case "Swift" -> SWIFT;
			case "JavaScript", "Node.js" -> JAVASCRIPT;
			case "Go" -> GO;
			case "Rust" -> RUST;
			case "Ruby" -> RUBY;
			default -> OTHER;
		};
	}

	/**
	 * 첫 토큰에서 뒤쪽 숫자 제거 (예: "C++17" → "C++", "PyPy3" → "PyPy", "C99" → "C")
	 * */
	private static String normalize(String language) {
		String token = language.split(" ")[0];
		return token.replaceAll("\\d+$", "");
	}

	private static ProgrammingLanguage findByEnumName(String enumName) {
		for (ProgrammingLanguage programmingLanguage : values()) {
			if (programmingLanguage.name().equals(enumName)) {
				return programmingLanguage;
			}
		}
		return OTHER;
	}

	public boolean supportsJudge0() {
		return judge0Id != null;
	}
}
