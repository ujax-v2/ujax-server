package com.ujax.infrastructure.web.user;

import org.springframework.http.ResponseEntity;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.user.UserService;
import com.ujax.domain.user.User;
import com.ujax.application.user.dto.response.UserResponse;
import com.ujax.infrastructure.web.user.dto.request.UserUpdateRequest;

import lombok.RequiredArgsConstructor;

/**
 * 유저 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	/**
	 * 내 정보 조회
	 * TODO: Security 적용 후 @AuthenticationPrincipal로 userId 받기
	 */
	@GetMapping("/me")
	public ResponseEntity<UserResponse> getMe() {
		// TODO: Security 적용 전까지 임시로 userId 1 사용
		Long userId = 1L;
		User user = userService.getUser(userId);
		return ResponseEntity.ok(UserResponse.from(user));
	}

	/**
	 * 내 정보 수정
	 * TODO: Security 적용 후 @AuthenticationPrincipal로 userId 받기
	 */
	@PatchMapping("/me")
	public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UserUpdateRequest request) {
		// TODO: Security 적용 전까지 임시로 userId 1 사용
		Long userId = 1L;
		User user = userService.updateUser(userId, request.name(), request.profileImageUrl());
		return ResponseEntity.ok(UserResponse.from(user));
	}

	/**
	 * 회원 탈퇴
	 * TODO: Security 적용 후 @AuthenticationPrincipal로 userId 받기
	 */
	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteMe() {
		// TODO: Security 적용 전까지 임시로 userId 1 사용
		Long userId = 1L;
		userService.deleteUser(userId);
		return ResponseEntity.noContent().build();
	}
}
