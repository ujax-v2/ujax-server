package com.ujax.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {

	GOOGLE("구글 회원가입"),
	KAKAO("카카오 회원가입"),
	LOCAL("자체 회원가입")

	;

	private final String text;
}
