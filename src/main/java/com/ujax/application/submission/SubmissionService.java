package com.ujax.application.submission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.InvalidSubmissionException;
import com.ujax.global.exception.common.Judge0Exception;
import com.ujax.infrastructure.external.judge0.dto.Judge0RawResponse;
import com.ujax.infrastructure.web.submission.dto.SubmissionRequest;
import com.ujax.infrastructure.web.submission.dto.SubmissionResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${judge0.api.url}")
    private String judge0Url;

    private record TestCaseMetadata(String input, String expected) {
    }

    public String submitAndAggregateTokens(SubmissionRequest request) {
        if (request.testCases() == null || request.testCases().isEmpty()) {
            throw new InvalidSubmissionException(ErrorCode.INVALID_SUBMISSION, "테스트 케이스는 최소 1개 이상이어야 합니다.");
        }
        int languageId = getLanguageId(request.language());

        try {
            List<Map<String, Object>> submissions = request.testCases().stream()
                    .map(tc -> {
                        Map<String, Object> s = new LinkedHashMap<>();
                        s.put("language_id", languageId);
                        s.put("source_code", request.sourceCode());
                        s.put("stdin", encodeToBase64(tc.input()));
                        s.put("expected_output", encodeToBase64(tc.expected()));
                        return s;
                    })
                    .toList();

            String jsonPayload = objectMapper.writeValueAsString(Map.of("submissions", submissions));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(judge0Url + "/submissions/batch?base64_encoded=true", entity, String.class);

            if (response.getBody() == null) throw new Judge0Exception(ErrorCode.JUDGE0_API_ERROR, "응답이 비어있습니다.");

            List<Map<String, String>> responseBody = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });

            Map<String, TestCaseMetadata> metadataMap = new LinkedHashMap<>();
            for (int i = 0; i < responseBody.size(); i++) {
                String token = responseBody.get(i).get("token");
                var originalTc = request.testCases().get(i);
                metadataMap.put(token, new TestCaseMetadata(originalTc.input(), originalTc.expected()));
            }

            String unifiedToken = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set("submission:" + unifiedToken, objectMapper.writeValueAsString(metadataMap), Duration.ofHours(1));

            return unifiedToken;
        } catch (InvalidSubmissionException e) {
            throw e;
        } catch (Exception e) {
            throw new Judge0Exception(ErrorCode.JUDGE0_API_ERROR, "제출 중 오류 발생: " + e.getMessage());
        }
    }

    public List<SubmissionResultResponse.TestCaseResult> getSubmissionResults(String submissionToken) {
        String metadataJson = redisTemplate.opsForValue().get("submission:" + submissionToken);
        if (metadataJson == null) throw new InvalidSubmissionException(ErrorCode.RESOURCE_NOT_FOUND, "제출 정보가 만료되었습니다.");

        try {
            // 1. Redis에서 메타데이터 복원
            Map<String, TestCaseMetadata> metadataMap = objectMapper.readValue(metadataJson, new TypeReference<>() {
            });
            String tokens = String.join(",", metadataMap.keySet());

            // 2. Judge0 결과 조회
            String url = String.format("%s/submissions/batch?tokens=%s&base64_encoded=true&fields=token,status_id,status,stdout,stderr,compile_output,time,memory", judge0Url, tokens);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getBody() == null) throw new Judge0Exception(ErrorCode.JUDGE0_API_ERROR, "응답이 비어있습니다.");

            Judge0RawResponse rawResponse = objectMapper.readValue(response.getBody(), Judge0RawResponse.class);

            // 3. 데이터 병합 및 반환
            return rawResponse.submissions().stream().map(item -> {
                TestCaseMetadata meta = metadataMap.get(item.token());
                boolean isCorrect = item.status().id() == 3; // Accepted 상태(status_id == 3)인 경우 성공이므로

                return new SubmissionResultResponse.TestCaseResult(
                        item.token(),
                        item.status().id(),
                        item.status().description(),
                        decodeBase64(item.stdout()),
                        decodeBase64(item.stderr()),
                        decodeBase64(item.compileOutput()),
                        item.time() != null ? Float.parseFloat(item.time()) : null,
                        item.memory() != null ? (int) Double.parseDouble(item.memory()) : null,
                        meta.input(),
                        meta.expected(),
                        isCorrect
                );
            }).toList();
        } catch (Exception e) {
            throw new Judge0Exception(ErrorCode.JUDGE0_API_ERROR, "조회 중 오류 발생: " + e.getMessage());
        }
    }

    private String encodeToBase64(String raw) {
        return (raw == null || raw.isEmpty()) ? "" : Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeBase64(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        try {
            return new String(Base64.getDecoder().decode(encoded.replaceAll("\\s", "")), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encoded;
        }
    }

    // 추후에 enum 도입 검토
    private int getLanguageId(String lang) {
        return switch (lang.toUpperCase()) {
            case "JAVA" -> 62;
            case "PYTHON" -> 71;
            case "CPP" -> 54;
            default -> throw new InvalidSubmissionException(ErrorCode.INVALID_SUBMISSION, "지원하지 않는 언어: " + lang);
        };
    }
}
