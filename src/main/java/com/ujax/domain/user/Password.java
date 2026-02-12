package com.ujax.domain.user;

import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Password {

	private static final int MIN_LENGTH = 8;
	private static final Pattern HAS_LETTER = Pattern.compile("[a-zA-Z]");
	private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");

	@Column(name = "password")
	private String encodedValue;

	private Password(String encodedValue) {
		this.encodedValue = encodedValue;
	}

	public static Password ofEncoded(String encodedValue) {
		return new Password(encodedValue);
	}

	public static Password encode(String rawPassword, PasswordEncoder encoder) {
		validate(rawPassword);
		return new Password(encoder.encode(rawPassword));
	}

	public boolean matches(String rawPassword, PasswordEncoder encoder) {
		return encoder.matches(rawPassword, encodedValue);
	}

	private static void validate(String rawPassword) {
		if (rawPassword.length() < MIN_LENGTH
			|| !HAS_LETTER.matcher(rawPassword).find()
			|| !HAS_DIGIT.matcher(rawPassword).find()) {
			throw new BadRequestException(ErrorCode.INVALID_PASSWORD);
		}
	}
}
