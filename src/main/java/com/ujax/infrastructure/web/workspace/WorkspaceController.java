package com.ujax.infrastructure.web.workspace;

import java.util.List;

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

import com.ujax.application.workspace.WorkspaceService;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestResponse;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestListItemResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMemberResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatusResponse;
import com.ujax.application.workspace.dto.response.WorkspaceResponse;
import com.ujax.application.workspace.dto.response.WorkspaceSettingsResponse;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.dto.request.CreateWorkspaceRequest;
import com.ujax.infrastructure.web.workspace.dto.request.InviteWorkspaceMemberRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceMemberNicknameRequest;
import com.ujax.infrastructure.web.workspace.dto.request.WorkspaceImageUploadRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

	private final WorkspaceService workspaceService;

	@GetMapping("/explore")
	public ApiResponse<PageResponse<WorkspaceResponse>> listWorkspaces(
		@RequestParam(required = false) String name,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(workspaceService.listWorkspaces(name, page, size));
	}

	@GetMapping("/me")
	public ApiResponse<List<WorkspaceResponse>> listMyWorkspaces(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(workspaceService.listMyWorkspaces(principal.getUserId()));
	}

	@GetMapping("/{workspaceId}")
	public ApiResponse<WorkspaceResponse> getWorkspace(@PathVariable Long workspaceId) {
		return ApiResponse.success(workspaceService.getWorkspace(workspaceId));
	}

	@GetMapping("/{workspaceId}/settings")
	public ApiResponse<WorkspaceSettingsResponse> getWorkspaceSettings(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(workspaceService.getWorkspaceSettings(workspaceId, principal.getUserId()));
	}

	@GetMapping("/{workspaceId}/members")
	public ApiResponse<PageResponse<WorkspaceMemberResponse>> listWorkspaceMembers(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(workspaceService.listWorkspaceMembers(workspaceId, principal.getUserId(), page, size));
	}

	@GetMapping("/{workspaceId}/members/me")
	public ApiResponse<WorkspaceMemberResponse> getMyWorkspaceMember(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(workspaceService.getMyWorkspaceMember(workspaceId, principal.getUserId()));
	}

	@PatchMapping("/{workspaceId}/members/me/nickname")
	public ApiResponse<WorkspaceMemberResponse> updateMyWorkspaceNickname(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateWorkspaceMemberNicknameRequest request
	) {
		return ApiResponse.success(
			workspaceService.updateMyWorkspaceNickname(workspaceId, principal.getUserId(), request.nickname())
		);
	}

	@PostMapping("/{workspaceId}/members/invite")
	public ApiResponse<Void> inviteWorkspaceMember(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody InviteWorkspaceMemberRequest request
	) {
		workspaceService.inviteWorkspaceMember(workspaceId, principal.getUserId(), request.email());
		return ApiResponse.success();
	}

	@PostMapping("/{workspaceId}/image/presigned-url")
	public ApiResponse<PresignedUrlResponse> createWorkspaceImagePresignedUrl(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody WorkspaceImageUploadRequest request
	) {
		return ApiResponse.success(
			workspaceService.createWorkspaceImagePresignedUrl(workspaceId, principal.getUserId(), request)
		);
	}

	@PostMapping("/{workspaceId}/join-requests")
	public ApiResponse<WorkspaceJoinRequestResponse> createJoinRequest(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(workspaceService.createJoinRequest(workspaceId, principal.getUserId()));
	}

	@GetMapping("/{workspaceId}/join-requests/me")
	public ApiResponse<WorkspaceMyJoinRequestStatusResponse> getMyJoinRequestStatus(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(workspaceService.getMyJoinRequestStatus(workspaceId, principal.getUserId()));
	}

	@GetMapping("/{workspaceId}/join-requests")
	public ApiResponse<PageResponse<WorkspaceJoinRequestListItemResponse>> listJoinRequests(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(workspaceService.listJoinRequests(workspaceId, principal.getUserId(), page, size));
	}

	@PostMapping("/{workspaceId}/join-requests/{requestId}/approve")
	public ApiResponse<Void> approveJoinRequest(
		@PathVariable Long workspaceId,
		@PathVariable Long requestId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceService.approveJoinRequest(workspaceId, principal.getUserId(), requestId);
		return ApiResponse.success();
	}

	@PostMapping("/{workspaceId}/join-requests/{requestId}/reject")
	public ApiResponse<Void> rejectJoinRequest(
		@PathVariable Long workspaceId,
		@PathVariable Long requestId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceService.rejectJoinRequest(workspaceId, principal.getUserId(), requestId);
		return ApiResponse.success();
	}

	@PostMapping
	public ApiResponse<WorkspaceResponse> createWorkspace(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody CreateWorkspaceRequest request
	) {
		return ApiResponse.success(
			workspaceService.createWorkspace(request.name(), request.description(), principal.getUserId())
		);
	}

	@PatchMapping("/{workspaceId}")
	public ApiResponse<WorkspaceResponse> updateWorkspace(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateWorkspaceRequest request
	) {
		return ApiResponse.success(
			workspaceService.updateWorkspace(
				workspaceId,
				principal.getUserId(),
				request.name(),
				request.description(),
				request.hookUrl(),
				request.imageUrl()
			)
		);
	}

	@DeleteMapping("/{workspaceId}")
	public ApiResponse<Void> deleteWorkspace(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceService.deleteWorkspace(workspaceId, principal.getUserId());
		return ApiResponse.success();
	}

	@PatchMapping("/{workspaceId}/members/{workspaceMemberId}/role")
	public ApiResponse<Void> updateWorkspaceMemberRole(
		@PathVariable Long workspaceId,
		@PathVariable Long workspaceMemberId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateWorkspaceMemberRoleRequest request
	) {
		workspaceService.updateWorkspaceMemberRole(workspaceId, principal.getUserId(), workspaceMemberId, request.role());
		return ApiResponse.success();
	}

	@DeleteMapping("/{workspaceId}/members/{workspaceMemberId}")
	public ApiResponse<Void> removeWorkspaceMember(
		@PathVariable Long workspaceId,
		@PathVariable Long workspaceMemberId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceService.removeWorkspaceMember(workspaceId, principal.getUserId(), workspaceMemberId);
		return ApiResponse.success();
	}

	@DeleteMapping("/{workspaceId}/members/me")
	public ApiResponse<Void> leaveWorkspace(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceService.leaveWorkspace(workspaceId, principal.getUserId());
		return ApiResponse.success();
	}
}
