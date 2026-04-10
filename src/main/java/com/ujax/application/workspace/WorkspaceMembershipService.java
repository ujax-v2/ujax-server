package com.ujax.application.workspace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.mail.MailNotifier;
import com.ujax.application.workspace.dto.response.WorkspaceMemberListResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMemberResponse;
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
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceMembershipService {

	private static final int NICKNAME_MIN = 1;
	private static final int NICKNAME_MAX = 30;

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final UserRepository userRepository;
	private final WorkspaceMemberActivationService workspaceMemberActivationService;
	private final MailNotifier mailNotifier;

	public PageResponse<WorkspaceMemberListResponse> listWorkspaceMembers(Long workspaceId, Long userId, int page, int size) {
		validatePageable(page, size);
		validateMember(workspaceId, userId);
		Page<WorkspaceMember> members = workspaceMemberRepository.findByWorkspace_Id(
			workspaceId,
			PageRequest.of(page, size)
		);

		return PageResponse.of(
			members.getContent().stream().map(WorkspaceMemberListResponse::from).toList(),
			members.getNumber(),
			members.getSize(),
			members.getTotalElements(),
			members.getTotalPages()
		);
	}

	public WorkspaceMemberResponse getMyWorkspaceMember(Long workspaceId, Long userId) {
		WorkspaceMember member = validateMember(workspaceId, userId);
		return WorkspaceMemberResponse.from(member);
	}

	@Transactional
	public WorkspaceMemberResponse updateMyWorkspaceNickname(Long workspaceId, Long userId, String nickname) {
		WorkspaceMember member = validateMember(workspaceId, userId);
		validateNickname(nickname);
		member.updateNickname(nickname);
		return WorkspaceMemberResponse.from(member);
	}

	@Transactional
	public void inviteWorkspaceMember(Long workspaceId, Long userId, String email) {
		validateOwner(workspaceId, userId);
		Workspace workspace = findWorkspaceById(workspaceId);

		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

		workspaceMemberActivationService.activateMember(workspace, user);
		mailNotifier.enqueueWorkspaceInvite(email, workspace.getName(), workspaceId);
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

	private Workspace findWorkspaceById(Long workspaceId) {
		return workspaceRepository.findById(workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_NOT_FOUND));
	}

	private WorkspaceMember findWorkspaceMember(Long workspaceId, Long workspaceMemberId) {
		return workspaceMemberRepository.findByWorkspace_IdAndId(workspaceId, workspaceMemberId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_MEMBER_NOT_FOUND));
	}

	private void validatePageable(int page, int size) {
		if (page < 0 || size <= 0) {
			throw new BadRequestException(ErrorCode.INVALID_PARAMETER);
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
