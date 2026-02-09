package com.ujax.domain.problem;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT p FROM Problem p WHERE p.id = :id")
	Optional<Problem> findById(@Param("id") @NonNull Long id);

	Optional<Problem> findByProblemNumber(int problemNumber);

	boolean existsByProblemNumber(int problemNumber);
}
