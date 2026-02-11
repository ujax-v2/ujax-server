package com.ujax.infrastructure.web.submission.dto;

import java.util.List;

public record SubmissionRequest(
        String language,
        String sourceCode, // source code는 인코딩해서 받기
        List<TestCaseRequest> testCases // 테케 세트들은 raw text로 받는걸로 (서비스에서 base64 인코딩)
) {
    public record TestCaseRequest(
            String input, // 테스트케이스 input
            String expected // 결과 기댓값
    ) {
    }
}
