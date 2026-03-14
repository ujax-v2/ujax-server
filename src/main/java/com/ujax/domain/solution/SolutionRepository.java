package com.ujax.domain.solution;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolutionRepository extends JpaRepository<Solution, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT s FROM Solution s WHERE s.id = :id")
	Optional<Solution> findById(@Param("id") @NonNull Long id);

	boolean existsBySubmissionId(Long submissionId);

	@Query(value = "SELECT s FROM Solution s WHERE s.workspaceProblem.id = :wpId",
		countQuery = "SELECT count(s) FROM Solution s WHERE s.workspaceProblem.id = :wpId")
	Page<Solution> findByWorkspaceProblemId(@Param("wpId") Long workspaceProblemId, Pageable pageable);
}
