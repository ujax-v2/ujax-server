package com.ujax.domain.workspace;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class WorkspaceRepositoryTest {

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("워크스페이스 이름으로 검색할 수 있다")
	void findByNameContaining() {
		// given
		Workspace workspace1 = workspaceRepository.save(Workspace.create("테스트 공간", "소개"));
		workspaceRepository.save(Workspace.create("다른 공간", "소개"));

		// when
		Page<Workspace> result = workspaceRepository.findByNameContaining("테스트", PageRequest.of(0, 10));

		// then
		assertThat(result.getContent())
			.extracting("id")
			.containsExactly(workspace1.getId());
	}

	@Test
	@DisplayName("유저가 속한 워크스페이스를 조회할 수 있다")
	void findByMemberUserId() {
		// given
		User user = userRepository.save(User.createLocalUser("test@example.com", Password.ofEncoded("password"), "유저"));
		Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
		workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER));

		// when
		var result = workspaceRepository.findByMemberUserId(user.getId());

		// then
		assertThat(result)
			.extracting("id")
			.containsExactly(workspace.getId());
	}

	@Test
	@DisplayName("워크스페이스 이름 중복 여부를 확인할 수 있다")
	void existsByName() {
		// given
		workspaceRepository.save(Workspace.create("중복", "소개"));

		// when
		boolean exists = workspaceRepository.existsByName("중복");

		// then
		assertThat(exists).isTrue();
	}
}
