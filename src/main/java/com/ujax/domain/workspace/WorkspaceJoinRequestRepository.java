package com.ujax.domain.workspace;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceJoinRequestRepository extends JpaRepository<WorkspaceJoinRequest, Long> {

	boolean existsByWorkspace_IdAndUser_Id(Long workspaceId, Long userId);

	Optional<WorkspaceJoinRequest> findByIdAndWorkspace_Id(Long requestId, Long workspaceId);

	Optional<WorkspaceJoinRequest> findByWorkspace_IdAndUser_Id(Long workspaceId, Long userId);

	Page<WorkspaceJoinRequest> findByWorkspace_IdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);
}
