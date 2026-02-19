package com.ujax.infrastructure.web.problem;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.problem.ProblemService;
import com.ujax.application.problem.dto.response.ProblemResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.web.problem.dto.request.ProblemIngestRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
public class ProblemController {

	private final ProblemService problemService;

	@GetMapping("/{problemId}")
	public ApiResponse<ProblemResponse> getProblem(@PathVariable Long problemId) {
		return ApiResponse.success(problemService.getProblem(problemId));
	}

	@GetMapping("/number/{problemNumber}")
	public ApiResponse<ProblemResponse> getProblemByNumber(@PathVariable int problemNumber) {
		return ApiResponse.success(problemService.getProblemByNumber(problemNumber));
	}

	@PostMapping("/ingest")
	public ApiResponse<ProblemResponse> ingestProblem(@Valid @RequestBody ProblemIngestRequest request) {
		return ApiResponse.success(problemService.createProblem(request));
	}
}
