package com.ujax.infrastructure.web.workspace;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.workspace.WorkspaceMembershipService;
import com.ujax.application.workspace.dto.response.WorkspaceMemberResponse;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.global.dto.ApiResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.dto.request.InviteWorkspaceMemberRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceMemberNicknameRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceMemberRoleRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
@RequiredArgsConstructor
public class WorkspaceMembershipController {

	private final WorkspaceMembershipService workspaceMembershipService;

	@GetMapping
	public ApiResponse<PageResponse<WorkspaceMemberResponse>> listWorkspaceMembers(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(workspaceMembershipService.listWorkspaceMembers(workspaceId, principal.getUserId(), page, size));
	}

	@GetMapping("/me")
	public ApiResponse<WorkspaceMemberResponse> getMyWorkspaceMember(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(workspaceMembershipService.getMyWorkspaceMember(workspaceId, principal.getUserId()));
	}

	@PatchMapping("/me/nickname")
	public ApiResponse<WorkspaceMemberResponse> updateMyWorkspaceNickname(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateWorkspaceMemberNicknameRequest request
	) {
		return ApiResponse.success(
			workspaceMembershipService.updateMyWorkspaceNickname(workspaceId, principal.getUserId(), request.nickname())
		);
	}

	@PostMapping("/invite")
	public ApiResponse<Void> inviteWorkspaceMember(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody InviteWorkspaceMemberRequest request
	) {
		workspaceMembershipService.inviteWorkspaceMember(workspaceId, principal.getUserId(), request.email());
		return ApiResponse.success();
	}

	@PatchMapping("/{workspaceMemberId}/role")
	public ApiResponse<Void> updateWorkspaceMemberRole(
		@PathVariable Long workspaceId,
		@PathVariable Long workspaceMemberId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateWorkspaceMemberRoleRequest request
	) {
		WorkspaceMemberRole role = request.role();
		workspaceMembershipService.updateWorkspaceMemberRole(workspaceId, principal.getUserId(), workspaceMemberId, role);
		return ApiResponse.success();
	}

	@DeleteMapping("/{workspaceMemberId}")
	public ApiResponse<Void> removeWorkspaceMember(
		@PathVariable Long workspaceId,
		@PathVariable Long workspaceMemberId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceMembershipService.removeWorkspaceMember(workspaceId, principal.getUserId(), workspaceMemberId);
		return ApiResponse.success();
	}

	@DeleteMapping("/me")
	public ApiResponse<Void> leaveWorkspace(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceMembershipService.leaveWorkspace(workspaceId, principal.getUserId());
		return ApiResponse.success();
	}
}
