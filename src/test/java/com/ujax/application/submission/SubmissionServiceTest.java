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
}