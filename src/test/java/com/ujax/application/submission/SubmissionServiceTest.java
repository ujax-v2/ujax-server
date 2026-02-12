package com.ujax.application.submission;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

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

import java.util.UUID;

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
    @DisplayName("성공: 코드 제출 시 통합 토큰을 생성하고 Redis에 저장한다")
    void submitAndAggregateTokens_Success() throws Exception {
        // given
        var testCase = new SubmissionRequest.TestCaseRequest("1 2", "3");
        var request = new SubmissionRequest("JAVA", "encodedCode", List.of(testCase));

        String judge0Response = "[{\"token\": \"token-abc-123\"}]";
        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willReturn(ResponseEntity.ok(judge0Response));

        // when
        String unifiedToken = submissionService.submitAndAggregateTokens(request);

        // then
        assertThat(unifiedToken).isNotNull();
        verify(valueOperations).set(startsWith("submission:"), eq("token-abc-123"), any());
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
                .hasMessageContaining("지원하지 않는 언어입니다");
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
    @DisplayName("실패: Judge0 서버 통신 중 에러 발생 시 Judge0Exception을 던진다")
    void submit_Judge0ServerError() {
        // given
        var request = new SubmissionRequest("PYTHON", "code",
                List.of(new SubmissionRequest.TestCaseRequest("in", "out")));

        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willThrow(new RuntimeException("Connection Timeout"));

        // when & then
        assertThatThrownBy(() -> submissionService.submitAndAggregateTokens(request))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("코드 제출 처리 중 오류 발생");
    }

    @Test
    @DisplayName("성공: CPP 언어 ID 변환이 정상 동작한다")
    void submit_CppLanguage() throws Exception {
        // given
        var request = new SubmissionRequest("CPP", "source",
                List.of(new SubmissionRequest.TestCaseRequest("1", "1")));

        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willReturn(ResponseEntity.ok("[{\"token\": \"cpp-token\"}]"));

        // when
        String token = submissionService.submitAndAggregateTokens(request);

        // then
        assertThat(token).isNotNull();
    }

    @Test
    @DisplayName("성공: Redis에서 토큰 목록을 조회하여 Judge0 결과를 반환하고 Base64를 디코딩한다")
    void getSubmissionResults_Success() throws Exception {
        // given
        String unifiedToken = UUID.randomUUID().toString();
        String individualTokens = "token1,token2";
        given(redisTemplate.opsForValue().get("submission:" + unifiedToken)).willReturn(individualTokens);

        // Judge0 응답 Mock (Base64 인코딩된 stdout: "Mw==" -> "3")
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
        assertThat(results.get(0).stdout()).isEqualTo("3"); // 디코딩 검증
        assertThat(results.get(0).time()).isEqualTo(0.008f); // 형변환 검증
        assertThat(results.get(0).statusDescription()).isEqualTo("Accepted");
    }

    @Test
    @DisplayName("실패: Redis에 해당 통합 토큰 정보가 없으면 InvalidSubmissionException이 발생한다")
    void getSubmissionResults_NotFound() {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> submissionService.getSubmissionResults("invalid-token"))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessageContaining("존재하지 않습니다");
    }

    @Test
    @DisplayName("실패: Judge0 서버 응답 본문이 null이면 Judge0Exception이 발생한다")
    void getSubmissionResults_Judge0Error() {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn("token1");
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(ResponseEntity.ok(null));

        // when & then
        assertThatThrownBy(() -> submissionService.getSubmissionResults("uuid"))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("응답이 비어있습니다");
    }

    @Test
    @DisplayName("엣지 케이스: 디코딩할 수 없는 stdout 값이 들어오면 원본을 그대로 반환한다")
    void decodeBase64_Fallback() throws Exception {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn("token1");
        String invalidBase64Response = """
                {
                    "submissions": [
                        {
                            "token": "token1",
                            "status": { "id": 3, "description": "Accepted" },
                            "stdout": "!!!InvalidBase64!!!"
                        }
                    ]
                }
                """;
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(ResponseEntity.ok(invalidBase64Response));

        // when
        List<SubmissionResultResponse.TestCaseResult> results = submissionService.getSubmissionResults("uuid");

        // then
        // catch 블록의 커버리지를 위해 디코딩 실패 시 원본 반환 확인
        assertThat(results.get(0).stdout()).isEqualTo("!!!InvalidBase64!!!");
    }

    @Test
    @DisplayName("엣지 케이스: 실행 시간(time)이나 메모리 값이 null인 경우 에러 없이 처리한다")
    void getSubmissionResults_NullFields() throws Exception {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn("token1");
        String nullFieldsResponse = """
                {
                    "submissions": [
                        {
                            "token": "token1",
                            "status": { "id": 1, "description": "In Queue" },
                            "time": null,
                            "memory": null
                        }
                    ]
                }
                """;
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(ResponseEntity.ok(nullFieldsResponse));

        // when
        List<SubmissionResultResponse.TestCaseResult> results = submissionService.getSubmissionResults("uuid");

        // then
        assertThat(results.getFirst().time()).isNull();
        assertThat(results.getFirst().memory()).isNull();
    }
}