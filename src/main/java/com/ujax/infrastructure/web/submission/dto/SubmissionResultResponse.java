package com.ujax.infrastructure.web.submission.dto;

import com.ujax.infrastructure.external.judge0.dto.Judge0SubmissionItem;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record SubmissionResultResponse(
        String token,
        Integer statusId,
        String statusDescription,
        String stdout,
        String stderr,
        String compileOutput,
        Float time,
        Integer memory,
        String input,
        String expected,
        Boolean isCorrect
) {
    public static SubmissionResultResponse from(Judge0SubmissionItem judge0Submission, String input, String expected) {
        boolean isCorrect = judge0Submission.status().id() == 3;
        return new SubmissionResultResponse(
                judge0Submission.token(),
                judge0Submission.status().id(),
                judge0Submission.status().description(),
                decodeBase64(judge0Submission.stdout()),
                decodeBase64(judge0Submission.stderr()),
                decodeBase64(judge0Submission.compileOutput()),
                judge0Submission.time() != null ? Float.parseFloat(judge0Submission.time()) : null,
                judge0Submission.memory() != null ? (int) Double.parseDouble(judge0Submission.memory()) : null,
                input,
                expected,
                isCorrect
        );
    }

    private static String decodeBase64(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        try {
            return new String(Base64.getDecoder().decode(encoded.replaceAll("\\s", "")), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encoded;
        }
    }
}
