package com.ujax.application.workspace;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.infrastructure.external.s3.S3StorageService;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceUnitTest {

	@Mock
	private WorkspaceRepository workspaceRepository;

	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private S3StorageService s3StorageService;

	@InjectMocks
	private WorkspaceService workspaceService;

	@Test
	@DisplayName("listWorkspaces: 검색어가 없으면 전체 조회 경로를 사용한다")
	void listWorkspacesUsesFindAllWhenNameIsNull() {
		// given
		Workspace workspace = workspace(1L, "워크스페이스");
		PageRequest pageable = PageRequest.of(0, 20, defaultSort());
		given(workspaceRepository.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(workspace), pageable, 1));
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		// when
		var response = workspaceService.listWorkspaces(null, 0, 20);

		// then
		then(workspaceRepository).should().findAll(pageableCaptor.capture());
		then(workspaceRepository).should(never()).findByNameContaining(anyString(), any(Pageable.class));
		assertThat(pageableCaptor.getValue()).isEqualTo(pageable);
		assertThat(response.getContent()).extracting("id", "name")
			.containsExactly(tuple(1L, "워크스페이스"));
	}

	@Test
	@DisplayName("listWorkspaces: 공백 검색어면 전체 조회 경로를 사용한다")
	void listWorkspacesUsesFindAllWhenNameIsBlank() {
		// given
		PageRequest pageable = PageRequest.of(0, 20, defaultSort());
		given(workspaceRepository.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(), pageable, 0));

		// when
		workspaceService.listWorkspaces("   ", 0, 20);

		// then
		then(workspaceRepository).should().findAll(pageable);
		then(workspaceRepository).should(never()).findByNameContaining(anyString(), any(Pageable.class));
	}

	@Test
	@DisplayName("listWorkspaces: 검색어가 있으면 이름 검색 경로를 사용한다")
	void listWorkspacesUsesNameSearchWhenNameExists() {
		// given
		Workspace workspace = workspace(2L, "테스트 공간");
		PageRequest pageable = PageRequest.of(0, 20, defaultSort());
		given(workspaceRepository.findByNameContaining(eq("테스트"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(workspace), pageable, 1));

		// when
		var response = workspaceService.listWorkspaces("  테스트  ", 0, 20);

		// then
		then(workspaceRepository).should().findByNameContaining("테스트", pageable);
		then(workspaceRepository).should(never()).findAll(any(Pageable.class));
		assertThat(response.getContent()).extracting("id", "name")
			.containsExactly(tuple(2L, "테스트 공간"));
	}

	@Test
	@DisplayName("listWorkspaces: 페이지 값이 잘못되면 조회하지 않고 예외가 발생한다")
	void listWorkspacesThrowsBeforeRepositoryCallWhenPageableInvalid() {
		// when & then
		assertThatThrownBy(() -> workspaceService.listWorkspaces(null, -1, 0))
			.isInstanceOf(BadRequestException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER);
		then(workspaceRepository).shouldHaveNoInteractions();
	}

	private Workspace workspace(Long id, String name) {
		Workspace workspace = Workspace.create(name, "소개");
		ReflectionTestUtils.setField(workspace, "id", id);
		return workspace;
	}

	private Sort defaultSort() {
		return Sort.by(
			Sort.Order.desc("createdAt"),
			Sort.Order.desc("id")
		);
	}
}
