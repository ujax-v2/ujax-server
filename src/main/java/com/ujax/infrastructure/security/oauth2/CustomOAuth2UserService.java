package com.ujax.infrastructure.security.oauth2;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.ujax.application.auth.AuthService;
import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.User;
import com.ujax.infrastructure.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final AuthService authService;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
		OAuth2UserInfo userInfo = extractUserInfo(registrationId, oAuth2User);

		try {
			User user = authService.findOrCreateOAuthUser(
				userInfo.getEmail(), userInfo.getName(), userInfo.getProfileImageUrl(),
				provider, userInfo.getProviderId()
			);
			return UserPrincipal.fromUser(user, oAuth2User.getAttributes());
		} catch (Exception e) {
			throw new OAuth2AuthenticationException(
				new OAuth2Error("oauth_error", e.getMessage(), null), e
			);
		}
	}

	private OAuth2UserInfo extractUserInfo(String registrationId, OAuth2User oAuth2User) {
		return switch (registrationId.toLowerCase()) {
			case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
			case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
			default -> throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 프로바이더입니다: " + registrationId);
		};
	}
}
