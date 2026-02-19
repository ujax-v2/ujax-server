package com.ujax.domain.workspace;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

	/** findById를 JPQL로 오버라이드하여 @Filter(softDeleteFilter) 적용 */
	@Override
	@NonNull
	@Query("SELECT w FROM Workspace w WHERE w.id = :id")
	Optional<Workspace> findById(@Param("id") @NonNull Long id);

	@Query(
		value = """
			SELECT w FROM Workspace w
			WHERE w.id IN (
				SELECT wm.workspace.id FROM WorkspaceMember wm
				WHERE wm.user.id = :userId
			)
			""",
		countQuery = "SELECT COUNT(wm) FROM WorkspaceMember wm WHERE wm.user.id = :userId"
	)
	Page<Workspace> findByMemberUserId(@Param("userId") Long userId, Pageable pageable);

	@Query("""
		SELECT w FROM Workspace w
		WHERE w.id IN (
			SELECT wm.workspace.id FROM WorkspaceMember wm
			WHERE wm.user.id = :userId
		)
		""")
	List<Workspace> findByMemberUserId(@Param("userId") Long userId);

	boolean existsByName(String name);

	Page<Workspace> findByNameContaining(String name, Pageable pageable);
}
