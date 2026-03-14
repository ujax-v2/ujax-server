package com.ujax.application.workspace;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.workspace.dto.response.WorkspaceResponse;
import com.ujax.application.workspace.dto.response.WorkspaceSettingsResponse;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
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
import com.ujax.infrastructure.external.s3.S3StorageService;
import com.ujax.infrastructure.external.s3.dto.PresignedUrlResult;
import com.ujax.infrastructure.web.workspace.dto.request.WorkspaceImageUploadRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

	private static final int NAME_MIN = 1;
	private static final int NAME_MAX = 50;
	private static final int DESCRIPTION_MAX = 200;
	private static final Sort WORKSPACE_DEFAULT_SORT = Sort.by(
		Sort.Order.desc("createdAt"),
		Sort.Order.desc("id")
	);

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final UserRepository userRepository;
	private final S3StorageService s3StorageService;

	public PageResponse<WorkspaceResponse> listWorkspaces(String name, int page, int size) {
		validatePageable(page, size);
		String normalizedName = name == null ? null : name.trim();
		PageRequest pageable = PageRequest.of(page, size, WORKSPACE_DEFAULT_SORT);
		Page<Workspace> workspaces = workspaceRepository.findByNameContainingOrAll(normalizedName, pageable);
		return PageResponse.of(
			workspaces.getContent().stream().map(WorkspaceResponse::from).toList(),
			workspaces.getNumber(),
			workspaces.getSize(),
			workspaces.getTotalElements(),
			workspaces.getTotalPages()
		);
	}

	public List<WorkspaceResponse> listMyWorkspaces(Long userId) {
		return workspaceRepository.findByMemberUserId(userId, WORKSPACE_DEFAULT_SORT).stream()
			.map(WorkspaceResponse::from)
			.toList();
	}

	public WorkspaceResponse getWorkspace(Long workspaceId) {
		return WorkspaceResponse.from(findWorkspaceById(workspaceId));
	}

	public PresignedUrlResponse createWorkspaceImagePresignedUrl(
		Long workspaceId,
		Long userId,
		WorkspaceImageUploadRequest request
	) {
		findWorkspaceById(workspaceId);
		validateOwner(workspaceId, userId);
		PresignedUrlResult result = s3StorageService.generateWorkspaceImagePresignedUrl(
			workspaceId,
			request.contentType(),
			request.fileSize()
		);
		return new PresignedUrlResponse(result.presignedUrl(), result.imageUrl());
	}

	@Transactional
	public WorkspaceResponse createWorkspace(String name, String description, Long userId) {
		validateName(name);
		validateDescription(description);
		validateNameDuplicate(name, null);

		User user = findUserById(userId);

		Workspace workspace = Workspace.create(name, description);
		Workspace saved = workspaceRepository.save(workspace);
		WorkspaceMember owner = WorkspaceMember.create(saved, user, WorkspaceMemberRole.OWNER);
		workspaceMemberRepository.save(owner);
		return WorkspaceResponse.from(saved);
	}

	@Transactional
	public WorkspaceResponse updateWorkspace(
		Long workspaceId,
		Long userId,
		String name,
		String description,
		String hookUrl,
		String imageUrl
	) {
		Workspace workspace = findWorkspaceById(workspaceId);
		validateOwner(workspaceId, userId);

		if (name == null && description == null && hookUrl == null && imageUrl == null) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		if (name != null) {
			validateName(name);
			validateNameDuplicate(name, workspace.getName());
		}
		if (description != null) {
			validateDescription(description);
		}

		workspace.update(name, description, hookUrl, imageUrl);
		return WorkspaceResponse.from(workspace);
	}

	@Transactional
	public void deleteWorkspace(Long workspaceId, Long userId) {
		Workspace workspace = findWorkspaceById(workspaceId);
		validateOwner(workspaceId, userId);
		workspaceMemberRepository.findByWorkspace_Id(workspaceId)
			.forEach(workspaceMemberRepository::delete);
		workspaceRepository.delete(workspace);
	}

	public WorkspaceSettingsResponse getWorkspaceSettings(Long workspaceId, Long userId) {
		Workspace workspace = findWorkspaceById(workspaceId);
		validateOwner(workspaceId, userId);
		return WorkspaceSettingsResponse.from(workspace);
	}

	private Workspace findWorkspaceById(Long workspaceId) {
		return workspaceRepository.findById(workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_NOT_FOUND));
	}

	private User findUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
	}

	private void validateName(String name) {
		if (name == null || name.isBlank()) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		int length = name.length();
		if (length < NAME_MIN || length > NAME_MAX) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
	}

	private void validatePageable(int page, int size) {
		if (page < 0 || size <= 0) {
			throw new BadRequestException(ErrorCode.INVALID_PARAMETER);
		}
	}

	private void validateDescription(String description) {
		if (description == null) {
			return;
		}
		if (description.length() > DESCRIPTION_MAX) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
	}

	private void validateNameDuplicate(String name, String currentName) {
		if (currentName != null && currentName.equals(name)) {
			return;
		}
		if (workspaceRepository.existsByName(name)) {
			throw new ConflictException(ErrorCode.WORKSPACE_NAME_DUPLICATE);
		}
	}

	private WorkspaceMember validateOwner(Long workspaceId, Long userId) {
		WorkspaceMember member = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));

		if (member.getRole() != WorkspaceMemberRole.OWNER) {
			throw new ForbiddenException(ErrorCode.WORKSPACE_OWNER_REQUIRED);
		}
		return member;
	}
}
