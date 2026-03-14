package com.ujax.infrastructure.web.api.submission;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static com.epages.restdocs.apispec.ResourceDocumentation.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.submission.SubmissionService;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.GlobalExceptionHandler;
import com.ujax.global.exception.common.InvalidSubmissionException;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.submission.SubmissionController;
import com.ujax.infrastructure.web.submission.dto.SubmissionRequest;
import com.ujax.infrastructure.web.submission.dto.SubmissionResultResponse;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Tag("restDocs")
@WebMvcTest(SubmissionController.class)
@AutoConfigureRestDocs
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class SubmissionControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private SubmissionService submissionService;

	@BeforeEach
	void setUpSecurityContext() {
		Claims claims = Jwts.claims()
			.subject("1")
			.add("role", "USER")
			.add("name", "테스트유저")
			.add("email", "test@example.com")
			.build();
		UserPrincipal principal = UserPrincipal.fromClaims(claims);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
		);
	}

	@Test
	@DisplayName("코드 제출 API")
	void createSubmission() throws Exception {
		// given
		SubmissionRequest request = new SubmissionRequest("JAVA", "System.out.println(1+2);",
			List.of(new SubmissionRequest.TestCaseRequest("1 2", "3")));

		given(submissionService.submitAndAggregateTokens(any())).willReturn("uuid-token-1234");

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/problems/{problemId}/submissions", 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("submission-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Submission")
					.summary("코드 제출")
					.description("소스 코드와 테스트 케이스를 Judge0에 제출하고 통합 토큰을 반환합니다")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemId").description("문제 ID")
					)
					.requestSchema(Schema.schema("SubmissionRequest"))
					.requestFields(
						fieldWithPath("language").type(JsonFieldType.STRING).description("프로그래밍 언어 (JAVA, PYTHON, CPP 등)"),
						fieldWithPath("sourceCode").type(JsonFieldType.STRING).description("소스 코드"),
						fieldWithPath("testCases").type(JsonFieldType.ARRAY).description("테스트 케이스 목록"),
						fieldWithPath("testCases[].input").type(JsonFieldType.STRING).description("입력값"),
						fieldWithPath("testCases[].expected").type(JsonFieldType.STRING).description("기대 출력값")
					)
					.responseSchema(Schema.schema("ApiResponse-SubmissionResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.submissionToken").type(JsonFieldType.STRING).description("제출 통합 토큰"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("코드 제출 API - 지원하지 않는 언어")
	void createSubmissionInvalidLanguage() throws Exception {
		// given
		SubmissionRequest request = new SubmissionRequest("BASIC", "code",
			List.of(new SubmissionRequest.TestCaseRequest("1", "1")));

		given(submissionService.submitAndAggregateTokens(any()))
			.willThrow(new InvalidSubmissionException(ErrorCode.INVALID_SUBMISSION, "지원하지 않는 언어: BASIC"));

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/problems/{problemId}/submissions", 1, 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("submission-create-invalid-language",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Submission")
					.summary("코드 제출")
					.description("코드 제출")
					.pathParameters(
						parameterWithName("workspaceId").description("워크스페이스 ID"),
						parameterWithName("problemId").description("문제 ID")
					)
					.requestSchema(Schema.schema("SubmissionRequest"))
					.requestFields(
						fieldWithPath("language").type(JsonFieldType.STRING).description("프로그래밍 언어"),
						fieldWithPath("sourceCode").type(JsonFieldType.STRING).description("소스 코드"),
						fieldWithPath("testCases").type(JsonFieldType.ARRAY).description("테스트 케이스 목록"),
						fieldWithPath("testCases[].input").type(JsonFieldType.STRING).description("입력값"),
						fieldWithPath("testCases[].expected").type(JsonFieldType.STRING).description("기대 출력값")
					)
					.responseSchema(Schema.schema("ProblemDetail-BadRequest"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	@Test
	@DisplayName("제출 결과 조회 API")
	void getResults() throws Exception {
		// given
		SubmissionResultResponse result = new SubmissionResultResponse(
			"token-123", 3, "Accepted", "3", null, null,
			0.008f, 2400, "1 2", "3", true);

		given(submissionService.getSubmissionResults(anyString())).willReturn(List.of(result));

		// when & then
		mockMvc.perform(get("/api/v1/submissions/{submissionToken}", "uuid-token-1234")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("submission-results",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Submission")
					.summary("제출 결과 조회")
					.description("통합 토큰으로 Judge0 채점 결과를 조회합니다")
					.pathParameters(
						parameterWithName("submissionToken").description("제출 통합 토큰")
					)
					.responseSchema(Schema.schema("ApiResponse-SubmissionResultList"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.ARRAY).description("채점 결과 목록"),
						fieldWithPath("data[].token").type(JsonFieldType.STRING).description("Judge0 토큰"),
						fieldWithPath("data[].statusId").type(JsonFieldType.NUMBER).description("상태 ID"),
						fieldWithPath("data[].statusDescription").type(JsonFieldType.STRING).description("상태 설명"),
						fieldWithPath("data[].stdout").type(JsonFieldType.STRING).description("표준 출력").optional(),
						fieldWithPath("data[].stderr").type(JsonFieldType.NULL).description("표준 에러").optional(),
						fieldWithPath("data[].compileOutput").type(JsonFieldType.NULL).description("컴파일 출력").optional(),
						fieldWithPath("data[].time").type(JsonFieldType.NUMBER).description("실행 시간 (초)").optional(),
						fieldWithPath("data[].memory").type(JsonFieldType.NUMBER).description("메모리 사용량 (KB)").optional(),
						fieldWithPath("data[].input").type(JsonFieldType.STRING).description("입력값"),
						fieldWithPath("data[].expected").type(JsonFieldType.STRING).description("기대 출력값"),
						fieldWithPath("data[].isCorrect").type(JsonFieldType.BOOLEAN).description("정답 여부"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("제출 결과 조회 API - 만료된 토큰")
	void getResultsExpiredToken() throws Exception {
		// given
		given(submissionService.getSubmissionResults(anyString()))
			.willThrow(new InvalidSubmissionException(ErrorCode.INVALID_SUBMISSION, "제출 정보가 만료되었습니다."));

		// when & then
		mockMvc.perform(get("/api/v1/submissions/{submissionToken}", "expired-token")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(document("submission-results-expired",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Submission")
					.summary("제출 결과 조회")
					.description("제출 결과 조회")
					.pathParameters(
						parameterWithName("submissionToken").description("제출 통합 토큰")
					)
					.responseSchema(Schema.schema("ProblemDetail-BadRequest"))
					.responseFields(problemDetailFields())
					.build()
				)
			));
	}

	private static FieldDescriptor[] problemDetailFields() {
		return new FieldDescriptor[] {
			fieldWithPath("type").type(JsonFieldType.STRING).description("오류 문서 URI"),
			fieldWithPath("title").type(JsonFieldType.STRING).description("에러 코드"),
			fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
			fieldWithPath("detail").type(JsonFieldType.STRING).description("오류 상세 메시지"),
			fieldWithPath("instance").type(JsonFieldType.STRING).description("오류 인스턴스").optional(),
			fieldWithPath("exception").type(JsonFieldType.STRING).description("예외 클래스명"),
			fieldWithPath("timestamp").type(JsonFieldType.STRING).description("발생 시각")
		};
	}
}
