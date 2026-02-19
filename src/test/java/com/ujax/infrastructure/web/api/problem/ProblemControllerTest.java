package com.ujax.infrastructure.web.api.problem;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(ProblemController.class)
@Import(TestSecurityConfig.class)
class ProblemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ProblemService problemService;

	private ProblemResponse createMockResponse() {
		return new ProblemResponse(
			1L, 1000, "A+B", "Bronze V", "2 초", "128 MB",
			"설명", "입력", "출력", "https://www.acmicpc.net/problem/1000",
			List.of(new SampleResponse(1L, 1, "1 2", "3")),
			List.of(new AlgorithmTagResponse(1L, "수학"))
		);
	}

	@Nested
	@DisplayName("문제 조회")
	class GetProblem {

		@Test
		@DisplayName("ID로 문제를 조회한다")
		void getProblem_Success() throws Exception {
			// given
			given(problemService.getProblem(1L)).willReturn(createMockResponse());

			// when & then
			mockMvc.perform(get("/api/v1/problems/1"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.problemNumber").value(1000))
				.andExpect(jsonPath("$.data.title").value("A+B"));
		}
	}

	@Nested
	@DisplayName("문제 번호로 조회")
	class GetProblemByNumber {

		@Test
		@DisplayName("문제 번호로 문제를 조회한다")
		void getProblemByNumber_Success() throws Exception {
			// given
			given(problemService.getProblemByNumber(1000)).willReturn(createMockResponse());

			// when & then
			mockMvc.perform(get("/api/v1/problems/number/1000"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.problemNumber").value(1000));
		}
	}

	@Nested
	@DisplayName("문제 생성")
	class IngestProblem {

		@Test
		@DisplayName("문제를 생성한다")
		void ingestProblem_Success() throws Exception {
			// given
			ProblemIngestRequest request = new ProblemIngestRequest(
				1000, "A+B", "Bronze V", "2 초", "128 MB",
				"설명", "입력", "출력", null,
				List.of(new ProblemIngestRequest.SampleDto(1, "1 2", "3")),
				List.of(new ProblemIngestRequest.TagDto("수학"))
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
				.andExpect(jsonPath("$.data.problemNumber").value(1000));
		}

		@Test
		@DisplayName("제목이 비어있으면 오류가 발생한다")
		void ingestProblem_BlankTitle() throws Exception {
			// given
			ProblemIngestRequest request = new ProblemIngestRequest(
				1000, "", "Bronze V", null, null,
				null, null, null, null, null, null
			);

			// when & then
			mockMvc.perform(post("/api/v1/problems/ingest")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("문제 번호가 0이하이면 오류가 발생한다")
		void ingestProblem_InvalidProblemNumber() throws Exception {
			// given
			ProblemIngestRequest request = new ProblemIngestRequest(
				0, "A+B", "Bronze V", null, null,
				null, null, null, null, null, null
			);

			// when & then
			mockMvc.perform(post("/api/v1/problems/ingest")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}
}
