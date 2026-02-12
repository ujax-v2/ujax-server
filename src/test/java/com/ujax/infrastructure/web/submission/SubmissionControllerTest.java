package com.ujax.infrastructure.web.submission;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.submission.SubmissionService;
import com.ujax.infrastructure.web.submission.dto.SubmissionRequest;

@WebMvcTest(SubmissionController.class)
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmissionService submissionService;

    @Test
    @DisplayName("코드 제출 API가 정상적으로 호출된다")
    void createSubmission_Success() throws Exception {
        // given
        var request = new SubmissionRequest("JAVA", "source",
                List.of(new SubmissionRequest.TestCaseRequest("1", "1")));
        String mockToken = "uuid-token-1234";

        given(submissionService.submitAndAggregateTokens(any())).willReturn(mockToken);

        // when & then
        mockMvc.perform(post("/api/v1/workspaces/1/problems/1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.submissionToken").value(mockToken));
    }

    @Test
    @DisplayName("결과 조회 API 호출 정상동작 확인")
    void getResults_ApiSuccess() throws Exception {
        given(submissionService.getSubmissionResults(anyString())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/submissions/test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }
}