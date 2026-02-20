package com.ujax.domain.problem;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProgrammingLanguage {

	JAVA("Java"),
	PYTHON("Python 3"),
	C("C"),
	CPP("C++"),
	CSHARP("C#"),
	JAVASCRIPT("JavaScript"),
	KOTLIN("Kotlin"),
	SWIFT("Swift"),
	GO("Go"),
	RUST("Rust"),
	RUBY("Ruby")

	;

	private final String text;
}
