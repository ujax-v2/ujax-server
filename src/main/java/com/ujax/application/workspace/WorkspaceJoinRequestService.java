package com.ujax.application.workspace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestListItemResponse;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatus;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatusResponse;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceJoinRequest;
import com.ujax.domain.workspace.WorkspaceJoinRequestRepository;
import com.ujax.domain.workspace.WorkspaceJoinRequestStatus;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceJoinRequestService {

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceJoinRequestRepository workspaceJoinRequestRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final UserRepository userRepository;
	private final WorkspaceMemberActivationService workspaceMemberActivationService;

	@Transactional
	public WorkspaceJoinRequestResponse createJoinRequest(Long workspaceId, Long userId) {
		Workspace workspace = findWorkspaceById(workspaceId);
		User user = findUserById(userId);

		if (workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId).isPresent()) {
			throw new ConflictException(ErrorCode.ALREADY_WORKSPACE_MEMBER);
		}
		if (workspaceJoinRequestRepository.existsByWorkspace_IdAndUser_IdAndStatus(
			workspaceId,
			userId,
			WorkspaceJoinRequestStatus.PENDING
		)) {
			throw new ConflictException(ErrorCode.WORKSPACE_JOIN_REQUEST_ALREADY_PENDING);
		}

		WorkspaceJoinRequest created = workspaceJoinRequestRepository.save(WorkspaceJoinRequest.create(workspace, user));
		return WorkspaceJoinRequestResponse.from(created);
	}

	public WorkspaceMyJoinRequestStatusResponse getMyJoinRequestStatus(Long workspaceId, Long userId) {
		findWorkspaceById(workspaceId);

		if (workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId).isPresent()) {
			return WorkspaceMyJoinRequestStatusResponse.of(true, WorkspaceMyJoinRequestStatus.MEMBER, false);
		}
		if (workspaceJoinRequestRepository.existsByWorkspace_IdAndUser_IdAndStatus(
			workspaceId,
			userId,
			WorkspaceJoinRequestStatus.PENDING
		)) {
			return WorkspaceMyJoinRequestStatusResponse.of(false, WorkspaceMyJoinRequestStatus.PENDING, false);
		}

		return workspaceJoinRequestRepository.findTopByWorkspace_IdAndUser_IdOrderByCreatedAtDesc(workspaceId, userId)
			.filter(request -> request.getStatus() == WorkspaceJoinRequestStatus.REJECTED)
			.map(request -> WorkspaceMyJoinRequestStatusResponse.of(false, WorkspaceMyJoinRequestStatus.REJECTED, true))
			.orElseGet(() -> WorkspaceMyJoinRequestStatusResponse.of(false, WorkspaceMyJoinRequestStatus.NONE, true));
	}

	public PageResponse<WorkspaceJoinRequestListItemResponse> listJoinRequests(
		Long workspaceId,
		Long userId,
		int page,
		int size
	) {
		findWorkspaceById(workspaceId);
		validatePageable(page, size);
		validateOwner(workspaceId, userId);

		Page<WorkspaceJoinRequest> joinRequests = workspaceJoinRequestRepository.findByWorkspace_IdAndStatusOrderByCreatedAtDesc(
			workspaceId,
			WorkspaceJoinRequestStatus.PENDING,
			PageRequest.of(page, size)
		);

		return PageResponse.of(
			joinRequests.getContent().stream().map(WorkspaceJoinRequestListItemResponse::from).toList(),
			joinRequests.getNumber(),
			joinRequests.getSize(),
			joinRequests.getTotalElements(),
			joinRequests.getTotalPages()
		);
	}

	@Transactional
	public void approveJoinRequest(Long workspaceId, Long userId, Long requestId) {
		Workspace workspace = findWorkspaceById(workspaceId);
		validateOwner(workspaceId, userId);

		WorkspaceJoinRequest joinRequest = findWorkspaceJoinRequestById(workspaceId, requestId);
		joinRequest.approve();
		workspaceMemberActivationService.activateMember(workspace, joinRequest.getUser());
	}

	@Transactional
	public void rejectJoinRequest(Long workspaceId, Long userId, Long requestId) {
		findWorkspaceById(workspaceId);
		validateOwner(workspaceId, userId);

		WorkspaceJoinRequest joinRequest = findWorkspaceJoinRequestById(workspaceId, requestId);
		joinRequest.reject();
	}

	private Workspace findWorkspaceById(Long workspaceId) {
		return workspaceRepository.findById(workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_NOT_FOUND));
	}

	private User findUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
	}

	private WorkspaceJoinRequest findWorkspaceJoinRequestById(Long workspaceId, Long requestId) {
		return workspaceJoinRequestRepository.findByIdAndWorkspace_Id(requestId, workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_JOIN_REQUEST_NOT_FOUND));
	}

	private void validateOwner(Long workspaceId, Long userId) {
		WorkspaceMember member = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));

		if (member.getRole() != WorkspaceMemberRole.OWNER) {
			throw new ForbiddenException(ErrorCode.WORKSPACE_OWNER_REQUIRED);
		}
	}

	private void validatePageable(int page, int size) {
		if (page < 0 || size <= 0) {
			throw new BadRequestException(ErrorCode.INVALID_PARAMETER);
		}
	}
}
