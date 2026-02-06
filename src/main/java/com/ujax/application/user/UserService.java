package com.ujax.application.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.user.dto.response.UserResponse;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public UserResponse getUser(Long userId) {
		return UserResponse.from(findUserById(userId));
	}

	@Transactional
	public UserResponse updateUser(Long userId, String name, String profileImageUrl) {
		User user = findUserById(userId);
		user.updateProfile(name, profileImageUrl);
		return UserResponse.from(user);
	}

	@Transactional
	public void deleteUser(Long userId) {
		User user = findUserById(userId);
		userRepository.delete(user);
	}

	private User findUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
	}
}
