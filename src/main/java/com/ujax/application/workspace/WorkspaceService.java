package com.ujax.application.workspace;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.workspace.dto.response.WorkspaceMemberListResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMemberResponse;
import com.ujax.application.workspace.dto.response.WorkspaceResponse;
import com.ujax.application.workspace.dto.response.WorkspaceSettingsResponse;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

	private static final int NAME_MIN = 1;
	private static final int NAME_MAX = 50;
	private static final int DESCRIPTION_MAX = 200;
	private static final int NICKNAME_MIN = 1;
	private static final int NICKNAME_MAX = 30;
	private static final Sort WORKSPACE_DEFAULT_SORT = Sort.by(
		Sort.Order.desc("createdAt"),
		Sort.Order.desc("id")
	);

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final UserRepository userRepository;
	private final WorkspaceInviteMailer workspaceInviteMailer;

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

	public PageResponse<WorkspaceResponse> listMyWorkspaces(Long userId, int page, int size) {
		validatePageable(page, size);
		Page<Workspace> workspaces = workspaceRepository.findByMemberUserId(userId, PageRequest.of(page, size));
		return PageResponse.of(
			workspaces.getContent().stream().map(WorkspaceResponse::from).toList(),
			workspaces.getNumber(),
			workspaces.getSize(),
			workspaces.getTotalElements(),
			workspaces.getTotalPages()
		);
	}

	public WorkspaceMemberListResponse listWorkspaceMembers(Long workspaceId, Long userId) {
		validateMember(workspaceId, userId);
		List<WorkspaceMemberResponse> items = workspaceMemberRepository.findByWorkspace_Id(workspaceId).stream()
			.map(WorkspaceMemberResponse::from)
			.toList();
		return WorkspaceMemberListResponse.of(items);
	}

	public WorkspaceMemberResponse getMyWorkspaceMember(Long workspaceId, Long userId) {
		WorkspaceMember member = validateMember(workspaceId, userId);
		return WorkspaceMemberResponse.from(member);
	}

	@Transactional
	public void inviteWorkspaceMember(Long workspaceId, Long userId, String email) {
		validateOwner(workspaceId, userId);
		Workspace workspace = findWorkspaceById(workspaceId);

		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		workspaceMemberRepository.findByWorkspaceIdAndUserIdIncludingDeleted(workspaceId, user.getId())
			.ifPresent(member -> {
				if (!member.isDeleted()) {
					throw new ConflictException(ErrorCode.ALREADY_WORKSPACE_MEMBER);
				}
				member.restore();
				member.updateRole(WorkspaceMemberRole.MEMBER);
				member.updateNickname(user.getName());
			});

		if (workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, user.getId()).isPresent()) {
			workspaceInviteMailer.sendInvitation(email, workspace.getName(), workspaceId);
			return;
		}

		WorkspaceMember member = WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER);
		workspaceMemberRepository.save(member);
		workspaceInviteMailer.sendInvitation(email, workspace.getName(), workspaceId);
	}

	public WorkspaceResponse getWorkspace(Long workspaceId) {
		return WorkspaceResponse.from(findWorkspaceById(workspaceId));
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
	public WorkspaceResponse updateWorkspace(Long workspaceId, Long userId, String name, String description, String mmWebhookUrl) {
		Workspace workspace = findWorkspaceById(workspaceId);
		validateOwner(workspaceId, userId);

		if (name == null && description == null && mmWebhookUrl == null) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		if (name != null) {
			validateName(name);
			validateNameDuplicate(name, workspace.getName());
		}
		if (description != null) {
			validateDescription(description);
		}

		workspace.update(name, description, mmWebhookUrl);
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

	@Transactional
	public void updateWorkspaceMemberRole(Long workspaceId, Long userId, Long workspaceMemberId, WorkspaceMemberRole role) {
		WorkspaceMember owner = validateOwner(workspaceId, userId);
		WorkspaceMember target = findWorkspaceMember(workspaceId, workspaceMemberId);

		if (target.getRole() == WorkspaceMemberRole.OWNER) {
			throw new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN, "소유자 권한은 변경할 수 없습니다.");
		}

		if (role == WorkspaceMemberRole.OWNER) {
			if (owner.getId().equals(target.getId())) {
				throw new BadRequestException(ErrorCode.INVALID_INPUT);
			}
			target.updateRole(WorkspaceMemberRole.OWNER);
			owner.updateRole(WorkspaceMemberRole.MANAGER);
			return;
		}

		target.updateRole(role);
	}

	@Transactional
	public void removeWorkspaceMember(Long workspaceId, Long userId, Long workspaceMemberId) {
		WorkspaceMember actor = validateMember(workspaceId, userId);
		WorkspaceMember target = findWorkspaceMember(workspaceId, workspaceMemberId);

		if (actor.getRole() == WorkspaceMemberRole.MEMBER) {
			throw new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN, "멤버는 다른 사용자를 추방할 수 없습니다.");
		}

		if (target.getRole() == WorkspaceMemberRole.OWNER) {
			throw new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN, "소유자는 추방할 수 없습니다.");
		}

		if (actor.getRole() == WorkspaceMemberRole.MANAGER && target.getRole() != WorkspaceMemberRole.MEMBER) {
			throw new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN, "매니저는 멤버만 추방할 수 있습니다.");
		}

		if (actor.getId().equals(target.getId())) {
			throw new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN, "자기 자신은 추방할 수 없습니다.");
		}

		workspaceMemberRepository.delete(target);
	}

	@Transactional
	public void leaveWorkspace(Long workspaceId, Long userId) {
		WorkspaceMember member = validateMember(workspaceId, userId);
		if (member.getRole() == WorkspaceMemberRole.OWNER) {
			throw new ForbiddenException(ErrorCode.WORKSPACE_FORBIDDEN, "소유자는 워크스페이스를 탈퇴할 수 없습니다.");
		}
		workspaceMemberRepository.delete(member);
	}

	@Transactional
	public WorkspaceMemberResponse updateMyWorkspaceNickname(Long workspaceId, Long userId, String nickname) {
		WorkspaceMember member = validateMember(workspaceId, userId);
		validateNickname(nickname);
		member.updateNickname(nickname);
		return WorkspaceMemberResponse.from(member);
	}

	private Workspace findWorkspaceById(Long workspaceId) {
		return workspaceRepository.findById(workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_NOT_FOUND));
	}

	private User findUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
	}

	private WorkspaceMember findWorkspaceMember(Long workspaceId, Long workspaceMemberId) {
		return workspaceMemberRepository.findByWorkspace_IdAndId(workspaceId, workspaceMemberId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_MEMBER_NOT_FOUND));
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

	private void validateNickname(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		int length = nickname.length();
		if (length < NICKNAME_MIN || length > NICKNAME_MAX) {
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

	private WorkspaceMember validateMember(Long workspaceId, Long userId) {
		return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
	}
}
