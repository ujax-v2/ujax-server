package com.ujax.application.submission;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.global.exception.common.InvalidSubmissionException;
import com.ujax.global.exception.common.Judge0Exception;
import com.ujax.infrastructure.web.submission.dto.SubmissionRequest;
import com.ujax.infrastructure.web.submission.dto.SubmissionResultResponse;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @InjectMocks
    private SubmissionService submissionService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(submissionService, "judge0Url", "http://localhost:2358");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("성공: 코드 제출 시 토큰과 메타데이터(input, expected)를 JSON Map으로 변환하여 Redis에 저장한다")
    void submitAndAggregateTokens_Success() throws Exception {
        // given
        var testCase = new SubmissionRequest.TestCaseRequest("1 2", "3");
        var request = new SubmissionRequest("JAVA", "encodedCode", List.of(testCase));

        String judge0Response = "[{\"token\": \"token-123\"}]";
        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willReturn(ResponseEntity.ok(judge0Response));

        // when
        String unifiedToken = submissionService.submitAndAggregateTokens(request);

        // then
        assertThat(unifiedToken).isNotNull();
        // Redis에 단순 토큰이 아닌, 입력/기대값이 포함된 JSON 구조가 저장되는지 검증
        verify(valueOperations).set(contains("submission:"), contains("\"input\":\"1 2\""), any());
        verify(valueOperations).set(contains("submission:"), contains("\"expected\":\"3\""), any());
    }

    @Test
    @DisplayName("실패: 지원하지 않는 언어를 입력하면 InvalidSubmissionException이 발생한다")
    void submit_InvalidLanguage() {
        // given
        var request = new SubmissionRequest("BASIC", "code",
                List.of(new SubmissionRequest.TestCaseRequest("in", "out")));

        // when & then
        assertThatThrownBy(() -> submissionService.submitAndAggregateTokens(request))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessageContaining("지원하지 않는 언어: BASIC");
    }

    @Test
    @DisplayName("실패: 테스트 케이스가 비어있으면 InvalidSubmissionException이 발생한다")
    void submit_EmptyTestCases() {
        // given
        var request = new SubmissionRequest("JAVA", "code", List.of());

        // when & then
        assertThatThrownBy(() -> submissionService.submitAndAggregateTokens(request))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessageContaining("테스트 케이스는 최소 1개 이상");
    }

    @Test
    @DisplayName("실패: Judge0 서버 제출 중 에러 발생 시 Judge0Exception을 던진다")
    void submit_Judge0ServerError() {
        // given
        var request = new SubmissionRequest("PYTHON", "code",
                List.of(new SubmissionRequest.TestCaseRequest("in", "out")));

        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willThrow(new RuntimeException("Connection Timeout"));

        // when & then
        assertThatThrownBy(() -> submissionService.submitAndAggregateTokens(request))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("제출 중 오류 발생");
    }

    @Test
    @DisplayName("성공: 조회 시 Redis 메타데이터를 결합하여 input, expected 및 정답 여부(isCorrect)를 반환한다")
    void getSubmissionResults_Success() throws Exception {
        // given
        String unifiedToken = UUID.randomUUID().toString();
        // Redis에 저장된 JSON Map 형태의 메타데이터 모킹
        String metadataJson = "{\"token1\":{\"input\":\"1 2\",\"expected\":\"3\"}}";
        given(redisTemplate.opsForValue().get("submission:" + unifiedToken)).willReturn(metadataJson);

        // Judge0 응답 Mock (Base64 stdout: "Mw==" -> "3", statusId 3 -> isCorrect: true)
        String judge0Response = """
                {
                    "submissions": [
                        {
                            "token": "token1",
                            "status": { "id": 3, "description": "Accepted" },
                            "stdout": "Mw==",
                            "time": "0.008",
                            "memory": "2400"
                        }
                    ]
                }
                """;
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(ResponseEntity.ok(judge0Response));

        // when
        List<SubmissionResultResponse.TestCaseResult> results = submissionService.getSubmissionResults(unifiedToken);

        // then
        assertThat(results).hasSize(1);
        var res = results.get(0);
        assertThat(res.stdout()).isEqualTo("3");
        assertThat(res.input()).isEqualTo("1 2");    // 메타데이터 병합 검증
        assertThat(res.expected()).isEqualTo("3");   // 메타데이터 병합 검증
        assertThat(res.isCorrect()).isTrue();         // 정답 여부 검증
        assertThat(res.time()).isEqualTo(0.008f);
    }

    @Test
    @DisplayName("실패: Redis에 해당 통합 토큰 정보가 없으면 InvalidSubmissionException이 발생한다")
    void getSubmissionResults_NotFound() {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> submissionService.getSubmissionResults("invalid-token"))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessageContaining("만료되었습니다");
    }

    @Test
    @DisplayName("실패: Judge0 결과 조회 중 응답 본문이 null이면 Judge0Exception이 발생한다")
    void getSubmissionResults_Judge0Error() {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn("{\"t1\":{}}");
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(ResponseEntity.ok(null));

        // when & then
        assertThatThrownBy(() -> submissionService.getSubmissionResults("uuid"))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("응답이 비어있습니다");
    }

    @Test
    @DisplayName("엣지 케이스: 디코딩할 수 없는 결과값이 들어오면 원본을 그대로 반환한다")
    void decodeBase64_Fallback() throws Exception {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn("{\"token1\":{}}");
        String invalidBase64Response = "{\"submissions\": [{\"token\": \"token1\", \"status\": {\"id\": 3}, \"stdout\": \"!!!Invalid!!!\"}]}";
        given(restTemplate.getForEntity(anyString(), eq(String.class))).willReturn(ResponseEntity.ok(invalidBase64Response));

        // when
        List<SubmissionResultResponse.TestCaseResult> results = submissionService.getSubmissionResults("uuid");

        // then
        assertThat(results.getFirst().stdout()).isEqualTo("!!!Invalid!!!");
    }

    @Test
    @DisplayName("엣지 케이스: Judge0 실행 정보(time, memory)가 null인 경우 에러 없이 처리한다")
    void getSubmissionResults_NullFields() throws Exception {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn("{\"token1\":{}}");
        String nullFieldsResponse = "{\"submissions\": [{\"token\": \"token1\", \"status\": {\"id\": 1}, \"time\": null, \"memory\": null}]}";
        given(restTemplate.getForEntity(anyString(), eq(String.class))).willReturn(ResponseEntity.ok(nullFieldsResponse));

        // when
        List<SubmissionResultResponse.TestCaseResult> results = submissionService.getSubmissionResults("uuid");

        // then
        assertThat(results.getFirst().time()).isNull();
        assertThat(results.getFirst().memory()).isNull();
    }
}