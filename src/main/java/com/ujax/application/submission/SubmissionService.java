package com.ujax.application.submission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.InvalidSubmissionException;
import com.ujax.global.exception.common.Judge0Exception;
import com.ujax.infrastructure.web.submission.dto.SubmissionRequest;
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
                    Duration.ofHours(1)
            );

            return unifiedToken;

        } catch (InvalidSubmissionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Judge0 Submission Failed: ", e);
            throw new Judge0Exception(ErrorCode.JUDGE0_API_ERROR, "코드 제출 처리 중 오류 발생: " + e.getMessage());
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