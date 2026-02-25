package com.ujax.infrastructure.web.problem;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.problem.ProblemBoxService;
import com.ujax.application.problem.dto.response.ProblemBoxListItemResponse;
import com.ujax.application.problem.dto.response.ProblemBoxResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.problem.dto.request.CreateProblemBoxRequest;
import com.ujax.infrastructure.web.problem.dto.request.UpdateProblemBoxRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/problem-boxes")
@RequiredArgsConstructor
public class ProblemBoxController {

	private final ProblemBoxService problemBoxService;

	@GetMapping
	public ApiResponse<PageResponse<ProblemBoxListItemResponse>> listProblemBoxes(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "9") int size
	) {
		return ApiResponse.success(
			problemBoxService.listProblemBoxes(workspaceId, principal.getUserId(), page, size)
		);
	}

	@GetMapping("/{problemBoxId}")
	public ApiResponse<ProblemBoxResponse> getProblemBox(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(
			problemBoxService.getProblemBox(workspaceId, problemBoxId, principal.getUserId())
		);
	}

	@PostMapping
	public ApiResponse<ProblemBoxResponse> createProblemBox(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody CreateProblemBoxRequest request
	) {
		return ApiResponse.success(
			problemBoxService.createProblemBox(
				workspaceId, principal.getUserId(), request)
		);
	}

	@PatchMapping("/{problemBoxId}")
	public ApiResponse<ProblemBoxResponse> updateProblemBox(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateProblemBoxRequest request
	) {
		return ApiResponse.success(
			problemBoxService.updateProblemBox(
				workspaceId, problemBoxId, principal.getUserId(), request
			)
		);
	}

	@DeleteMapping("/{problemBoxId}")
	public ApiResponse<Void> deleteProblemBox(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		problemBoxService.deleteProblemBox(workspaceId, problemBoxId, principal.getUserId());
		return ApiResponse.success();
	}
}
