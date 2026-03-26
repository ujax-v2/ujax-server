package com.ujax.domain.solution;

import java.time.LocalDateTime;
import java.util.List;
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

	@Query(value = """
		SELECT s
		FROM Solution s
		WHERE s.workspaceProblem.id = :workspaceProblemId
		AND s.workspaceMember.id = :workspaceMemberId
		""",
		countQuery = """
			SELECT count(s)
			FROM Solution s
			WHERE s.workspaceProblem.id = :workspaceProblemId
			AND s.workspaceMember.id = :workspaceMemberId
			""")
	Page<Solution> findByWorkspaceProblemIdAndWorkspaceMemberId(
		@Param("workspaceProblemId") Long workspaceProblemId,
		@Param("workspaceMemberId") Long workspaceMemberId,
		Pageable pageable
	);

	@Query("""
		SELECT s
		FROM Solution s
		JOIN FETCH s.workspaceMember wm
		WHERE s.workspaceProblem.id = :wpId
		ORDER BY s.createdAt DESC, s.id DESC
		""")
	List<Solution> findAllByWorkspaceProblemIdOrderByCreatedAtDescIdDesc(@Param("wpId") Long workspaceProblemId);

	Optional<Solution> findBySubmissionIdAndWorkspaceProblem_IdAndWorkspaceMember_Id(
		Long submissionId,
		Long workspaceProblemId,
		Long workspaceMemberId
	);

	@Query("""
		SELECT count(s)
		FROM Solution s
		WHERE s.workspaceProblem.problemBox.workspace.id = :workspaceId
		AND s.createdAt >= :start
		AND s.createdAt <= :end
		""")
	long countByWorkspaceIdAndCreatedAtBetween(
		@Param("workspaceId") Long workspaceId,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end
	);

	@Query("""
		SELECT s.workspaceProblem.id, count(s)
		FROM Solution s
		WHERE s.workspaceProblem.problemBox.workspace.id = :workspaceId
		AND s.createdAt >= :start
		AND s.createdAt <= :end
		GROUP BY s.workspaceProblem.id
		ORDER BY count(s) DESC, max(s.createdAt) DESC, s.workspaceProblem.id ASC
		""")
	List<Object[]> countByWorkspaceProblemBetween(
		@Param("workspaceId") Long workspaceId,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end
	);

	@Query("""
			SELECT s.workspaceMember.id, count(distinct s.workspaceProblem.id)
			FROM Solution s
			WHERE s.workspaceProblem.problemBox.workspace.id = :workspaceId
		AND s.status = :status
		AND s.createdAt >= :start
		AND s.createdAt <= :end
		GROUP BY s.workspaceMember.id
		""")
	List<Object[]> countSolvedProblemsByMemberBetween(
		@Param("workspaceId") Long workspaceId,
		@Param("status") SolutionStatus status,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end
	);

	@Query("""
		SELECT s.workspaceMember.id, count(distinct s.workspaceProblem.id)
		FROM Solution s
		WHERE s.workspaceProblem.problemBox.workspace.id = :workspaceId
		AND s.status = :status
		AND s.workspaceProblem.deadline IS NOT NULL
		AND s.workspaceProblem.deadline < :now
		AND s.createdAt <= s.workspaceProblem.deadline
		GROUP BY s.workspaceMember.id
		""")
	List<Object[]> countOnTimeSolvedProblemsByMember(
		@Param("workspaceId") Long workspaceId,
		@Param("status") SolutionStatus status,
		@Param("now") LocalDateTime now
	);

	@Query("""
		SELECT count(s)
		FROM Solution s
		WHERE s.workspaceMember.id = :workspaceMemberId
		""")
	long countByWorkspaceMemberId(@Param("workspaceMemberId") Long workspaceMemberId);

	@Query("""
		SELECT count(s)
		FROM Solution s
		WHERE s.workspaceMember.id = :workspaceMemberId
		AND s.status = :status
		""")
	long countByWorkspaceMemberIdAndStatus(
		@Param("workspaceMemberId") Long workspaceMemberId,
		@Param("status") SolutionStatus status
	);

	@Query("""
		SELECT count(distinct s.workspaceProblem.problem.id)
		FROM Solution s
		WHERE s.workspaceMember.id = :workspaceMemberId
		AND s.status = :status
		""")
	long countSolvedProblemsByWorkspaceMemberId(
		@Param("workspaceMemberId") Long workspaceMemberId,
		@Param("status") SolutionStatus status
	);

	@Query("""
		SELECT s.programmingLanguage, count(s)
		FROM Solution s
		WHERE s.workspaceMember.id = :workspaceMemberId
		GROUP BY s.programmingLanguage
		ORDER BY count(s) DESC, s.programmingLanguage ASC
		""")
	List<Object[]> countLanguageStatsByWorkspaceMemberId(@Param("workspaceMemberId") Long workspaceMemberId);

	@Query("""
		SELECT at.name, count(distinct s.workspaceProblem.problem.id)
		FROM Solution s
		JOIN s.workspaceProblem.problem p
		JOIN p.algorithmTags at
		WHERE s.workspaceMember.id = :workspaceMemberId
		AND s.status = :status
		GROUP BY at.name
		ORDER BY count(distinct s.workspaceProblem.problem.id) DESC, at.name ASC
		""")
	List<Object[]> countAlgorithmStatsByWorkspaceMemberId(
		@Param("workspaceMemberId") Long workspaceMemberId,
		@Param("status") SolutionStatus status
	);

	@Query("""
		SELECT FUNCTION('DATE', s.createdAt), count(distinct s.workspaceProblem.problem.id)
		FROM Solution s
		WHERE s.workspaceMember.id = :workspaceMemberId
		AND s.status = :status
		AND s.createdAt >= :start
		AND s.createdAt <= :end
		GROUP BY FUNCTION('DATE', s.createdAt)
		ORDER BY FUNCTION('DATE', s.createdAt) ASC
		""")
	List<Object[]> countSolvedActivityByWorkspaceMemberIdBetween(
		@Param("workspaceMemberId") Long workspaceMemberId,
		@Param("status") SolutionStatus status,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end
	);
}
