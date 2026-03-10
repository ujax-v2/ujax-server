package com.ujax.infrastructure.web.solution;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.solution.SolutionService;
import com.ujax.application.solution.dto.response.SolutionResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.solution.dto.request.SolutionIngestRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SolutionController {

	private final SolutionService solutionService;

	@PostMapping("/submissions/ingest")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SolutionResponse> ingest(
		@Valid @RequestBody SolutionIngestRequest request,
		@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.success(solutionService.ingest(request, principal.getUserId()));
	}

	@GetMapping("/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solutions")
	public ApiResponse<PageResponse<SolutionResponse>> getSolutions(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(
			solutionService.getSolutions(
				workspaceId, problemBoxId, workspaceProblemId,
				principal.getUserId(), page, size)
		);
	}
}
