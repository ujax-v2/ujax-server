package com.ujax.infrastructure.web.submission;

import com.ujax.application.submission.SubmissionService;
import com.ujax.infrastructure.web.submission.dto.SubmissionRequest;
import com.ujax.infrastructure.web.submission.dto.SubmissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/problems/{problemId}")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/submissions")
    public ResponseEntity<SubmissionResponse> createSubmission(
            @PathVariable Long workspaceId,
            @PathVariable Long problemId,
            @RequestBody SubmissionRequest request) {

        String unifiedToken = submissionService.submitAndAggregateTokens(request);

        return ResponseEntity.ok(SubmissionResponse.SubmissionData.ok(unifiedToken));
    }
}