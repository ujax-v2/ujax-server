package com.ujax.domain.workspace;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceJoinRequestRepository extends JpaRepository<WorkspaceJoinRequest, Long> {

	boolean existsByWorkspace_IdAndUser_IdAndStatus(
		Long workspaceId,
		Long userId,
		WorkspaceJoinRequestStatus status
	);

	Optional<WorkspaceJoinRequest> findByIdAndWorkspace_Id(Long requestId, Long workspaceId);

	Optional<WorkspaceJoinRequest> findTopByWorkspace_IdAndUser_IdOrderByCreatedAtDesc(Long workspaceId, Long userId);

	Page<WorkspaceJoinRequest> findByWorkspace_IdAndStatusOrderByCreatedAtDesc(Long workspaceId,
		WorkspaceJoinRequestStatus status, Pageable pageable);
}
