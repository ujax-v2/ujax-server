package com.ujax.domain.user;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT u FROM User u WHERE u.id = :id")
	Optional<User> findById(@Param("id") @NonNull Long id);

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
