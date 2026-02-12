package com.ujax.infrastructure.web.submission.dto;

import java.util.List;

public record SubmissionResultResponse(
        String code,
        List<TestCaseResult> data
) {
    public record TestCaseResult(
            String token,
            Integer statusId,
            String statusDescription,
            String stdout,
            String stderr,
            String compileOutput,
            Float time,
            Integer memory
    ) {
    }

    public static SubmissionResultResponse ok(List<TestCaseResult> results) {
        return new SubmissionResultResponse("OK", results);
    }
}
