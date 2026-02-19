package com.ujax.infrastructure.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRole;

import io.jsonwebtoken.Claims;
import lombok.Getter;

@Getter
public class UserPrincipal implements OAuth2User {

	private final Long userId;
	private final UserRole role;
	private final String name;
	private final String email;
	private final Map<String, Object> attributes;

	private UserPrincipal(Long userId, UserRole role, String name, String email, Map<String, Object> attributes) {
		this.userId = userId;
		this.role = role;
		this.name = name;
		this.email = email;
		this.attributes = attributes;
	}

	public static UserPrincipal fromClaims(Claims claims) {
		return new UserPrincipal(
			Long.valueOf(claims.getSubject()),
			UserRole.valueOf(claims.get("role", String.class)),
			claims.get("name", String.class),
			claims.get("email", String.class),
			Collections.emptyMap()
		);
	}

	public static UserPrincipal fromUser(User user, Map<String, Object> attributes) {
		return new UserPrincipal(
			user.getId(),
			user.getRole(),
			user.getName(),
			user.getEmail(),
			attributes
		);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public String getName() {
		return name;
	}
}
