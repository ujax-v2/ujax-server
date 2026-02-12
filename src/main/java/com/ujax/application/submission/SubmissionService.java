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

            Map<String, Object> batchRequest = Map.of("submissions", submissions);
            String jsonPayload = objectMapper.writeValueAsString(batchRequest);

            //Judge0로 전송
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            String url = judge0Url + "/submissions/batch?base64_encoded=true";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getBody() == null) {
                throw new Judge0Exception(ErrorCode.JUDGE0_API_ERROR, "Judge0 서버 응답이 비어있습니다.");
            }

            //응답 파싱 및 Redis 저장
            List<Map<String, String>> responseBody = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<>() {
                    }
            );

            List<String> tokens = responseBody.stream()
                    .map(res -> res.get("token"))
                    .filter(Objects::nonNull)
                    .toList();

            String unifiedToken = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(
                    "submission:" + unifiedToken,
                    String.join(",", tokens),
                    Duration.ofMinutes(5)
            );
            return unifiedToken;

        } catch (InvalidSubmissionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Judge0 Submission Failed: ", e);
            throw new Judge0Exception(ErrorCode.JUDGE0_API_ERROR, "코드 제출 처리 중 오류 발생: " + e.getMessage());
        }
    }

    public List<SubmissionResultResponse.TestCaseResult> getSubmissionResults(String submissionToken) {
        // Redis 조회
        String tokens = redisTemplate.opsForValue().get("submission:" + submissionToken);
        if (tokens == null) {
            throw new InvalidSubmissionException(ErrorCode.RESOURCE_NOT_FOUND, "제출 정보가 존재하지 않습니다.");
        }

        // Judge0 호출
        String url = String.format("%s/submissions/batch?tokens=%s&base64_encoded=true&fields=token,status_id,status,stdout,stderr,compile_output,time,memory",
                judge0Url, tokens);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getBody() == null) {
                throw new Judge0Exception(ErrorCode.JUDGE0_API_ERROR, "Judge0 서버 응답이 비어있습니다.");
            }

            Judge0RawResponse rawResponse = objectMapper.readValue(response.getBody(), Judge0RawResponse.class);

            // 데이터 변환 및 디코딩
            return rawResponse.submissions().stream().map(item -> {
                String timeStr = item.time() != null ? String.valueOf(item.time()) : null;
                String memoryStr = item.memory() != null ? String.valueOf(item.memory()) : null;

                return new SubmissionResultResponse.TestCaseResult(
                        item.token(),
                        item.status().id(),
                        item.status().description(),
                        decodeBase64(item.stdout()),
                        decodeBase64(item.stderr()),
                        decodeBase64(item.compileOutput()),
                        timeStr != null ? Float.parseFloat(timeStr) : null,
                        memoryStr != null ? (int) Double.parseDouble(memoryStr) : null
                );
            }).toList();

        } catch (Judge0Exception e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch results from Judge0: ", e);
            throw new Judge0Exception(ErrorCode.JUDGE0_API_ERROR, "결과 조회 중 오류 발생: " + e.getMessage());
        }
    }

    // front에는 채점 결과를 다시 decoding해서 보내기
    private String decodeBase64(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encoded.replaceAll("\\s", ""));
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("Base64 decoding failed for: {}", encoded);
            return encoded;
        }
    }

    private String encodeToBase64(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private int getLanguageId(String language) {
        if (language == null) {
            throw new InvalidSubmissionException(ErrorCode.INVALID_SUBMISSION, "언어 정보가 누락되었습니다.");
        }
        return switch (language.toUpperCase()) {
            case "JAVA" -> 62;
            case "PYTHON" -> 71;
            case "CPP" -> 54;
            default -> throw new InvalidSubmissionException(ErrorCode.INVALID_SUBMISSION, "지원하지 않는 언어입니다: " + language);
        };
    }
}
