package com.ujax.domain.user;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ujax.domain.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "users",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_users_provider_provider_id",
			columnNames = {"provider", "provider_id"}
		)
	}
)
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	private static final String DEFAULT_PROFILE_IMAGE_URL =
		"https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	/** 비밀번호 (OAuth 사용 시 null) */
	@Embedded
	private Password password;

	@Column(nullable = false, length = 30)
	private String name;

	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthProvider provider;

	/** OAuth 식별자 (자체 회원가입시 null) */
	@Column(name = "provider_id")
	private String providerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private UserRole role;

	/** 백준 아이디 (연동 전 null) */
	private String baekjoonId;

	@Builder
	private User(String email, Password password, String name, String profileImageUrl,
		AuthProvider provider, String providerId, UserRole role) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.profileImageUrl = profileImageUrl != null ? profileImageUrl : DEFAULT_PROFILE_IMAGE_URL;
		this.provider = provider;
		this.providerId = providerId;
		this.role = role != null ? role : UserRole.USER;
	}

	public static User createOAuthUser(String email, String name, String profileImageUrl,
		AuthProvider provider, String providerId) {
		return User.builder()
			.email(email)
			.name(name)
			.profileImageUrl(profileImageUrl)
			.provider(provider)
			.providerId(providerId)
			.role(UserRole.USER)
			.build();
	}

	public static User createLocalUser(String email, Password password, String name) {
		return User.builder()
			.email(email)
			.password(password)
			.name(name)
			.provider(AuthProvider.LOCAL)
			.role(UserRole.USER)
			.build();
	}

	public boolean matchesPassword(String rawPassword, PasswordEncoder encoder) {
		return password != null && password.matches(rawPassword, encoder);
	}

	public void updateProfile(String name, String profileImageUrl, String baekjoonId) {
		if (name != null) {
			this.name = name;
		}
		if (profileImageUrl != null) {
			this.profileImageUrl = profileImageUrl;
		}
		if (baekjoonId != null) {
			this.baekjoonId = baekjoonId;
		}
	}

	public void updateProfileImage(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}
}
