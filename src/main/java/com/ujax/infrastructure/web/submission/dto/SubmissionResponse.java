package com.ujax.infrastructure.web.submission.dto;

public record SubmissionResponse(
        String code,
        SubmissionData data
) {
    public record SubmissionData(
            String submissionToken // 여러 토큰을 묶은 통합 식별자 -> front에서 polling할때 한 번에 조회하기 위함
    ) {
        public static SubmissionResponse ok(String token) {
            return new SubmissionResponse("OK", new SubmissionData(token));
        }
    }
}
