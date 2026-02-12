package com.ujax.infrastructure.security.oauth2;

public interface OAuth2UserInfo {

	String getProviderId();

	String getEmail();

	String getName();

	String getProfileImageUrl();
}
