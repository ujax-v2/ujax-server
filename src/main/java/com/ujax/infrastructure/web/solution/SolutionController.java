package com.ujax.infrastructure.web.solution;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.solution.SolutionLikeService;
import com.ujax.application.solution.SolutionCommentService;
import com.ujax.application.solution.SolutionService;
import com.ujax.application.solution.dto.response.SolutionCommentResponse;
import com.ujax.application.solution.dto.response.SolutionLikeStatusResponse;
import com.ujax.application.solution.dto.response.SolutionMemberSummaryResponse;
import com.ujax.application.solution.dto.response.SolutionResponse;
import com.ujax.application.solution.dto.response.SolutionVersionResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.solution.dto.request.CreateSolutionCommentRequest;
import com.ujax.infrastructure.web.solution.dto.request.SolutionIngestRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SolutionController {

	private final SolutionCommentService solutionCommentService;
	private final SolutionLikeService solutionLikeService;
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

	@GetMapping("/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members")
	public ApiResponse<List<SolutionMemberSummaryResponse>> getSolutionMembers(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.success(
			solutionService.getSolutionMembers(
				workspaceId, problemBoxId, workspaceProblemId, principal.getUserId())
		);
	}

	@GetMapping("/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions")
	public ApiResponse<PageResponse<SolutionVersionResponse>> getSolutionVersions(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@PathVariable Long workspaceMemberId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "1") int size) {
		return ApiResponse.success(
			solutionService.getSolutionVersions(
				workspaceId,
				problemBoxId,
				workspaceProblemId,
				workspaceMemberId,
				principal.getUserId(),
				page,
				size
			)
		);
	}

	@GetMapping("/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/comments")
	public ApiResponse<List<SolutionCommentResponse>> getSolutionComments(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@PathVariable Long workspaceMemberId,
		@PathVariable Long submissionId,
		@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.success(solutionCommentService.getComments(
			workspaceId,
			problemBoxId,
			workspaceProblemId,
			workspaceMemberId,
			submissionId,
			principal.getUserId()
		));
	}

	@PostMapping("/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SolutionCommentResponse> createSolutionComment(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@PathVariable Long workspaceMemberId,
		@PathVariable Long submissionId,
		@Valid @RequestBody CreateSolutionCommentRequest request,
		@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.success(solutionCommentService.createComment(
			workspaceId,
			problemBoxId,
			workspaceProblemId,
			workspaceMemberId,
			submissionId,
			principal.getUserId(),
			request.content()
		));
	}

	@DeleteMapping("/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/comments/{commentId}")
	public ApiResponse<Void> deleteSolutionComment(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@PathVariable Long workspaceMemberId,
		@PathVariable Long submissionId,
		@PathVariable Long commentId,
		@AuthenticationPrincipal UserPrincipal principal) {
		solutionCommentService.deleteComment(
			workspaceId,
			problemBoxId,
			workspaceProblemId,
			workspaceMemberId,
			submissionId,
			commentId,
			principal.getUserId()
		);
		return ApiResponse.success();
	}

	@PutMapping("/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/likes")
	public ApiResponse<SolutionLikeStatusResponse> likeSolution(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@PathVariable Long workspaceMemberId,
		@PathVariable Long submissionId,
		@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.success(solutionLikeService.like(
			workspaceId,
			problemBoxId,
			workspaceProblemId,
			workspaceMemberId,
			submissionId,
			principal.getUserId()
		));
	}

	@DeleteMapping("/workspaces/{workspaceId}/problem-boxes/{problemBoxId}/problems/{workspaceProblemId}/solution-members/{workspaceMemberId}/submissions/{submissionId}/likes")
	public ApiResponse<SolutionLikeStatusResponse> unlikeSolution(
		@PathVariable Long workspaceId,
		@PathVariable Long problemBoxId,
		@PathVariable Long workspaceProblemId,
		@PathVariable Long workspaceMemberId,
		@PathVariable Long submissionId,
		@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.success(solutionLikeService.unlike(
			workspaceId,
			problemBoxId,
			workspaceProblemId,
			workspaceMemberId,
			submissionId,
			principal.getUserId()
		));
	}
}
