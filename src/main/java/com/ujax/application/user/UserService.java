package com.ujax.application.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.application.user.dto.response.UserResponse;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BusinessRuleViolationException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.external.s3.dto.PresignedUrlResult;
import com.ujax.infrastructure.external.s3.S3StorageService;
import com.ujax.infrastructure.web.user.dto.request.ProfileImageUploadRequest;
import com.ujax.infrastructure.web.user.dto.request.UserUpdateRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final S3StorageService s3StorageService;

	public UserResponse getUser(Long userId) {
		return UserResponse.from(findUserById(userId));
	}

	public PresignedUrlResponse createProfileImagePresignedUrl(Long userId, ProfileImageUploadRequest request) {
		PresignedUrlResult result =
			s3StorageService.generatePresignedUrl(userId, request.contentType(), request.fileSize());
		return new PresignedUrlResponse(result.presignedUrl(), result.imageUrl());
	}

	@Transactional
	public UserResponse updateUser(Long userId, UserUpdateRequest request) {
		User user = findUserById(userId);

		if (request.profileImageUrl() != null) {
			s3StorageService.deleteByUrl(user.getProfileImageUrl());
		}
		user.updateProfile(request.name(), request.profileImageUrl(), request.baekjoonId());
		return UserResponse.from(user);
	}

	@Transactional
	public void deleteUser(Long userId) {
		User user = findUserById(userId);
		if (workspaceMemberRepository.existsByUser_IdAndRole(userId, WorkspaceMemberRole.OWNER)) {
			throw new BusinessRuleViolationException(ErrorCode.WORKSPACE_OWNER_CANNOT_WITHDRAW);
		}
		userRepository.delete(user);
	}

	private User findUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
	}
}
