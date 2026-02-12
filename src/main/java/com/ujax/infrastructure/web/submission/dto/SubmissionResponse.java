package com.ujax.infrastructure.web.submission.dto;

public record SubmissionResponse(
        String code,
        SubmissionData data
) {
    public record SubmissionData(
            String submissionToken
    ) {
        public static SubmissionData from(String token) {
            return new SubmissionData(token);
        }
    }
}
