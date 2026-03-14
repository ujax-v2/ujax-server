package com.ujax.domain.problem;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceProblemRepository extends JpaRepository<WorkspaceProblem, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT wp FROM WorkspaceProblem wp WHERE wp.id = :id")
	Optional<WorkspaceProblem> findById(@Param("id") @NonNull Long id);

	Optional<WorkspaceProblem> findByIdAndProblemBox_Id(Long id, Long problemBoxId);

	Optional<WorkspaceProblem> findByIdAndProblemBox_IdAndProblemBox_Workspace_Id(
		Long id,
		Long problemBoxId,
		Long workspaceId
	);

	boolean existsByProblemBox_IdAndProblem_Id(Long problemBoxId, Long problemId);

	@Query(value = "SELECT wp FROM WorkspaceProblem wp JOIN FETCH wp.problem WHERE wp.problemBox.id = :problemBoxId",
		countQuery = "SELECT count(wp) FROM WorkspaceProblem wp WHERE wp.problemBox.id = :problemBoxId")
	Page<WorkspaceProblem> findByProblemBoxIdWithProblem(@Param("problemBoxId") Long problemBoxId, Pageable pageable);

	@Query("SELECT wp FROM WorkspaceProblem wp "
		+ "JOIN FETCH wp.problemBox pb "
		+ "JOIN FETCH pb.workspace "
		+ "JOIN FETCH wp.problem "
		+ "WHERE wp.id = :id")
	Optional<WorkspaceProblem> findByIdWithProblemBoxAndWorkspace(@Param("id") Long id);
}
