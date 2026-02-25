package com.ujax.infrastructure.web.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.user.UserService;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.application.user.dto.response.UserResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.user.dto.request.ProfileImageUploadRequest;
import com.ujax.infrastructure.web.user.dto.request.UserUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.success(userService.getUser(principal.getUserId()));
	}

	@PostMapping("/me/profile-image/presigned-url")
	public ApiResponse<PresignedUrlResponse> createProfileImagePresignedUrl(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody ProfileImageUploadRequest request
	) {
		return ApiResponse.success(userService.createProfileImagePresignedUrl(principal.getUserId(), request));
	}

	@PatchMapping("/me")
	public ApiResponse<UserResponse> updateMe(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UserUpdateRequest request
	) {
		return ApiResponse.success(userService.updateUser(principal.getUserId(), request));
	}

	@DeleteMapping("/me")
	public ApiResponse<Void> deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
		userService.deleteUser(principal.getUserId());
		return ApiResponse.success();
	}
}
