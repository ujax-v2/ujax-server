package com.ujax.infrastructure.web.api.workspace;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ujax.application.workspace.WorkspaceService;
import com.ujax.application.workspace.dto.response.WorkspaceResponse;
import com.ujax.application.workspace.dto.response.WorkspaceSettingsResponse;
import com.ujax.application.user.dto.response.PresignedUrlResponse;
import com.ujax.global.dto.PageResponse;
import com.ujax.infrastructure.security.UserPrincipal;
import com.ujax.infrastructure.web.workspace.WorkspaceController;
import com.ujax.infrastructure.web.workspace.dto.request.WorkspaceImageUploadRequest;
import com.ujax.support.TestSecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@WebMvcTest(WorkspaceController.class)
@Import(TestSecurityConfig.class)
class WorkspaceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WorkspaceService workspaceService;

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

	@Nested
	@DisplayName("내 워크스페이스 목록 조회")
	class ListMyWorkspaces {

		@Test
		@DisplayName("페이지 파라미터 없이 내 워크스페이스 목록을 조회한다")
		void listMyWorkspaces() throws Exception {
			WorkspaceResponse workspace = new WorkspaceResponse(
				1L,
				"워크스페이스",
				"소개",
				"https://image.example.com/workspaces/1.png"
			);
			given(workspaceService.listMyWorkspaces(anyLong())).willReturn(List.of(workspace));

			mockMvc.perform(get("/api/v1/workspaces/me"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[0].name").value("워크스페이스"))
				.andExpect(jsonPath("$.data[0].imageUrl").value("https://image.example.com/workspaces/1.png"));

			then(workspaceService).should().listMyWorkspaces(1L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 탐색 목록 조회")
	class ListWorkspaces {

		@Test
		@DisplayName("검색어와 페이지 파라미터를 반영해 탐색 목록을 조회한다")
		void listWorkspaces() throws Exception {
			WorkspaceResponse workspace = new WorkspaceResponse(
				3L,
				"알고리즘 스터디",
				"소개",
				"https://image.example.com/workspaces/3.png"
			);
			PageResponse<WorkspaceResponse> response = PageResponse.of(List.of(workspace), 0, 20, 1L, 1);
			given(workspaceService.listWorkspaces(any(), anyInt(), anyInt())).willReturn(response);

			mockMvc.perform(get("/api/v1/workspaces/explore")
					.param("name", "알고리즘")
					.param("page", "0")
					.param("size", "20"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content[0].id").value(3))
				.andExpect(jsonPath("$.data.content[0].name").value("알고리즘 스터디"))
				.andExpect(jsonPath("$.data.content[0].imageUrl").value("https://image.example.com/workspaces/3.png"))
				.andExpect(jsonPath("$.data.page.page").value(0))
				.andExpect(jsonPath("$.data.page.size").value(20));

			then(workspaceService).should().listWorkspaces("알고리즘", 0, 20);
		}

		@Test
		@DisplayName("파라미터가 없으면 기본값(0,20)으로 탐색 목록을 조회한다")
		void listWorkspacesWithDefaultPageable() throws Exception {
			PageResponse<WorkspaceResponse> response = PageResponse.of(List.of(), 0, 20, 0L, 0);
			given(workspaceService.listWorkspaces(any(), anyInt(), anyInt())).willReturn(response);

			mockMvc.perform(get("/api/v1/workspaces/explore"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.page.page").value(0))
				.andExpect(jsonPath("$.data.page.size").value(20));

			then(workspaceService).should().listWorkspaces(isNull(), eq(0), eq(20));
		}
	}

	@Nested
	@DisplayName("워크스페이스 상세 조회")
	class GetWorkspace {

		@Test
		@DisplayName("워크스페이스 상세 정보를 조회한다")
		void getWorkspace() throws Exception {
			WorkspaceResponse response = new WorkspaceResponse(
				3L,
				"알고리즘 스터디",
				"소개",
				"https://image.example.com/workspaces/3.png"
			);
			given(workspaceService.getWorkspace(anyLong())).willReturn(response);

			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.name").value("알고리즘 스터디"))
				.andExpect(jsonPath("$.data.imageUrl").value("https://image.example.com/workspaces/3.png"));

			then(workspaceService).should().getWorkspace(3L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 설정 조회")
	class GetWorkspaceSettings {

		@Test
		@DisplayName("워크스페이스 설정 정보를 조회한다")
		void getWorkspaceSettings() throws Exception {
			WorkspaceSettingsResponse response = new WorkspaceSettingsResponse(
				3L,
				"알고리즘 스터디",
				"소개",
				"https://image.example.com/workspaces/3.png",
				"https://hook.example.com"
			);
			given(workspaceService.getWorkspaceSettings(anyLong(), anyLong())).willReturn(response);

			mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/settings", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.imageUrl").value("https://image.example.com/workspaces/3.png"))
				.andExpect(jsonPath("$.data.mmWebhookUrl").value("https://hook.example.com"));

			then(workspaceService).should().getWorkspaceSettings(3L, 1L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 이미지 Presigned URL 생성")
	class CreateWorkspaceImagePresignedUrl {

		@Test
		@DisplayName("오너가 업로드용 Presigned URL을 생성한다")
		void createWorkspaceImagePresignedUrl() throws Exception {
			PresignedUrlResponse response = new PresignedUrlResponse("https://presigned-url", "https://image-url");
			given(workspaceService.createWorkspaceImagePresignedUrl(anyLong(), anyLong(), any(WorkspaceImageUploadRequest.class)))
				.willReturn(response);

			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/image/presigned-url", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"contentType\":\"image/png\",\"fileSize\":1024}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.presignedUrl").value("https://presigned-url"))
				.andExpect(jsonPath("$.data.imageUrl").value("https://image-url"));

			then(workspaceService).should()
				.createWorkspaceImagePresignedUrl(eq(3L), eq(1L), any(WorkspaceImageUploadRequest.class));
		}

		@Test
		@DisplayName("요청 값이 비어 있으면 400을 반환한다")
		void createWorkspaceImagePresignedUrlValidationError() throws Exception {
			mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/image/presigned-url", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"contentType\":\"\",\"fileSize\":null}"))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("워크스페이스 생성")
	class CreateWorkspace {

		@Test
		@DisplayName("워크스페이스를 생성한다")
		void createWorkspace() throws Exception {
			WorkspaceResponse response = new WorkspaceResponse(
				20L,
				"신규 워크스페이스",
				"소개",
				"https://image.example.com/workspaces/20.png"
			);
			given(workspaceService.createWorkspace(anyString(), any(), anyLong())).willReturn(response);

			mockMvc.perform(post("/api/v1/workspaces")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"신규 워크스페이스\",\"description\":\"소개\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(20))
				.andExpect(jsonPath("$.data.name").value("신규 워크스페이스"))
				.andExpect(jsonPath("$.data.imageUrl").value("https://image.example.com/workspaces/20.png"));

			then(workspaceService).should().createWorkspace("신규 워크스페이스", "소개", 1L);
		}
	}

	@Nested
	@DisplayName("워크스페이스 수정")
	class UpdateWorkspace {

		@Test
		@DisplayName("워크스페이스 정보를 수정한다")
		void updateWorkspace() throws Exception {
			WorkspaceResponse response = new WorkspaceResponse(
				3L,
				"수정된 워크스페이스",
				"수정된 소개",
				"https://new-image.com/workspace.png"
			);
			given(workspaceService.updateWorkspace(anyLong(), anyLong(), any(), any(), any(), any())).willReturn(response);

			mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}", 3L)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"수정된 워크스페이스\",\"description\":\"수정된 소개\",\"mmWebhookUrl\":\"https://hook.example.com\",\"imageUrl\":\"https://new-image.com/workspace.png\"}"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.name").value("수정된 워크스페이스"))
				.andExpect(jsonPath("$.data.imageUrl").value("https://new-image.com/workspace.png"));

			then(workspaceService).should().updateWorkspace(
				3L,
				1L,
				"수정된 워크스페이스",
				"수정된 소개",
				"https://hook.example.com",
				"https://new-image.com/workspace.png"
			);
		}
	}

	@Nested
	@DisplayName("워크스페이스 삭제")
	class DeleteWorkspace {

		@Test
		@DisplayName("워크스페이스를 삭제한다")
		void deleteWorkspace() throws Exception {
			willDoNothing().given(workspaceService).deleteWorkspace(anyLong(), anyLong());

			mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", 3L))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			then(workspaceService).should().deleteWorkspace(3L, 1L);
		}
	}
}
