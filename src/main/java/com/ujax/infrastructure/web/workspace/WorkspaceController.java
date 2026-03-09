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
import com.ujax.application.workspace.dto.response.WorkspaceResponse;
import com.ujax.application.workspace.dto.response.WorkspaceSettingsResponse;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.dto.request.CreateWorkspaceRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceRequest;
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
				request.mmWebhookUrl(),
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
}
