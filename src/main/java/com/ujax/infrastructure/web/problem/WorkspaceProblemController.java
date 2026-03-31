package com.ujax.infrastructure.web.problem;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.problem.WorkspaceProblemService;
import com.ujax.application.problem.dto.response.WorkspaceProblemResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.problem.dto.request.CreateWorkspaceProblemRequest;
import com.ujax.infrastructure.web.problem.dto.request.UpdateWorkspaceProblemRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems")
@RequiredArgsConstructor
@Validated
public class WorkspaceProblemController {

	private final WorkspaceProblemService workspaceProblemService;

	@GetMapping
	public ApiResponse<PageResponse<WorkspaceProblemResponse>> listWorkspaceProblems(
		@PathVariable @Positive Long workspaceId,
		@PathVariable @Positive Long problemBoxId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(required = false) String keyword,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Positive int size
	) {
		return ApiResponse.success(
			workspaceProblemService.listWorkspaceProblems(
				workspaceId, problemBoxId, principal.getUserId(), keyword, page, size)
		);
	}

	@PostMapping
	public ApiResponse<WorkspaceProblemResponse> createWorkspaceProblem(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody CreateWorkspaceProblemRequest request
	) {
		return ApiResponse.success(
			workspaceProblemService.createWorkspaceProblem(
				workspaceId, problemBoxId, principal.getUserId(), request)
		);
	}

	@PatchMapping("/{workspaceProblemId}")
	public ApiResponse<WorkspaceProblemResponse> updateWorkspaceProblem(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateWorkspaceProblemRequest request
	) {
		return ApiResponse.success(
			workspaceProblemService.updateWorkspaceProblem(
				workspaceId, problemBoxId, workspaceProblemId, principal.getUserId(), request)
		);
	}

	@DeleteMapping("/{workspaceProblemId}")
	public ApiResponse<Void> deleteWorkspaceProblem(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		workspaceProblemService.deleteWorkspaceProblem(
			workspaceId, problemBoxId, workspaceProblemId, principal.getUserId());
		return ApiResponse.success();
	}
}
