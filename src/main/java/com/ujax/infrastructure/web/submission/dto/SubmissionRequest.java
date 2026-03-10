package com.ujax.infrastructure.web.submission.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ujax.domain.solution.ProgrammingLanguage;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.InvalidSubmissionException;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record SubmissionRequest(
        @NotBlank(message = "언어는 필수입니다.")
        String language,

        @NotBlank(message = "소스 코드는 필수입니다.")
        String sourceCode,

        @NotEmpty(message = "테스트 케이스는 최소 1개 이상이어야 합니다.")
        List<TestCaseRequest> testCases
) {
    public record TestCaseRequest(
            String input,
            String expected
    ) {
        public TestCaseRequest {
            if (input == null) input = "";
            if (expected == null) expected = "";
        }
    }

    public List<Map<String, Object>> toJudge0Submissions() {
        int languageId = getLanguageId(this.language);
        return testCases.stream()
                .map(tc -> {
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("language_id", languageId);
                    s.put("source_code", sourceCode);
                    s.put("stdin", encodeToBase64(tc.input));
                    s.put("expected_output", encodeToBase64(tc.expected));
                    return s;
                })
                .toList();
    }

    private int getLanguageId(String lang) {
        ProgrammingLanguage programmingLanguage = ProgrammingLanguage.fromSubmissionLanguage(lang);
        if (!programmingLanguage.supportsJudge0()) {
            throw unsupportedLanguage(lang);
        }
        return programmingLanguage.getJudge0Id();
    }

    private InvalidSubmissionException unsupportedLanguage(String lang) {
        return new InvalidSubmissionException(ErrorCode.INVALID_SUBMISSION, "지원하지 않는 언어: " + lang);
    }

    private String encodeToBase64(String raw) {
        return (raw == null || raw.isEmpty()) ? "" : Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
