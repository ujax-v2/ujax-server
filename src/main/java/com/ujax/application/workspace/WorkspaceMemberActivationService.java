package com.ujax.application.workspace;

import org.springframework.stereotype.Component;

import com.ujax.domain.user.User;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ConflictException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberActivationService {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	public WorkspaceMember activateMember(Workspace workspace, User user) {
		return workspaceMemberRepository.findByWorkspaceIdAndUserIdIncludingDeleted(workspace.getId(), user.getId())
			.map(existingMember -> restoreMember(user, existingMember))
			.orElseGet(() -> createMember(workspace, user));
	}

	private WorkspaceMember restoreMember(User user, WorkspaceMember existingMember) {
		if (!existingMember.isDeleted()) {
			throw new ConflictException(ErrorCode.ALREADY_WORKSPACE_MEMBER);
		}
		existingMember.restore();
		existingMember.updateRole(WorkspaceMemberRole.MEMBER);
		existingMember.updateNickname(user.getName());
		return existingMember;
	}

	private WorkspaceMember createMember(Workspace workspace, User user) {
		WorkspaceMember member = WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER);
		return workspaceMemberRepository.save(member);
	}
}
