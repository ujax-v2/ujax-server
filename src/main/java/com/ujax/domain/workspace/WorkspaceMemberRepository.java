package com.ujax.domain.workspace;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT wm FROM WorkspaceMember wm WHERE wm.id = :id")
	Optional<WorkspaceMember> findById(@Param("id") @NonNull Long id);

	Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(Long workspaceId, Long userId);

	@Query(
		value = "SELECT * FROM workspace_members WHERE workspace_id = :workspaceId AND user_id = :userId",
		nativeQuery = true
	)
	Optional<WorkspaceMember> findByWorkspaceIdAndUserIdIncludingDeleted(
		@Param("workspaceId") Long workspaceId,
		@Param("userId") Long userId
	);

	Optional<WorkspaceMember> findByWorkspace_IdAndId(Long workspaceId, Long workspaceMemberId);

	List<WorkspaceMember> findByWorkspace_Id(Long workspaceId);

	@Query(
		value = """
			SELECT wm FROM WorkspaceMember wm
			JOIN FETCH wm.user u
			WHERE wm.workspace.id = :workspaceId
			ORDER BY
				CASE
					WHEN wm.role = com.ujax.domain.workspace.WorkspaceMemberRole.OWNER THEN 0
					WHEN wm.role = com.ujax.domain.workspace.WorkspaceMemberRole.MANAGER THEN 1
					ELSE 2
				END,
				wm.createdAt ASC,
				wm.id ASC
		""",
		countQuery = "SELECT COUNT(wm) FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId"
	)
	Page<WorkspaceMember> findByWorkspace_Id(@Param("workspaceId") Long workspaceId, Pageable pageable);

	boolean existsByUser_IdAndRole(Long userId, WorkspaceMemberRole role);
}
