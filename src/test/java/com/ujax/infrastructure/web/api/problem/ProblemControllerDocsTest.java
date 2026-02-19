package com.ujax.infrastructure.web.api.problem;

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

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.problem.ProblemService;
import com.ujax.application.problem.dto.response.AlgorithmTagResponse;
import com.ujax.application.problem.dto.response.ProblemResponse;
import com.ujax.application.problem.dto.response.SampleResponse;
import com.ujax.infrastructure.web.problem.ProblemController;
import com.ujax.infrastructure.web.problem.dto.request.ProblemIngestRequest;
import com.ujax.support.TestSecurityConfig;

@Tag("restDocs")
@WebMvcTest(ProblemController.class)
@AutoConfigureRestDocs
@Import(TestSecurityConfig.class)
class ProblemControllerDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ProblemService problemService;

	private ProblemResponse createMockResponse() {
		return new ProblemResponse(
			1L, 1000, "A+B", "Bronze V", "2 초", "128 MB",
			"두 정수 A와 B를 입력받은 다음, A+B를 출력하는 프로그램을 작성하시오.",
			"첫째 줄에 A와 B가 주어진다. (0 < A, B < 10)",
			"첫째 줄에 A+B를 출력한다.",
			"https://www.acmicpc.net/problem/1000",
			List.of(
				new SampleResponse(1L, 1, "1 2", "3"),
				new SampleResponse(2L, 2, "3 4", "7")
			),
			List.of(
				new AlgorithmTagResponse(1L, "수학"),
				new AlgorithmTagResponse(2L, "구현")
			)
		);
	}

	@Test
	@DisplayName("문제 조회 API")
	void getProblem() throws Exception {
		// given
		given(problemService.getProblem(1L)).willReturn(createMockResponse());

		// when & then
		mockMvc.perform(get("/api/v1/problems/{problemId}", 1L)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.problemNumber").value(1000))
			.andDo(document("problem-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Problem")
					.summary("문제 조회")
					.description("문제 ID로 문제를 조회합니다")
					.pathParameters(
						parameterWithName("problemId").description("문제 ID")
					)
					.responseSchema(Schema.schema("ApiResponse-ProblemResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("문제 ID"),
						fieldWithPath("data.problemNumber").type(JsonFieldType.NUMBER).description("백준 문제 번호"),
						fieldWithPath("data.title").type(JsonFieldType.STRING).description("문제 제목"),
						fieldWithPath("data.tier").type(JsonFieldType.STRING).description("난이도").optional(),
						fieldWithPath("data.timeLimit").type(JsonFieldType.STRING).description("시간 제한").optional(),
						fieldWithPath("data.memoryLimit").type(JsonFieldType.STRING).description("메모리 제한").optional(),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("문제 설명").optional(),
						fieldWithPath("data.inputDescription").type(JsonFieldType.STRING).description("입력 설명").optional(),
						fieldWithPath("data.outputDescription").type(JsonFieldType.STRING).description("출력 설명").optional(),
						fieldWithPath("data.url").type(JsonFieldType.STRING).description("백준 문제 URL").optional(),
						fieldWithPath("data.samples").type(JsonFieldType.ARRAY).description("입출력 예제 목록"),
						fieldWithPath("data.samples[].id").type(JsonFieldType.NUMBER).description("예제 ID"),
						fieldWithPath("data.samples[].sampleIndex").type(JsonFieldType.NUMBER).description("예제 번호"),
						fieldWithPath("data.samples[].input").type(JsonFieldType.STRING).description("예제 입력").optional(),
						fieldWithPath("data.samples[].output").type(JsonFieldType.STRING).description("예제 출력").optional(),
						fieldWithPath("data.algorithmTags").type(JsonFieldType.ARRAY).description("알고리즘 태그 목록"),
						fieldWithPath("data.algorithmTags[].id").type(JsonFieldType.NUMBER).description("태그 ID"),
						fieldWithPath("data.algorithmTags[].name").type(JsonFieldType.STRING).description("태그 이름"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제 번호로 조회 API")
	void getProblemByNumber() throws Exception {
		// given
		given(problemService.getProblemByNumber(1000)).willReturn(createMockResponse());

		// when & then
		mockMvc.perform(get("/api/v1/problems/number/{problemNumber}", 1000)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.problemNumber").value(1000))
			.andDo(document("problem-get-by-number",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Problem")
					.summary("문제 번호로 조회")
					.description("백준 문제 번호로 문제를 조회합니다")
					.pathParameters(
						parameterWithName("problemNumber").description("백준 문제 번호")
					)
					.responseSchema(Schema.schema("ApiResponse-ProblemResponse"))
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("문제 ID"),
						fieldWithPath("data.problemNumber").type(JsonFieldType.NUMBER).description("백준 문제 번호"),
						fieldWithPath("data.title").type(JsonFieldType.STRING).description("문제 제목"),
						fieldWithPath("data.tier").type(JsonFieldType.STRING).description("난이도").optional(),
						fieldWithPath("data.timeLimit").type(JsonFieldType.STRING).description("시간 제한").optional(),
						fieldWithPath("data.memoryLimit").type(JsonFieldType.STRING).description("메모리 제한").optional(),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("문제 설명").optional(),
						fieldWithPath("data.inputDescription").type(JsonFieldType.STRING).description("입력 설명").optional(),
						fieldWithPath("data.outputDescription").type(JsonFieldType.STRING).description("출력 설명").optional(),
						fieldWithPath("data.url").type(JsonFieldType.STRING).description("백준 문제 URL").optional(),
						fieldWithPath("data.samples").type(JsonFieldType.ARRAY).description("입출력 예제 목록"),
						fieldWithPath("data.samples[].id").type(JsonFieldType.NUMBER).description("예제 ID"),
						fieldWithPath("data.samples[].sampleIndex").type(JsonFieldType.NUMBER).description("예제 번호"),
						fieldWithPath("data.samples[].input").type(JsonFieldType.STRING).description("예제 입력").optional(),
						fieldWithPath("data.samples[].output").type(JsonFieldType.STRING).description("예제 출력").optional(),
						fieldWithPath("data.algorithmTags").type(JsonFieldType.ARRAY).description("알고리즘 태그 목록"),
						fieldWithPath("data.algorithmTags[].id").type(JsonFieldType.NUMBER).description("태그 ID"),
						fieldWithPath("data.algorithmTags[].name").type(JsonFieldType.STRING).description("태그 이름"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}

	@Test
	@DisplayName("문제 등록 API")
	void ingestProblem() throws Exception {
		// given
		ProblemIngestRequest request = new ProblemIngestRequest(
			1000, "A+B", "Bronze V", "2 초", "128 MB",
			"두 정수 A와 B를 입력받은 다음, A+B를 출력하는 프로그램을 작성하시오.",
			"첫째 줄에 A와 B가 주어진다. (0 < A, B < 10)",
			"첫째 줄에 A+B를 출력한다.",
			"https://www.acmicpc.net/problem/1000",
			List.of(
				new ProblemIngestRequest.SampleDto(1, "1 2", "3"),
				new ProblemIngestRequest.SampleDto(2, "3 4", "7")
			),
			List.of(
				new ProblemIngestRequest.TagDto("수학"),
				new ProblemIngestRequest.TagDto("구현")
			)
		);
		given(problemService.createProblem(any(ProblemIngestRequest.class)))
			.willReturn(createMockResponse());

		// when & then
		mockMvc.perform(post("/api/v1/problems/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.problemNumber").value(1000))
			.andDo(document("problem-ingest",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				resource(ResourceSnippetParameters.builder()
					.tag("Problem")
					.summary("문제 등록")
					.description("크롤러에서 수집한 백준 문제를 등록합니다")
					.requestSchema(Schema.schema("ProblemIngestRequest"))
					.responseSchema(Schema.schema("ApiResponse-ProblemResponse"))
					.requestFields(
						fieldWithPath("problemNum").type(JsonFieldType.NUMBER).description("백준 문제 번호"),
						fieldWithPath("title").type(JsonFieldType.STRING).description("문제 제목"),
						fieldWithPath("tier").type(JsonFieldType.STRING).description("난이도").optional(),
						fieldWithPath("timeLimit").type(JsonFieldType.STRING).description("시간 제한").optional(),
						fieldWithPath("memoryLimit").type(JsonFieldType.STRING).description("메모리 제한").optional(),
						fieldWithPath("problemDesc").type(JsonFieldType.STRING).description("문제 설명").optional(),
						fieldWithPath("problemInput").type(JsonFieldType.STRING).description("입력 설명").optional(),
						fieldWithPath("problemOutput").type(JsonFieldType.STRING).description("출력 설명").optional(),
						fieldWithPath("url").type(JsonFieldType.STRING).description("백준 문제 URL").optional(),
						fieldWithPath("samples").type(JsonFieldType.ARRAY).description("입출력 예제 목록").optional(),
						fieldWithPath("samples[].sampleIndex").type(JsonFieldType.NUMBER).description("예제 번호"),
						fieldWithPath("samples[].input").type(JsonFieldType.STRING).description("예제 입력").optional(),
						fieldWithPath("samples[].output").type(JsonFieldType.STRING).description("예제 출력").optional(),
						fieldWithPath("tags").type(JsonFieldType.ARRAY).description("알고리즘 태그 목록").optional(),
						fieldWithPath("tags[].name").type(JsonFieldType.STRING).description("태그 이름")
					)
					.responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("문제 ID"),
						fieldWithPath("data.problemNumber").type(JsonFieldType.NUMBER).description("백준 문제 번호"),
						fieldWithPath("data.title").type(JsonFieldType.STRING).description("문제 제목"),
						fieldWithPath("data.tier").type(JsonFieldType.STRING).description("난이도").optional(),
						fieldWithPath("data.timeLimit").type(JsonFieldType.STRING).description("시간 제한").optional(),
						fieldWithPath("data.memoryLimit").type(JsonFieldType.STRING).description("메모리 제한").optional(),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("문제 설명").optional(),
						fieldWithPath("data.inputDescription").type(JsonFieldType.STRING).description("입력 설명").optional(),
						fieldWithPath("data.outputDescription").type(JsonFieldType.STRING).description("출력 설명").optional(),
						fieldWithPath("data.url").type(JsonFieldType.STRING).description("백준 문제 URL").optional(),
						fieldWithPath("data.samples").type(JsonFieldType.ARRAY).description("입출력 예제 목록"),
						fieldWithPath("data.samples[].id").type(JsonFieldType.NUMBER).description("예제 ID"),
						fieldWithPath("data.samples[].sampleIndex").type(JsonFieldType.NUMBER).description("예제 번호"),
						fieldWithPath("data.samples[].input").type(JsonFieldType.STRING).description("예제 입력").optional(),
						fieldWithPath("data.samples[].output").type(JsonFieldType.STRING).description("예제 출력").optional(),
						fieldWithPath("data.algorithmTags").type(JsonFieldType.ARRAY).description("알고리즘 태그 목록"),
						fieldWithPath("data.algorithmTags[].id").type(JsonFieldType.NUMBER).description("태그 ID"),
						fieldWithPath("data.algorithmTags[].name").type(JsonFieldType.STRING).description("태그 이름"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("메시지").optional()
					)
					.build()
				)
			));
	}
}
