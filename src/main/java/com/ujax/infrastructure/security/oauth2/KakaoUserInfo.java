package com.ujax.infrastructure.security.oauth2;

import java.util.Map;

/**
 * @SuppressWarnings("unchecked") — 컴파일러의 "unchecked cast" 경고를 억제한다.
 * @see <a href="https://docs.oracle.com/javase/tutorial/java/generics/erasure.html">Type Erasure (Oracle Docs)</a>
 */
public class KakaoUserInfo implements OAuth2UserInfo {

	private final Map<String, Object> attributes;

	public KakaoUserInfo(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String getProviderId() {
		return String.valueOf(attributes.get("id"));
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getEmail() {
		Map<String, Object> kakaoAccount = (Map<String, Object>)attributes.get("kakao_account");
		if (kakaoAccount == null) {
			return null;
		}
		return (String)kakaoAccount.get("email");
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getName() {
		Map<String, Object> kakaoAccount = (Map<String, Object>)attributes.get("kakao_account");
		if (kakaoAccount == null) {
			return null;
		}
		Map<String, Object> profile = (Map<String, Object>)kakaoAccount.get("profile");
		if (profile == null) {
			return null;
		}
		return (String)profile.get("nickname");
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getProfileImageUrl() {
		Map<String, Object> kakaoAccount = (Map<String, Object>)attributes.get("kakao_account");
		if (kakaoAccount == null) {
			return null;
		}
		Map<String, Object> profile = (Map<String, Object>)kakaoAccount.get("profile");
		if (profile == null) {
			return null;
		}
		return (String)profile.get("profile_image_url");
	}
}
