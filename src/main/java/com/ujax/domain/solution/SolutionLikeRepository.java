package com.ujax.domain.solution;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolutionLikeRepository extends JpaRepository<SolutionLike, SolutionLikeId> {

	@Query("""
		SELECT sl.solution.id, COUNT(sl)
		FROM SolutionLike sl
		WHERE sl.solution.id IN :solutionIds
		AND sl.deleted = false
		GROUP BY sl.solution.id
		""")
	List<Object[]> countBySolutionIds(@Param("solutionIds") List<Long> solutionIds);

	@Query("""
		SELECT sl.solution.id
		FROM SolutionLike sl
		WHERE sl.solution.id IN :solutionIds
		AND sl.workspaceMember.id = :workspaceMemberId
		AND sl.deleted = false
		""")
	List<Long> findMyLikedSolutionIds(
		@Param("solutionIds") List<Long> solutionIds,
		@Param("workspaceMemberId") Long workspaceMemberId
	);

	@Modifying
	@Query("""
		UPDATE SolutionLike sl
		SET sl.deleted = :deleted
		WHERE sl.solution.id = :solutionId
		AND sl.workspaceMember.id = :workspaceMemberId
		""")
	int updateDeleted(
		@Param("solutionId") Long solutionId,
		@Param("workspaceMemberId") Long workspaceMemberId,
		@Param("deleted") boolean deleted
	);
}
