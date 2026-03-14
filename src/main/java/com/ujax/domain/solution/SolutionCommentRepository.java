package com.ujax.domain.solution;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ujax.infrastructure.persistence.jpa.IncludeDeleted;

public interface SolutionCommentRepository extends JpaRepository<SolutionComment, Long> {

	@Override
	@NonNull
	@Query("SELECT sc FROM SolutionComment sc WHERE sc.id = :id")
	Optional<SolutionComment> findById(@Param("id") @NonNull Long id);

	@EntityGraph(attributePaths = {"solution", "author"})
	@IncludeDeleted
	@Query("""
		SELECT sc FROM SolutionComment sc
		WHERE sc.solution.id = :solutionId
		AND sc.deletedAt IS NULL
		AND sc.solution.deletedAt IS NULL
		AND sc.solution.workspaceProblem.deletedAt IS NULL
		ORDER BY sc.createdAt ASC, sc.id ASC
		""")
	List<SolutionComment> findBySolutionId(@Param("solutionId") Long solutionId);

	@EntityGraph(attributePaths = {"solution", "author"})
	@IncludeDeleted
	@Query("""
		SELECT sc FROM SolutionComment sc
		WHERE sc.id = :commentId
		AND sc.solution.id = :solutionId
		AND sc.deletedAt IS NULL
		AND sc.solution.deletedAt IS NULL
		AND sc.solution.workspaceProblem.deletedAt IS NULL
		""")
	Optional<SolutionComment> findByIdAndSolutionId(
		@Param("commentId") Long commentId,
		@Param("solutionId") Long solutionId
	);

	@Query("""
		SELECT sc.solution.id, COUNT(sc)
		FROM SolutionComment sc
		WHERE sc.solution.id IN :solutionIds
		GROUP BY sc.solution.id
		""")
	List<Object[]> countBySolutionIds(@Param("solutionIds") List<Long> solutionIds);
}
