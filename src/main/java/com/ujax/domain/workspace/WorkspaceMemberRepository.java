package com.ujax.domain.workspace;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
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
}
