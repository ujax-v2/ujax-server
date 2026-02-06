package com.ujax.infrastructure.web.user;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.user.UserService;
import com.ujax.application.user.dto.response.UserResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.web.user.dto.request.UserUpdateRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	// TODO: Security 적용 후 @AuthenticationPrincipal로 userId 받기
	@GetMapping("/me")
	public ApiResponse<UserResponse> getMe() {
		// TODO: Security 적용 전까지 임시로 userId 1 사용
		Long userId = 1L;
		return ApiResponse.success(userService.getUser(userId));
	}

	@PatchMapping("/me")
	public ApiResponse<UserResponse> updateMe(@Valid @RequestBody UserUpdateRequest request) {
		Long userId = 1L;
		return ApiResponse.success(userService.updateUser(userId, request.name(), request.profileImageUrl()));
	}

	@DeleteMapping("/me")
	public ApiResponse<Void> deleteMe() {
		Long userId = 1L;
		userService.deleteUser(userId);
		return ApiResponse.success();
	}
}
