package com.ujax.application.user.dto.response;

import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.User;

public record UserResponse(

	Long id,
	String email,
	String name,
	String profileImageUrl,
	AuthProvider provider
) {

	public static UserResponse from(User user) {
		return new UserResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getProfileImageUrl(),
			user.getProvider()
		);
	}
}
