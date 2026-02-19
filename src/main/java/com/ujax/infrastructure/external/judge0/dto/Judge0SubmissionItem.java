package com.ujax.infrastructure.external.judge0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Judge0SubmissionItem(
        String token,
        Judge0Status status,
        String stdout,
        String stderr,

        @JsonProperty("compile_output")
        String compileOutput,

        String time,
        String memory
) {
}
