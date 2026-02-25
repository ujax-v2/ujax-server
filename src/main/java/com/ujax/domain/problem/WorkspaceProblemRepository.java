package com.ujax.domain.problem;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceProblemRepository extends JpaRepository<WorkspaceProblem, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT wp FROM WorkspaceProblem wp WHERE wp.id = :id")
	Optional<WorkspaceProblem> findById(@Param("id") @NonNull Long id);

	@Query("SELECT wp.problemBox.id, MAX(wp.createdAt) FROM WorkspaceProblem wp " +
		"WHERE wp.problemBox.id IN :problemBoxIds GROUP BY wp.problemBox.id")
	List<Object[]> findLatestCreatedAtByProblemBoxIds(@Param("problemBoxIds") List<Long> problemBoxIds);
}
