package com.ujax.infrastructure.web.workspace;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.workspace.WorkspaceJoinRequestService;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestListItemResponse;
import com.ujax.application.workspace.dto.response.WorkspaceJoinRequestResponse;
import com.ujax.application.workspace.dto.response.WorkspaceMyJoinRequestStatusResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/join-requests")
@RequiredArgsConstructor
public class WorkspaceJoinRequestController {

	private final WorkspaceJoinRequestService workspaceJoinRequestService;

	@PostMapping
	public ApiResponse<WorkspaceJoinRequestResponse> createJoinRequest(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(workspaceJoinRequestService.createJoinRequest(workspaceId, principal.getUserId()));
	}

	@GetMapping("/me")
	public ApiResponse<WorkspaceMyJoinRequestStatusResponse> getMyJoinRequestStatus(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(workspaceJoinRequestService.getMyJoinRequestStatus(workspaceId, principal.getUserId()));
	}

	@DeleteMapping("/me")
	public ApiResponse<Void> cancelJoinRequest(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceJoinRequestService.cancelJoinRequest(workspaceId, principal.getUserId());
		return ApiResponse.success();
	}

	@GetMapping
	public ApiResponse<PageResponse<WorkspaceJoinRequestListItemResponse>> listJoinRequests(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(workspaceJoinRequestService.listJoinRequests(workspaceId, principal.getUserId(), page, size));
	}

	@PostMapping("/{requestId}/approve")
	public ApiResponse<Void> approveJoinRequest(
		@PathVariable Long workspaceId,
		@PathVariable Long requestId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceJoinRequestService.approveJoinRequest(workspaceId, principal.getUserId(), requestId);
		return ApiResponse.success();
	}

	@PostMapping("/{requestId}/reject")
	public ApiResponse<Void> rejectJoinRequest(
		@PathVariable Long workspaceId,
		@PathVariable Long requestId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceJoinRequestService.rejectJoinRequest(workspaceId, principal.getUserId(), requestId);
		return ApiResponse.success();
	}
}
