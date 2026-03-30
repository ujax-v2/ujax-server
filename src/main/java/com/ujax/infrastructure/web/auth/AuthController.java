package com.ujax.infrastructure.web.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.auth.AuthService;
import com.ujax.application.auth.dto.response.AuthTokenResponse;
import com.ujax.application.auth.dto.response.SignupStartResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.web.auth.dto.request.EmailAvailabilityRequest;
import com.ujax.infrastructure.web.auth.dto.request.LoginRequest;
import com.ujax.infrastructure.web.auth.dto.request.RefreshRequest;
import com.ujax.infrastructure.web.auth.dto.request.SignupConfirmRequest;
import com.ujax.infrastructure.web.auth.dto.request.SignupResendRequest;
import com.ujax.infrastructure.web.auth.dto.request.SignupStartRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/email-availability")
	public ApiResponse<Void> checkEmailAvailability(@Valid @RequestBody EmailAvailabilityRequest request) {
		authService.checkEmailAvailability(request.email());
		return ApiResponse.success();
	}

	@PostMapping("/signup/request")
	public ApiResponse<SignupStartResponse> requestSignup(@Valid @RequestBody SignupStartRequest request) {
		return ApiResponse.success(authService.requestSignup(request.email(), request.password(), request.name()));
	}

	@PostMapping("/signup/confirm")
	public ApiResponse<AuthTokenResponse> confirmSignup(@Valid @RequestBody SignupConfirmRequest request) {
		return ApiResponse.success(authService.confirmSignup(request.requestToken(), request.code()));
	}

	@PostMapping("/signup/resend")
	public ApiResponse<SignupStartResponse> resendSignupCode(@Valid @RequestBody SignupResendRequest request) {
		return ApiResponse.success(authService.resendSignupCode(request.requestToken()));
	}

	@PostMapping("/login")
	public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request.email(), request.password()));
	}

	@PostMapping("/refresh")
	public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ApiResponse.success(authService.refresh(request.refreshToken()));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
		authService.logout(request.refreshToken());
		return ApiResponse.success();
	}
}
