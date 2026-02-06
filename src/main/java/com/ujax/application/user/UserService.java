package com.ujax.application.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	public User getUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
	}

	@Transactional
	public User updateUser(Long userId, String name, String profileImageUrl) {
		User user = getUser(userId);
		user.updateProfile(name, profileImageUrl);
		return user;
	}

	@Transactional
	public void deleteUser(Long userId) {
		User user = getUser(userId);
		userRepository.delete(user);
	}
}
