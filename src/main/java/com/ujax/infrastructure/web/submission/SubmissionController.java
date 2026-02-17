package com.ujax.infrastructure.web.submission;

import com.ujax.application.submission.SubmissionService;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.web.submission.dto.SubmissionRequest;
import com.ujax.infrastructure.web.submission.dto.SubmissionResponse;
import com.ujax.infrastructure.web.submission.dto.SubmissionResultResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/workspaces/{workspaceId}/problems/{problemId}/submissions")
    public ApiResponse<SubmissionResponse> createSubmission(
            @PathVariable Long workspaceId,
            @PathVariable Long problemId,
            @RequestBody @Valid SubmissionRequest request) {

        String unifiedToken = submissionService.submitAndAggregateTokens(request);

        return ApiResponse.success(SubmissionResponse.from(unifiedToken));
    }

    @GetMapping("/submissions/{submissionToken}")
    public ApiResponse<List<SubmissionResultResponse>> getResults(
            @PathVariable String submissionToken) {

        List<SubmissionResultResponse> results =
                submissionService.getSubmissionResults(submissionToken);

        return ApiResponse.success(results);
    }
}