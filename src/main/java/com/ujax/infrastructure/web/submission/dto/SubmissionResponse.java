package com.ujax.infrastructure.web.submission.dto;

public record SubmissionResponse(
        String submissionToken
) {
    public static SubmissionResponse from(String token) {
        return new SubmissionResponse(token);
    }
}
