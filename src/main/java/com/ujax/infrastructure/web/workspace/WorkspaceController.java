package com.ujax.infrastructure.web.workspace;

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
import com.ujax.application.workspace.dto.response.WorkspaceListResponse;
import com.ujax.application.workspace.dto.response.WorkspaceResponse;
import com.ujax.application.workspace.dto.response.WorkspaceSettingsResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.web.workspace.dto.request.CreateWorkspaceRequest;
import com.ujax.infrastructure.web.workspace.dto.request.UpdateWorkspaceRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

	private final WorkspaceService workspaceService;

	@GetMapping("/explore")
	public ApiResponse<PageResponse<WorkspaceResponse>> listWorkspaces(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(workspaceService.listWorkspaces(page, size));
	}

	@GetMapping("/search")
	public ApiResponse<PageResponse<WorkspaceResponse>> searchWorkspaces(
		@RequestParam String name,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(workspaceService.searchWorkspaces(name, page, size));
	}

	@GetMapping
	public ApiResponse<WorkspaceListResponse> listMyWorkspaces(@RequestParam Long userId) {
		return ApiResponse.success(workspaceService.listMyWorkspaces(userId));
	}

	@GetMapping("/{workspaceId}")
	public ApiResponse<WorkspaceResponse> getWorkspace(@PathVariable Long workspaceId) {
		return ApiResponse.success(workspaceService.getWorkspace(workspaceId));
	}

	@GetMapping("/{workspaceId}/settings")
	public ApiResponse<WorkspaceSettingsResponse> getWorkspaceSettings(
		@PathVariable Long workspaceId,
		@RequestParam Long userId
	) {
		return ApiResponse.success(workspaceService.getWorkspaceSettings(workspaceId, userId));
	}

	@PostMapping
	public ApiResponse<WorkspaceResponse> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
		return ApiResponse.success(
			workspaceService.createWorkspace(request.name(), request.description(), request.userId())
		);
	}

	@PatchMapping("/{workspaceId}")
	public ApiResponse<WorkspaceResponse> updateWorkspace(
		@PathVariable Long workspaceId,
		@RequestParam Long userId,
		@Valid @RequestBody UpdateWorkspaceRequest request
	) {
		return ApiResponse.success(
			workspaceService.updateWorkspace(
				workspaceId,
				userId,
				request.name(),
				request.description(),
				request.mmWebhookUrl()
			)
		);
	}

	@DeleteMapping("/{workspaceId}")
	public ApiResponse<Void> deleteWorkspace(
		@PathVariable Long workspaceId,
		@RequestParam Long userId
	) {
		workspaceService.deleteWorkspace(workspaceId, userId);
		return ApiResponse.success();
	}
}
