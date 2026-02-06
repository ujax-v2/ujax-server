package com.ujax.infrastructure.web.user.dto.request;

public record UserUpdateRequest(

	String name,
	String profileImageUrl
) {
}
