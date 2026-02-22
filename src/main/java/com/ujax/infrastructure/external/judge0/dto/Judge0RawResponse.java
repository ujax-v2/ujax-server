package com.ujax.infrastructure.external.judge0.dto;

import java.util.List;

public record Judge0RawResponse(
        List<Judge0SubmissionItem> submissions
) {
}
