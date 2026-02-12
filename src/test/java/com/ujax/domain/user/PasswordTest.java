package com.ujax.domain.user;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ujax.global.exception.common.BadRequestException;

class PasswordTest {

	private final PasswordEncoder encoder = new BCryptPasswordEncoder();

	@Nested
	@DisplayName("비밀번호 생성")
	class Encode {

		@Test
		@DisplayName("유효한 비밀번호로 생성한다")
		void encode_Success() {
			// when
			Password password = Password.encode("password1", encoder);

			// then
			assertThat(password.getEncodedValue()).isNotNull();
		}

		@Test
		@DisplayName("8자 미만이면 오류가 발생한다")
		void encode_TooShort() {
			// when & then
			assertThatThrownBy(() -> Password.encode("pass1", encoder))
				.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("영문이 없으면 오류가 발생한다")
		void encode_NoLetter() {
			// when & then
			assertThatThrownBy(() -> Password.encode("12345678", encoder))
				.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("숫자가 없으면 오류가 발생한다")
		void encode_NoDigit() {
			// when & then
			assertThatThrownBy(() -> Password.encode("abcdefgh", encoder))
				.isInstanceOf(BadRequestException.class);
		}
	}

	@Test
	@DisplayName("원본 비밀번호와 인코딩된 비밀번호가 일치한다")
	void matches() {
		// given
		String raw = "password1";
		Password password = Password.encode(raw, encoder);

		// when & then
		assertThat(password.matches(raw, encoder)).isTrue();
		assertThat(password.matches("wrongpass1", encoder)).isFalse();
	}
}
